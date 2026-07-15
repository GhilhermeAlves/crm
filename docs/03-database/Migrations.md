# Migrations — Migrations com Flyway

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Convenções](#convenções)
- [Fluxo](#fluxo)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a estratégia de migrations do banco de dados usando Flyway.

## Descrição

Flyway gerencia versões do schema do banco de dados. Cada migration é um arquivo SQL versionado que pode ser aplicado ou revertido.

## Convenções

### Nomenclatura de Arquivos

```
V{version}__{description}.sql

Exemplos:
V1__create_users_table.sql
V2__add_email_to_contacts.sql
V3__create_leads_table.sql
```

### Estrutura de Arquivo

```sql
-- V{version}__{description}.sql

-- Criar tabela
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Criar índice
CREATE INDEX idx_users_email ON users(email);

-- Criar constraint
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE(email);
```

## Fluxo

```
1. Desenvolvedor cria arquivo de migration
        │
2. Migration é testada localmente
        │
3. Migration é commitada no git
        │
4. CI roda migration em banco de teste
        │
5. Deploy aplica migration em staging
        │
6. Deploy aplica migration em produção
        │
7. Flyway registra versão aplicada
```

## Responsabilidades

- Versionar schema do banco de dados
- Garantir migrations reprodutíveis
- Suportar rollback quando necessário
- Registrar histórico de mudanças

## Dependências

- [Overview.md](./Overview.md) — Stack de banco de dados
- [00-core/TechStack.md](../00-core/TechStack.md) — Flyway

## Regras

- Migrations são imutáveis após commit
- Nunca editar migration já aplicada
- Criar nova migration para correções
- Testar migration antes do merge
- Migrations devem ser backward-compatible
- Usar transactions quando possível
- Não usar dados sensíveis em migrations

## Futuras Melhorias

- Migration testing automatizado
- Rollback scripts para cada migration
- Dry-run antes de aplicar
- Dashboard de status de migrations
- Multi-tenant migration orchestration

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
