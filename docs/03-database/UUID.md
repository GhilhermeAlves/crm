# UUID — Uso de UUIDs

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Formato](#formato)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o uso de UUIDs como identificadores únicos no sistema.

## Descrição

UUIDs (Universally Unique Identifiers) são usados como primary key em todas as tabelas. Isso permite geração distribuída de IDs sem coordenação central.

## Formato

### UUID v4 (Padrão)

```
550e8400-e29b-41d4-a716-446655440000
         ├─ 8 ─┤├─ 4 ─┤├─ 4 ─┤├─ 4 ─┤├─ 12 ─┤
```

### Geração

```sql
-- PostgreSQL
DEFAULT gen_random_uuid()

-- Java
UUID.randomUUID()

-- TypeScript
import { v4 as uuidv4 } from 'uuid';
uuidv4();
```

## Responsabilidades

- Garantir unicidade global de IDs
- Evitar exposição de sequência (security)
- Suportar geração distribuída
- Funcionar em ambientes multi-tenant

## Dependências

- [00-core/Decisions.md](../00-core/Decisions.md) — ADR-006

## Regras

- UUID v4 como padrão
- UUIDs são imutáveis após criação
- Nunca expor sequência interna
- URLs usam UUID (não ID numérico)
- UUIDs em URLs são case-insensitive
- Formato padrão: lowercase com hífens

## Futuras Melhorias

- ULID para ordenabilidade
- NanoID para URLs amigáveis
- UUID v7 (time-ordered) quando disponível

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
