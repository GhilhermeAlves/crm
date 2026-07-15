# SoftDelete — Soft Delete

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Implementação](#implementação)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a estratégia de soft delete utilizada no banco de dados.

## Descrição

Soft delete preserva dados históricos ao invés de deletar fisicamente. Registros são marcados com `deleted_at` e excluídos das queries normais via filtro.

## Implementação

### Coluna

```sql
ALTER TABLE {table} ADD COLUMN deleted_at TIMESTAMP NULL;
```

### Filtro Automático

```sql
-- View para dados ativos
CREATE VIEW {table}_active AS
SELECT * FROM {table} WHERE deleted_at IS NULL;

-- Ou via filter no ORM
@Entity
@Table(name = "{table}")
@EntityListeners(SoftDeleteListener.class)
public class BaseEntity {
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

### Soft Delete

```sql
-- Marcar como deletado
UPDATE {table} SET deleted_at = NOW() WHERE id = '{id}';

-- Nunca: DELETE FROM {table} WHERE id = '{id}';
```

## Responsabilidades

- Preservar dados históricos
- Permitir recuperação de registros
- Manter integridade referencial
- Suportar auditoria

## Dependências

- [Audit.md](./Audit.md) — Auditoria de exclusões
- [00-core/Architecture.md](../00-core/Architecture.md) — Princípios

## Regras

- Toda tabela principal tem coluna deleted_at
- Queries normais filtram deleted_at IS NULL
- Admin pode visualizar registros deletados
- Registros deletados são preservados por 90 dias
- Após 90 dias, hard delete (cleanup automático)
- Foreign keys não são afetadas por soft delete

## Futuras Melhorias

- Hard delete automático após período
- Recovery de registros deletados
- Backup de registros antes do hard delete
- Dashboard de registros deletados

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
