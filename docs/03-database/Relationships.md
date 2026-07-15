# Relationships — Relacionamentos

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Tipos de Relacionamento](#tipos-de-relacionamento)
- [Tabela de Relacionamentos](#tabela-de-relacionamentos)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todos os relacionamentos entre entidades do banco de dados.

## Descrição

Relacionamentos definem como as tabelas se conectam. Cada foreign key é documentada com tipo de cardinalidade e regras de cascade.

## Tipos de Relacionamento

| Tipo | Cardinalidade | Exemplo |
|---|---|---|
| One-to-One | 1:1 | User ↔ UserSettings |
| One-to-Many | 1:N | Company → Contacts |
| Many-to-Many | N:N | Contacts ↔ Tags |

## Tabela de Relacionamentos

| Tabela Origem | Coluna FK | Tabela Referência | Cardinalidade | On Delete |
|---|---|---|---|---|
| users | company_id | companies | N:1 | RESTRICT |
| contacts | company_id | companies | N:1 | RESTRICT |
| leads | company_id | companies | N:1 | RESTRICT |
| leads | contact_id | contacts | N:1 | CASCADE |
| pipelines | company_id | companies | N:1 | RESTRICT |
| stages | pipeline_id | pipelines | N:1 | CASCADE |
| opportunities | pipeline_id | pipelines | N:1 | RESTRICT |
| opportunities | stage_id | stages | N:1 | RESTRICT |
| opportunities | contact_id | contacts | N:1 | CASCADE |
| conversations | company_id | companies | N:1 | RESTRICT |
| conversations | contact_id | contacts | N:1 | CASCADE |
| messages | conversation_id | conversations | N:1 | CASCADE |
| campaigns | company_id | companies | N:1 | RESTRICT |
| automations | company_id | companies | N:1 | RESTRICT |
| audit_logs | company_id | companies | N:1 | RESTRICT |
| audit_logs | user_id | users | N:1 | SET NULL |
| contact_tags | contact_id | contacts | N:N | CASCADE |
| contact_tags | tag_id | tags | N:N | CASCADE |
| user_roles | user_id | users | N:N | CASCADE |
| user_roles | role_id | roles | N:N | RESTRICT |

## Regras de Cascade

| Regra | Quando Usar |
|---|---|
| RESTRICT | Não permitir delete se existirem referências |
| CASCADE | Deletar registros filhos junto com o pai |
| SET NULL | Manter filho, remover referência |

## Responsabilidades

- Documentar todas as foreign keys
- Definir regras de cascade adequadas
- Garantir integridade referencial

## Dependências

- [Entities.md](./Entities.md) — Definição das tabelas
- [ERD.md](./ERD.md) — Diagrama visual

## Regras

- Toda FK deve ter constraint nomeada
- Índices automáticos em colunas FK
- Nunca usar CASCADE em tabelas de auditoria
- RESTRICT é o padrão quando não há regra específica
- ON DELETE deve ser explícito em toda FK

## Futuras Melhorias

- Diagrama visual de relacionamentos
- Validação automática de integridade
- Documentação de triggers e views

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
