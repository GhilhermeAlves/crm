# CRM — Registro de Decisões Arquiteturais

> Registro oficial das decisões arquiteturais importantes do CRM.
>
> Sempre que houver uma decisão arquitetural nova, registrar aqui com o próximo número DEC-XXX.

---

## DEC-001 — companies não representa Account

**Data:** 2026-09-03

`companies` é o **tenant** da aplicação SaaS (multi-tenancy, companyId, RLS).

Uma futura `Account` representa o **cliente/empresa dentro do CRM**.

Esses conceitos NÃO devem ser misturados. Uma `Account` deve ser uma entidade independente quando for implementada.

---

## DEC-002 — Preservar arquitetura existente

**Data:** 2026-09-03

Novos módulos devem seguir o padrão existente:

```text
frontend/
└── src/
    ├── app/(dashboard)/<modulo>/page.tsx
    └── features/<modulo>/
        ├── components/
        ├── hooks/
        ├── services/
        ├── types/
        └── schemas/
```

Backend deve seguir a arquitetura hexagonal:

```text
application/<modulo>/
presentation/rest/
infrastructure/<modulo>.persistence/
domain/
```

**Não criar arquitetura paralela.**

---

## DEC-003 — Preservar Design System

**Data:** 2026-09-03

Não criar um segundo design system. Reutilizar os componentes de `src/components/ui/`, `src/components/common/` e `src/components/feedback/`.

Novas telas devem preservar o padrão visual atual (PageTitle, space-y-6, Card, shadcn/ui, Tailwind, lucide-react, dark mode, pt-BR, BRL).

**Regra:** NÃO criar um novo componente visual quando já existir um componente equivalente.

---

## DEC-004 — Documentação obrigatória

**Data:** 2026-09-03

Toda evolução relevante do CRM deve **atualizar a documentação**.

Sempre registrar em `CRM-CHANGELOG.md` e, quando houver decisão arquitetural, em `CRM-DECISIONS.md`.

Antes de implementar, consultar `CRM-ARCHITECTURE.md`, `CRM-DECISIONS.md` e `CRM-CHANGELOG.md`.

---

## DEC-005 — Manter o padrão Feature → Hook → Service → API

**Data:** 2026-09-03

O fluxo oficial de dados do frontend é:

```text
page.tsx
 → feature component
 → hook TanStack Query
 → feature service
 → src/lib/api.ts
 → auth-service / BFF
 → backend Controller → UseCase → Repository → PostgreSQL
```

Toda chamada de dados deve passar por `src/lib/api.ts`. **Não** criar chamadas fora desse caminho.

---

## DEC-006 — Backend como autoridade de segurança

**Data:** 2026-09-03

As permissões de menu exibidas no frontend são **UX apenas**. A autorização final é sempre validada pelo backend (`@PreAuthorize`) e reforçada por RLS.

O frontend não deve confiar exclusivamente nas permissões exibidas para liberar ações.

---

## DEC-007 — Multi-tenancy e RLS

**Data:** 2026-09-03

- Respeitar `companyId`.
- Respeitar `TenantContext`.
- Respeitar RLS.
- Respeitar `@PreAuthorize`.
- Não acessar o banco diretamente pelo frontend.
- Criar permissões quando um novo módulo exigir.

---

## DEC-008 — Relacionamentos por UUID/FK

**Data:** 2026-09-03

As entidades do CRM relacionam-se por UUID/FK no PostgreSQL.

O modelo conceitual atual é:

```text
CONTACT
   ├── LEAD
   ├── OPPORTUNITY (PIPELINE, STAGE)
   ├── TASK
   └── ACTIVITY
```

Modelo futuro (conceitual, NÃO implementar agora) adiciona `ACCOUNT`, `PRODUCTS`, `PROPOSALS`, `PROJECTS` e `ATTACHMENTS`.

---

## DEC-009 — Biblioteca ≠ Componente reutilizável

**Data:** 2026-09-03

A presença de uma biblioteca instalada (Radix, cmdk) **não** significa que existe um componente reutilizável próprio do projeto.

Ao validar (Tabs, Pagination, Combobox/Command), deve-se verificar a existência de um wrapper reutilizável em `src/components/`. Caso exista somente a biblioteca, registrar como:

```
BIBLIOTECA EXISTENTE / COMPONENTE DO PROJETO AUSENTE
```

### Status atual (2026-09-03)

| Item       | Status                                                      |
| ---------- | ----------------------------------------------------------- |
| Tabs       | BIBLIOTECA (Radix) / COMPONENTE DO PROJETO AUSENTE          |
| Pagination | AUSENTE (sem padrão genérico reutilizável)                  |
| Combobox   | BIBLIOTECA (cmdk) / COMPONENTE DO PROJETO AUSENTE           |

---

## DEC-010 — Entidades de tenant ≠ Entidades de cliente do CRM

**Data:** 2026-09-03

Existe uma distinção clara entre:

- **Entidades de tenant** (infraestrutura do SaaS): `companies`, `users`, `members`, `roles`, `permissions`, `invitations`, `tenants`.
- **Entidades de negócio do CRM** (clientes e relacionamento): `lead`, `contact`, `opportunity`, `pipeline`, `stage`, `task`, `activity`, `campaign`, `channel`, `workflow`.

Esses grupos devem evoluir de forma coerente, mas sempre respeitando a multi-tenancy (`companyId`) por cima.

---

## DEC-011 — Contas / Accounts como evolução futura

**Data:** 2026-09-03

`Contas / Accounts` é um conceito **novo e necessário**, mas ainda **AUSENTE**.

Será a entidade que representa o cliente/empresa dentro do CRM, de forma independente do tenant. Deverá seguir o fluxo e a arquitetura oficiais (Feature → Hook → Service → API), usar os componentes existentes e preservar o padrão visual.

A implementação deve ser planejada (Fase 2 do roadmap) e registrada nesta documentação.

---

## DEC-012 — Sem refatoração sem necessidade

**Data:** 2026-09-03

Não refatorar código existente sem real necessidade. Preservar a arquitetura e o padrão visual atuais.

Qualquer alteração estrutural relevante deve ser justificada e registrada na documentação (arquitetura, changelog, decisões).
