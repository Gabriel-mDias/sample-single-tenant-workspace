# Checklist de Fechamento de Rodada

Copie e preencha ao fim de **cada** rodada. Reusável em todas.

---

## A. Antes do código — o portão humano

- [ ] `/speckit-analyze` executado, sem achado **CRITICAL** pendente
- [ ] Achados HIGH e MEDIUM incorporados aos artefatos, não só listados
- [ ] `/speckit-clarify` executado **ou** dispensado por escrito, com a razão
- [ ] Todas as decisões da §2 do roteiro confirmadas pelo product owner
- [ ] O modelo de dados e o contrato REST aprovados
- [ ] O raio de impacto sobre o que já está entregue é aceito
- [ ] **§12 do `roteiro.md` assinada, com data**

> Nenhuma linha de código de feature antes desta seção estar fechada. Princípio VIII.

---

## B. Durante — a cada lote entregue

- [ ] O portão do lote rodou **verde**, e a saída foi lida (não presumida)
- [ ] `./mvnw clean install` do **reator completo** — nunca `-pl` isolado quando a mudança é
      transversal
- [ ] `npm test` e `npm run build`
- [ ] `npx prettier --write .`, se tocou frontend
- [ ] O orquestrador — não o delegado — verificou e comitou
- [ ] Nenhuma decisão marcada ✅ foi alterada sem registro

---

## C. Testes que o projeto exige por escrito

- [ ] Toda consulta derivada que alimenta DTO fora da transação tem teste de integração
      **NÃO transacional**
- [ ] Todo `@NamedEntityGraph` novo está no **lado que origina a navegação**, e há teste que falha
      se ele for removido
- [ ] Todo wizard tocado ganhou teste de navegação — `goNext()` e `goBack()`
- [ ] Toda varredura nova parte da **fonte da verdade** (mapeamento JPA, `app.routes.ts`), nunca de
      lista escrita à mão
- [ ] Toda rede nova foi provada por **mutação**: quebre o alvo, confirme que o teste falha

---

## D. Depois do código — para considerar a rodada concluída

- [ ] Código em `main`
- [ ] Números de teste **medidos e anotados no roteiro** — backend e frontend, não herdados de
      documento anterior
- [ ] O critério de aceitação foi exercitado com **dado real de negócio**, num navegador
- [ ] Zero regressão nas features já entregues
- [ ] Homologação do product owner registrada no roteiro, **com data**
- [ ] `scripts/verificar-contexto-agentes.ps1` verde; adaptadores raiz e regras por camada atualizados
- [ ] Divergências entre documentação e código reportadas — e corrigidas, se pequenas e óbvias

---

## E. O que se aprende do jeito caro

Leia antes de declarar uma rodada pronta. Esta tabela é pequena de propósito: **só entra o que
custou**. Acrescente a linha na rodada em que a lição foi paga, com a origem — a origem é o que
impede que ela seja lida como opinião.

| Lição | Por quê |
| :--- | :--- |
| **Teste unitário com repository mockado não prova que a consulta funciona** | O mock devolve o objeto já montado; a `LazyInitializationException` só existe quando há sessão de verdade e ela fechou |
| **Teste de integração `@Transactional` esconde o defeito** | A sessão fica aberta até o fim do teste, e ele passa com e sem o problema |
| **A tela pode existir e ser inalcançável** | Um passo de wizard pode aparecer na barra de progresso sem que nenhum caminho do formulário chegue nele. A suíte testa o componente, não a navegação até ele |
| **A rede montada à mão é vítima do próprio esquecimento** | Uma varredura que percorre uma lista escrita à mão ignora em silêncio o que não foi acrescentado à lista. Ela precisa partir da fonte da verdade |
| **Defeito silencioso custa mais que defeito barulhento** | Erro que aparece no primeiro minuto de uso real e em nenhum teste é a assinatura de uma suíte que testa o que é fácil testar |
| **Uma mutação pode passar porque o TESTE é fraco, não porque a rede é boa** | Ao quebrar o alvo, escolha o caminho que **só o alvo explica** — senão o teste responde certo com a regra invertida |
| **Função de data é fronteira entre a HQL e o banco, e só o teste de integração a alcança** | Funções de data têm semântica própria em cada banco, e uma tipagem genérica pode recusar operações que compilam. Nenhuma das duas falhas aparece em compilação |

---

## F. Relatório final

Um texto curto dizendo:

1. **o que foi feito**;
2. **o que não foi, e por quê**;
3. **o que ficou frouxo** e em que rodada fecha;
4. as **divergências** encontradas entre documentação e código.

**Se algo falhou, mostre a saída.** Relatório que omite o que não funcionou custa mais que a falha.
