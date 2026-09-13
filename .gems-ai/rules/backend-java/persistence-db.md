---
stack: backend-java
module: gems-jpa
severity: mandatory
see-also: [architecture.md, repositories.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Persistência & Banco de Dados — PostgreSQL + Liquibase

> **TL;DR:**
> - Prefixos obrigatórios: `ID_` (UUID), `CD_` (enum/cód), `DS_` (varchar≤4k), `TX_` (text longo), `VL_` (numérico), `DT_` (data).
> - Banco: PostgreSQL + Liquibase (`changelog-multi-schemas.xml`). Tabelas em MAIÚSCULO.
> - Toda tabela inclui `DT_CRIACAO` e `DT_EXCLUSAO`. Soft delete via `@SQLDelete + @SQLRestriction` — nunca DELETE físico.
> - UUID auto: `defaultValueComputed="${uuid_function}"` no Liquibase.

## 1. Tecnologias

- **Banco:** PostgreSQL.
- **Versionamento:** Liquibase (`changelog-multi-schemas.xml`).

---

## 2. Prefixos de Colunas (Obrigatórios)

| Prefixo | Significado | Tipo Recomendado | Exemplo |
| :--- | :--- | :--- | :--- |
| **ID_** | Identificadores e FKs | UUID | `ID_ENTIDADE`, `ID_RELACIONADO` |
| **CD_** | Códigos, Enums, Documentos | VARCHAR / Enum | `CD_TIPO`, `CD_SITUACAO` |
| **DS_** | Descritivos e Strings (até 4000 chars) | VARCHAR(4000) | `DS_NOME`, `DS_EMAIL` |
| **TX_** | Textos longos (>4000 chars) | TEXT | `TX_OBSERVACAO`, `TX_DESCRICAO` |
| **VL_** | Valores numéricos e monetários | NUMERIC(10,2) / INT | `VL_VALOR`, `VL_QUANTIDADE` |
| **DT_** | Datas e Timestamps | TIMESTAMP | `DT_CRIACAO`, `DT_EXCLUSAO`, `DT_NASCIMENTO` |

---

## 3. Tipagem de Dados (PostgreSQL)

| Caso | Tipo SQL |
| :--- | :--- |
| Strings curtas | `VARCHAR(N)` |
| Textos longos | `TEXT` |
| Monetário/Decimal | `NUMERIC(10,2)` |
| Inteiro | `INT4` (Integer) ou `INT8` (Long) |
| Identificadores | `UUID` |
| Datas e timestamps | `TIMESTAMP` |

---

## 4. Liquibase — Estrutura de ChangeSet

### Auto-incremento de UUID
```xml
<column name="ID_ENTIDADE" type="UUID" defaultValueComputed="${uuid_function}">
    <constraints primaryKey="true" nullable="false"/>
</column>
```

### Soft Delete — coluna obrigatória
```xml
<column name="DT_EXCLUSAO" type="TIMESTAMP"/>
```

### Colunas de auditoria — toda tabela deve ter
```xml
<column name="DT_CRIACAO" type="TIMESTAMP" defaultValueDate="CURRENT_TIMESTAMP">
    <constraints nullable="false"/>
</column>
<column name="DT_EXCLUSAO" type="TIMESTAMP"/>
```

---

## 5. Convenções de Tabelas

- Nomes em **MAIÚSCULO** (SNAKE_CASE): `ORGANIZACAO`, `INSTITUICAO`.
- Toda tabela inclui `DT_CRIACAO` e `DT_EXCLUSAO`.
- Nenhuma tabela usa DELETE físico — soft delete apenas.

---

## 6. Soft Delete na Entidade JPA

O soft delete é gerenciado automaticamente via Hibernate — nunca implemente manualmente:

```java
@SQLDelete( sql = "UPDATE NOME_TABELA SET DT_EXCLUSAO = NOW() WHERE ID_ENTIDADE = ?" )
@SQLRestriction( "DT_EXCLUSAO IS NULL" )
public class {Entidade} {
    @Column( name = "DT_EXCLUSAO" )
    private LocalDateTime dataExclusao;
}
```

- `@SQLDelete` substitui o DELETE físico por um UPDATE na coluna `DT_EXCLUSAO`.
- `@SQLRestriction` filtra automaticamente registros excluídos em todas as queries.
- Na Service, basta chamar `repository.deleteById(id)` — o soft delete é transparente.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `nome VARCHAR(100)` sem prefixo | `DS_NOME VARCHAR(100)` |
| `id BIGINT AUTO_INCREMENT` | `ID_ENTIDADE UUID defaultValueComputed="${uuid_function}"` |
| `DELETE FROM TB_ENTIDADE WHERE ID_ENTIDADE = ?` | Soft delete via `@SQLDelete` + `@SQLRestriction` |
| `VARCHAR(10000)` para textos longos | `TEXT` (prefixo `TX_`) para conteúdo longo |
| Tabela sem `DT_CRIACAO` e `DT_EXCLUSAO` | Toda tabela inclui as duas colunas de auditoria |
