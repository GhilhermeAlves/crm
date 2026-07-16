# Database Prompt - Migrations & Schema

## Quando utilizar

- Criando ou modificando esquema do banco de dados
- Criando novas migrations Flyway
- Criando entidades JPA

## Objetivo

Criar migrations Flyway e entidades JPA seguindo convenções do projeto.

## Entrada esperada

- Tabelas necessárias
- Colunas e tipos
- Relacionamentos (1:1, 1:N, N:N)
- Índices necessários

## Resultado esperado

- Arquivo de migration SQL (Flyway)
- Entidade JPA correspondente

## Arquivos normalmente envolvidos

```
backend/src/main/resources/db/migration/
  └── V{next_version}__{description}.sql

backend/src/main/java/com/becommerce/crm/infrastructure/persistence/entity/
  └── {Entity}JpaEntity.java
```

## Boas práticas

- **UUID v4 PKs**: Sempre usar `UUID` como chave primária com `gen_random_uuid()`.
- **snake_case naming**: Tabelas em `snake_case` plural, colunas em `snake_case`.
- **Timestamps obrigatórios**: Sempre incluir `created_at`, `updated_at` com `DEFAULT NOW()`.
- **Soft delete**: Campo `deleted_at` nullable, indexado.
- **Constraints nomeadas**: Nomear todas as constraints (FK, UNIQUE, CHECK).
- **Indexar FKs**: Criar índice em todas as foreign keys.
- **Nunca alterar migrations já executadas**: Sempre criar nova migration.
- **Versionamento**: Usar `V{timestamp}__{description}.sql`.
- **Idempotência**: Migrations devem ser seguras para re-execução.
- **Entidades JPA**: Usar `@GeneratedValue(strategy = GenerationType.UUID)`.

## Exemplo de uso

```
Criar tabela Categories com:
- id (UUID PK)
- name (VARCHAR 100 NOT NULL UNIQUE)
- description (TEXT)
- active (BOOLEAN DEFAULT true)
- created_at (TIMESTAMP DEFAULT NOW())
- updated_at (TIMESTAMP DEFAULT NOW())
- deleted_at (TIMESTAMP NULL)
- Índice em name
```
