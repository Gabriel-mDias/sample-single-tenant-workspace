import { describe, expect, it } from 'vitest';
import { Acoes } from '../auth/acoes';
import { MenuAuthContext } from '../auth/auth.service';
import { buildMenu } from './menu.builder';

/**
 * O construtor de menu é função pura, e por isso é testável sem Keycloak, sem TestBed e sem HTTP.
 *
 * O que estes testes protegem não é a aparência do menu: é a regra de que **cada item depende da
 * ação de consulta do seu recurso**. Um item que passe a depender de outra coisa — um perfil, um
 * nível, a ausência de uma ação — quebra aqui.
 */
describe('buildMenu', () => {
  const contextoCom = (...acoes: string[]): MenuAuthContext => ({
    hasRole: (acao: string) => acoes.includes(acao),
    hasAnyRole: (lista: string[]) => lista.some((acao) => acoes.includes(acao)),
  });

  it('sem ação nenhuma, o menu é vazio — falha FECHADO', () => {
    expect(buildMenu(contextoCom())).toEqual([]);
  });

  it('cada ação de consulta acrescenta exatamente um item', () => {
    expect(buildMenu(contextoCom(Acoes.CONSULTAR_PARCEIRO))).toHaveLength(1);
    expect(buildMenu(contextoCom(Acoes.CONSULTAR_SOLICITACAO))).toHaveLength(1);
    expect(buildMenu(contextoCom(Acoes.CONSULTAR_AUDITORIA))).toHaveLength(1);
  });

  it('o item de parceiros aponta para a rota de parceiros', () => {
    const [item] = buildMenu(contextoCom(Acoes.CONSULTAR_PARCEIRO));
    expect(item.route).toBe('/parceiros');
    expect(item.label).toBe('Parceiros');
  });

  it('ação de ESCRITA não abre item de menu sozinha', () => {
    // Quem só pode inserir mas não pode consultar não consegue abrir a listagem: mostrar o item
    // seria oferecer uma tela que responde 403.
    expect(buildMenu(contextoCom(Acoes.INSERIR_PARCEIRO))).toEqual([]);
  });

  it('quem tem tudo vê todos os itens, sem duplicata', () => {
    const menu = buildMenu(
      contextoCom(Acoes.CONSULTAR_PARCEIRO, Acoes.CONSULTAR_SOLICITACAO, Acoes.CONSULTAR_AUDITORIA),
    );
    expect(menu).toHaveLength(3);
    expect(new Set(menu.map((i) => i.route)).size).toBe(3);
  });

  it('a home não tem entrada de menu', () => {
    const menu = buildMenu(
      contextoCom(Acoes.CONSULTAR_PARCEIRO, Acoes.CONSULTAR_SOLICITACAO, Acoes.CONSULTAR_AUDITORIA),
    );
    expect(menu.map((i) => i.route)).not.toContain('/');
  });
});
