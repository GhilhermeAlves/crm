# 03-Database — Documentação de Banco de Dados

## Índice

| Documento | Descrição |
|---|---|
| [Overview.md](./Overview.md) | Visão geral do banco de dados |
| [ERD.md](./ERD.md) | Diagrama entidade-relacionamento |
| [Entities.md](./Entities.md) | Entidades principais |
| [Relationships.md](./Relationships.md) | Relacionamentos |
| [Indexes.md](./Indexes.md) | Índices |
| [UUID.md](./UUID.md) | Uso de UUIDs |
| [SoftDelete.md](./SoftDelete.md) | Soft delete |
| [Audit.md](./Audit.md) | Auditoria no banco |
| [Migrations.md](./Migrations.md) | Migrations com Flyway |
| [Backup.md](./Backup.md) | Backup e recuperação |
| [Performance.md](./Performance.md) | Performance e otimização |

---

## Objetivo

Documentar toda a modelagem de dados, estrutura, indices e estratégias de banco de dados.

## Descrição

O banco de dados é PostgreSQL 16 com schemas separados por tenant. Usa Flyway para migrations e UUID como primary key.

## Regras

- UUID como primary key em todas as tabelas
- Soft delete (coluna deleted_at)
- Timestamps obrigatórios (created_at, updated_at)
- Naming: snake_case para tabelas e colunas
- Migrations são backward-compatible

## Dependências

- [00-core/Architecture.md](../00-core/Architecture.md) — Arquitetura
- [01-backend/Overview.md](../01-backend/Overview.md) — Backend

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
