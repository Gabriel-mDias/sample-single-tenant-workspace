/**
 * Gate do lado Node para o catálogo de ações.
 *
 * A partir da rodada 007 o catálogo tem UMA fonte: o enum `AcaoSistema`, em
 * `backend/app-security`. O JSON deste diretório é GERADO a partir dele por
 * `FrontendActionCatalogGenerator`, e a coerência entre os dois é verificada no build Maven.
 *
 * POR QUE ESTE SCRIPT CONTINUA EXISTINDO, então: o Maven não roda em alteração que toque só o
 * frontend. Sem ele, alguém editaria o JSON gerado à mão, `npm test` passaria, e a divergência só
 * apareceria no próximo build do backend — ou em produção, como botão que não aparece. O que ele
 * verifica:
 *
 *   1. o JSON gerado é válido e está na forma que o gerador produz;
 *   2. o que o TypeScript importa é exatamente o que o JSON declara;
 *   3. as duas listas são disjuntas e sem duplicidade;
 *   4. os NOMES e os ESCOPOS conferem com as constantes do enum `AcaoSistema`.
 *
 * O item 4 entrou na homologação da 007, e é o que o script não fazia. Antes dele, este gate
 * comparava só a CARDINALIDADE: uma ação renomeada à mão para outro nome válido passava, e
 * `npm run build` — que roda este script mas não os specs do Vitest — gerava bundle verde com o
 * catálogo divergente do enum. O `npm test` reprovava por causa de um spec que fixa os seis nomes,
 * e o build Maven reprovava de verdade; um pipeline que só buildasse o frontend não reprovava
 * nada. Ler o enum é de propósito: ele é a fonte única, e um gate que não a consulta não é gate.
 *
 * O que ele NÃO faz é comparar três cópias — duas delas não existem mais.
 */
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const frontendRoot = resolve(import.meta.dirname, '..');
const catalogPath = resolve(frontendRoot, 'src/app/core/authorization/authorization.actions.json');
const consumerPath = resolve(frontendRoot, 'src/app/core/authorization/authorization.actions.ts');
const enumPath = resolve(
  frontendRoot,
  '../backend/app-security/src/main/java/br/com/gems/meduc/security/authorization/AcaoSistema.java',
);

const pattern = /^[A-Z][A-Z0-9]*_[A-Z0-9_]+$/;

function validate(name, values) {
  if (
    !Array.isArray(values) ||
    values.some((value) => typeof value !== 'string' || !pattern.test(value))
  ) {
    throw new Error(`Catálogo ${name} inválido`);
  }
  if (new Set(values).size !== values.length) {
    throw new Error(`Catálogo ${name} contém duplicidade`);
  }
  return [...values].sort();
}

const raw = readFileSync(catalogPath, 'utf8');
const catalog = JSON.parse(raw);

if (Object.keys(catalog).length !== 2 || !('global' in catalog) || !('tenant' in catalog)) {
  throw new Error(
    'authorization.actions.json é gerado e deve conter exatamente as chaves "global" e "tenant". ' +
      'Ele não é editado à mão: regere-o pelo build do backend.',
  );
}

const global = validate('global', catalog.global);
const tenant = validate('tenant', catalog.tenant);

if (global.some((action) => tenant.includes(action))) {
  throw new Error('Catálogos global e tenant devem ser disjuntos');
}

// O gerador emite as ações em ordem alfabética. Um JSON fora de ordem é JSON editado à mão, e o
// build Maven o reprovaria — melhor descobrir aqui.
for (const [name, values] of [
  ['global', catalog.global],
  ['tenant', catalog.tenant],
]) {
  if (JSON.stringify(values) !== JSON.stringify([...values].sort())) {
    throw new Error(
      `Catálogo ${name} fora da ordem que o gerador produz. O arquivo é gerado, não editado.`,
    );
  }
}

// O TypeScript precisa consumir as DUAS listas do JSON. Um consumidor que declarasse ações à mão
// reintroduziria a cópia que esta rodada removeu.
const consumer = readFileSync(consumerPath, 'utf8');
for (const key of ['actions.global', 'actions.tenant']) {
  if (!consumer.includes(key)) {
    throw new Error(
      `authorization.actions.ts deve consumir ${key} do JSON gerado; não encontrei a referência.`,
    );
  }
}
if (/['"][A-Z][A-Z0-9]*_[A-Z0-9_]+['"]/.test(consumer)) {
  throw new Error(
    'authorization.actions.ts contém nome de ação escrito à mão. O catálogo tem uma fonte: o enum ' +
      'AcaoSistema, do backend.',
  );
}

// O enum é a fonte única. Aqui ele é lido como texto — o lado Node não compila Java, mas as
// constantes de um enum têm forma fixa: `NOME(AuthorizationScope.ESCOPO)`. Se o arquivo não estiver
// alcançável (um checkout só do frontend), o gate diz isso em vez de fingir que verificou.
let enumSource;
try {
  enumSource = readFileSync(enumPath, 'utf8');
} catch {
  throw new Error(
    `Não consegui ler o enum AcaoSistema em ${enumPath}. O catálogo do frontend é gerado a partir ` +
      'dele, e sem ele este gate não tem contra o que comparar.',
  );
}

// Só o corpo do enum: o Javadoc da classe cita nomes de ação em prosa, e comentário não é fonte.
const body = enumSource.slice(enumSource.indexOf('public enum AcaoSistema'));
const declared = { global: [], tenant: [] };
for (const [, name, scope] of body.matchAll(
  /^\s{4}([A-Z][A-Z0-9_]+)\(\s*AuthorizationScope\.(GLOBAL|TENANT)\s*\)/gm,
)) {
  declared[scope === 'GLOBAL' ? 'global' : 'tenant'].push(name);
}

if (declared.global.length + declared.tenant.length === 0) {
  throw new Error(
    'Não encontrei constante alguma em AcaoSistema. A forma do enum mudou, e este gate ficou cego: ' +
      'corrija o script antes de seguir.',
  );
}

for (const [name, fromEnum, fromJson] of [
  ['global', declared.global, global],
  ['tenant', declared.tenant, tenant],
]) {
  const expected = [...fromEnum].sort();
  if (JSON.stringify(expected) !== JSON.stringify(fromJson)) {
    const faltando = expected.filter((a) => !fromJson.includes(a));
    const sobrando = fromJson.filter((a) => !expected.includes(a));
    throw new Error(
      `Catálogo ${name} divergente de AcaoSistema. O arquivo é gerado, não editado: regere-o pelo ` +
        `build do backend.${faltando.length ? ` Falta no JSON: ${faltando.join(', ')}.` : ''}` +
        `${sobrando.length ? ` Não existe no enum: ${sobrando.join(', ')}.` : ''}`,
    );
  }
}

console.log(
  `Catálogo de ações em paridade com AcaoSistema: ${global.length} globais, ${tenant.length} de organização.`,
);
