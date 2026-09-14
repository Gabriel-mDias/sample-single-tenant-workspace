# Quickstart de validação — Conformidade GEMS

Executar somente depois de D-P01–D-P03 resolvidas, implementação concluída e antes de D-P04 final.

## Auditoria

1. Aplicar upgrade sobre schema antigo populado; confirmar parada diante de dado incompatível e
   `ddl-auto: validate` verde no caso válido.
2. Confirmar commit com identidade estável/correlação, rollback sem dado/efeito/trilha, sigilo e ator
   técnico.
3. Alterar/remover usuário e reutilizar login; confirmar filtro histórico sem mistura.
4. Exercitar histórico agregado e busca com data final inclusiva.

## Keycloak e ambiente

1. Subir apenas com `docker compose up -d`; confirmar one-shot após health, cache false e backend após
   conclusão em local/staging/produção.
2. Validar 21 ações, compostas, grupos e idempotência em realm já existente.
3. Criar, alterar, suspender, reativar e mover usuário; induzir falhas para provar compensações/502.
4. Se D-P03 for escolhida, comparar seções não derivadas do realm byte/semanticamente antes/depois.

## Frontend

1. Rodar `npm ci` sem flag em host, Dockerfile e CI.
2. Exercitar oito rotas com ação presente/ausente, create/edit/view, erros acessíveis e navegação.
3. Gerar inventário residual com identidade nominal e submeter D-P04.

## Gates finais

```powershell
cd backend
.\mvnw.cmd clean install

cd ..\frontend
npm test
npm run build
npx prettier --check .
```

O backend fecha somente com zero dívida Java, `suppressions.xml` sem dívida, baselines sem tolerância e
sem `ratchetFrom`. O frontend fecha somente depois da decisão humana sobre cada residual.
