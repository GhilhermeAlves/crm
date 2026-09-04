# CRM — Arquitetura

> Documento oficial de referência da arquitetura funcional do CRM.
> Deve ser consultado e atualizado sempre que houver evolução do sistema.

---

## 1. Visão geral

O CRM é uma aplicação SaaS multi-tenant que atualmente oferece os seguintes módulos:

- **Dashboard** — visão executiva com KPIs.
- **Leads** — captação e gestão de leads.
- **Contatos / Customer360** — gestão de contatos com visão unificada.
- **Pipeline / Negociações** — gestão de oportunidades por estágios.
- **Tarefas** — gestão de tarefas operacionais.
- **Atividades / Timeline** — registro e histórico de interações.
- **Automações** — workflows de automação.
- **Notificações** — avisos e alertas ao usuário.
- **Inbox / Omnichannel** — caixa de entrada unificada de comunicação.
- **Canais** — gestão de canais de comunicação.
- **Campanhas** — gestão de campanhas.
- **Relatórios** — painéis analíticos.
- **Assistente IA** — assistente virtual (Léo).

### Importante

> **`companies` representa a empresa/tenant do SaaS e NÃO deve ser tratado como `Account/Conta de cliente`.**
>
> Uma futura entidade `Account` (conta de cliente) será um conceito de negócio do CRM, independente do tenant.

---

## 2. Mapa atual do CRM

### 2.1 Menu atual

```text
CRM
├── Leads
├── Contatos
├── Pipeline
├── Tarefas
├── Timeline
├── Automações
├── Notificações
└── Léo · Assistente IA

Comunicação
├── Inbox
├── Canais
└── Campanhas

Análise
└── Relatórios
```

### 2.2 Menu completo (inclui áreas administrativas)

```text
Dashboard

Administração
├── Empresas            (tenants)
├── Membros             (members)
├── Convites            (invitations)
├── Permissões          (permissions / RBAC)
└── Arquivos            (storage)

Segurança
├── Usuários            (settings/users)
└── Perfis              (settings/roles)

CRM
├── Leads
├── Contatos
├── Pipeline
├── Tarefas
├── Timeline
├── Automações
├── Notificações
└── Léo · Assistente IA

Comunicação
├── Inbox
├── Canais
└── Campanhas

Análise
└── Relatórios

Sistema
├── Auditoria
└── Configurações
```

### 2.3 Rotas atuais (frontend)

| Rota | Página |
| ---- | ------ |
| `/crm` | Página inicial do CRM (central de módulos) |
| `/dashboard` | Dashboard |
| `/leads` | Lista de Leads |
| `/leads/new` | Novo Lead |
| `/leads/[id]` | Detalhe do Lead |
| `/leads/[id]/edit` | Editar Lead |
| `/contacts` | Lista de Contatos |
| `/contacts/[id]` | Detalhe do Contato (Customer360) |
| `/pipeline` | Pipeline de Negociações |
| `/tasks` | Tarefas |
| `/activities` | Timeline / Atividades |
| `/workflows` | Automações |
| `/notifications` | Notificações |
| `/assistant` | Assistente IA (Léo) |
| `/inbox` | Inbox |
| `/channels` | Canais |
| `/campaigns` | Campanhas |
| `/campaigns/new` | Nova Campanha |
| `/campaigns/[id]` | Detalhe da Campanha |
| `/reports` | Relatórios |
| `/audit` | Auditoria |
| `/audit/[id]` | Detalhe da Auditoria |
| `/invitations` | Convites |
| `/members` | Membros |
| `/storage` | Arquivos |
| `/tenants` | Empresas (tenant) |
| `/tenants/new` | Nova Empresa |
| `/tenants/[id]` | Detalhe da Empresa |
| `/tenants/[id]/edit` | Editar Empresa |
| `/users` | Usuários |
| `/users/new` | Novo Usuário |
| `/users/[id]` | Detalhe do Usuário |
| `/users/[id]/edit` | Editar Usuário |
| `/profile` | Perfil |
| `/settings/users` | Configurações — Usuários |
| `/settings/roles` | Configurações — Perfis |

Rotas de autenticação/onboarding (fora do layout autenticado):

- `/login`, `/register`, `/forgot-password`, `/reset-password`
- `/onboarding`
- `/invitations/accept`

### 2.4 Permissões atuais (sidebar / RBAC)

As permissões de menu são controladas por grupo + item:

- **dashboard**: `dashboard:page:view`
- **companies (empresas/tenant)**: `company:view`
- **members / convites**: `membership:view`
- **permissões / perfis**: `role:read`
- **segurança — usuários / perfis**: `security:page:view`
- **leads**: `lead:page:view`
- **contatos**: `contact:page:view`
- **pipeline**: `pipeline:page:view`
- **tarefas**: `task:page:view`
- **timeline**: `activity:page:view`
- **automações**: `workflow:page:view`
- **notificações**: `notification:page:view`
- **assistente IA**: `ai:chat`
- **inbox / canais (omnichannel)**: `omnichannel:page:view`
- **campanhas**: `campaign:page:view`
- **auditoria**: `audit:page:view`

> **Observação:** a visibilidade do menu é uma UX baseada nas permissões carregadas no frontend, mas a **autorização final é sempre validada pelo backend** (`@PreAuthorize`). Quando não há permissões de negócio carregadas, o menu permanece todo visível por padrão.

---

## 3. Módulos existentes

Cada feature segue o padrão Feature-driven:

```text
features/<modulo>/
├── components/
├── hooks/
├── services/
├── types/
└── schemas/   (quando aplicável)
```

### 3.1 Leads

- **Rota:** `/leads`, `/leads/new`, `/leads/[id]`, `/leads/[id]/edit`
- **Feature:** `src/features/leads/` (components, hooks, schemas, services, types)
- **Componentes:** LeadTable, LeadForm, LeadFilters, LeadBadges, DeleteLeadDialog
- **Funcionalidades:** listagem com filtros, criação, edição, detalhe, badges de estado, exclusão.
- **Fluxo:** página → componente → hook TanStack Query → service → `src/lib/api.ts` → BFF/auth-service → backend.
- **Entidade/tabela:** `lead` (representação no backend).

### 3.2 Contatos / Customer360

- **Rota:** `/contacts`, `/contacts/[id]`
- **Feature:** `src/features/contacts/` (components, hooks, services, types)
- **Componentes:** ContactTable, ContactSummaryCard, CreateContactDialog, TimelinePanel, TasksPanel, OpportunitiesPanel, NextActionCard
- **Funcionalidades:**
  - Lista de contatos.
  - Detalhe do contato (Customer360).
  - Painel de oportunidades relacionadas.
  - Painel de tarefas relacionadas.
  - Timeline de atividades.
  - Próxima ação sugerida (NextActionCard).
- **Entidade/tabela:** `contact`.

### 3.3 Pipeline / Negociações

- **Rota:** `/pipeline`
- **Feature:** `src/features/pipeline/` (components, hooks, schemas, services, types)
- **Componentes:** PipelineBoard, OpportunityCard, PipelineMetricsStrip, CreateOpportunityForm, CreateOpportunityDialog, LostReasonDialog.
- **Funcionalidades:**
  - Pipelines e stages.
  - Opportunities (negociações).
  - Kanban visual.
  - Métricas (PipelineMetricsStrip).
  - Movimentação entre stages (atualmente por botões/actions).
  - Registro de ganho/perda (LostReasonDialog).
  - Histórico.
- **Entidade/tabela:** `opportunity` (negociação), relacionada a `pipeline` e `stage`.

### 3.4 Atividades / Timeline

- **Rota:** `/activities`
- **Feature:** `src/features/activities/` (components, hooks, schemas, services, types)
- **Funcionalidades:** registro e visualização da timeline de atividades.
- **Tipos existentes:**
  ```text
  CALL
  MEETING
  EMAIL
  MESSAGE
  NOTE
  PROPOSAL
  FOLLOW_UP
  OTHER
  ```

### 3.5 Tarefas

- **Rota:** `/tasks`
- **Feature:** `src/features/tasks/` (components, hooks, schemas, services, types)
- **Funcionalidades:**
  - Status.
  - Prioridade.
  - Vencimento (due date).
  - Responsável (assignee/owner).
  - Relacionamento com contato.
  - Relacionamento com oportunidade.

### 3.6 Dashboard

- **Rota:** `/dashboard`
- **Feature:** `src/features/dashboard/` (components, hooks, services, types)
- **Funcionalidades:**
  - KPIs.
  - Itens de atenção.
  - Tarefas do dia.
  - Atividades recentes.

### 3.7 Automações (Workflows)

- **Rota:** `/workflows` (e sub-rotas de criar/editar)
- **Feature:** `src/features/workflows/` (components, hooks, schemas, services, types)
- **Funcionalidades:** criação e gestão de workflows de automação.

### 3.8 Notificações

- **Rota:** `/notifications`
- **Feature:** `src/features/notifications/` (components, hooks, services, types, lib)

### 3.9 Inbox / Omnichannel

- **Rota:** `/inbox`, `/channels`
- **Feature:** `src/features/omnichannel/` (components, hooks, services, types)
- **Componentes:** ChannelFormDialog, ChannelStatusBadge, ChatThread, ConversationList.
- **Funcionalidades:** caixa de entrada unificada, gestão de canais, conversas (threads).

### 3.10 Campanhas

- **Rota:** `/campaigns`, `/campaigns/new`, `/campaigns/[id]`
- **Feature:** `src/features/campaigns/`

### 3.11 Relatórios

- **Rota:** `/reports`
- **Feature:** `src/features/analytics/` (services, types, hooks) — analítico.

### 3.12 Assistente IA (Léo)

- **Rota:** `/assistant`
- **Feature:** `src/features/ai/` (components, hooks, services, types)
- **Componentes:** AiChatAssistant, AiConversationList, AiMessageBubble, AiAnalysisCard, AiActionProposalCard.
- **Permissão:** `ai:chat`.

### 3.13 Módulos administrativos

- **RBAC / Funções:** Rota `/permissions`, `/roles*`, `/settings/roles` — feature `rbac`.
- **Usuários:** `/users*`, `/settings/users` — feature `users`.
- **Tenants (Empresas):** `/tenants*` — feature `tenants` (entidade `companies`/tenant).
- **Membros:** `/members` — feature `members`.
- **Convites:** `/invitations*` — feature `invitations`.
- **Auditoria:** `/audit` — feature `audit`.
- **Arquivos/Storage:** `/storage` — feature `storage`.
- **Auth:** feature `auth`.
- **Onboarding:** feature `onboarding`.

### 3.14 Página inicial do CRM

- **Rota:** `/crm`
- **Feature:** `src/features/crm/components/CrmModuleCard.tsx`
- **Componentes:** CrmModuleCard (cards de navegação específicos da tela).
- **Funcionalidades:** ponto central de acesso aos módulos comerciais.
  - Título: "CRM" (via `PageTitle`).
  - Descrição: "Central de gestão comercial e relacionamento com clientes."
  - Cards para: Contatos, Negociações, Leads, Contas, Projetos de clientes, Atividades e Painel de vendas.
- **Navegação por módulo:**
  - Contatos → `/contacts` (permissão `contact:page:view`).
  - Negociações → `/pipeline` (permissão `pipeline:page:view`).
  - Leads → `/leads` (permissão `lead:page:view`).
  - Contas → **sem rota ainda** (card preparado, "Em breve"; depende de implementação futura).
  - Projetos de clientes → **sem rota ainda** (card preparado, "Em breve"; depende de implementação futura).
  - Atividades → `/activities` (permissão `activity:page:view`).
  - Painel de vendas → `/reports` (permissão `analytics:read`), pois o painel analítico existente fornece os indicadores de desempenho comercial.
- **Permissões:** reutiliza as permissões `contact:page:view`, `pipeline:page:view`, `lead:page:view`, `activity:page:view` e `analytics:read` para controlar a visibilidade dos cards. Não cria permissões novas.
- **Integração futura:** os módulos **Contas** (`/accounts`) e **Projetos de clientes** (`/projects`) ainda não existem; os cards estão preparados para as rotas futuras, sem inventar dados ou endpoints.

---

## 4. O que existe x o que falta

| Funcionalidade         | Status    | Observação                     |
| ---------------------- | --------- | ------------------------------ |
| Leads                  | EXISTENTE |                                |
| Contatos               | EXISTENTE |                                |
| Customer360            | EXISTENTE |                                |
| Pipeline               | EXISTENTE |                                |
| Negociações            | EXISTENTE | Opportunity                    |
| Atividades             | EXISTENTE |                                |
| Tarefas                | EXISTENTE |                                |
| Dashboard              | EXISTENTE |                                |
| Página inicial do CRM  | EXISTENTE | `/crm` — central de módulos    |
| Contas / Accounts      | AUSENTE   | Novo conceito necessário       |
| Produtos               | AUSENTE   |                                |
| Serviços               | AUSENTE   |                                |
| Preços                 | AUSENTE   |                                |
| Propostas              | AUSENTE   |                                |
| Projetos de clientes   | AUSENTE   |                                |
| Equipes                | AUSENTE   |                                |
| Comentários            | AUSENTE   |                                |
| Histórico genérico     | PARCIAL   |                                |
| Anexos por registro    | PARCIAL   | Storage existente              |
| Campos personalizados  | PARCIAL   |                                |
| Painel de vendas       | PARCIAL   | Analytics/dashboard existentes |
| Drag & Drop Pipeline   | PARCIAL   | Hoje movimentação por botões   |
| Paginação reutilizável | AUSENTE   |                                |
| Tabs reutilizável      | VALIDAR   |                                |
| Combobox reutilizável  | VALIDAR   |                                |

---

## 5. Arquitetura obrigatória para novos módulos

Todo novo módulo CRM deverá seguir o padrão:

```text
frontend/
└── src/
    ├── app/(dashboard)/<modulo>/
    │   └── page.tsx
    │
    └── features/<modulo>/
        ├── components/
        ├── hooks/
        ├── services/
        ├── types/
        └── schemas/
```

O backend deverá seguir a arquitetura existente:

```text
application/<modulo>/
presentation/rest/
infrastructure/<modulo>.persistence/
domain/
```

> **Não criar uma arquitetura paralela.**

---

## 6. Design System obrigatório

Novas telas devem reutilizar os componentes existentes.

**Priorizar:**

```text
src/components/ui/
src/components/common/
src/components/feedback/
```

**Componentes reutilizáveis existentes:**

| Camada | Componentes |
| ------ | ----------- |
| `ui/` | alert-dialog, avatar, badge, button, card, checkbox, dialog, dropdown-menu, form, input, label, scroll-area, select, separator, sheet, skeleton, switch, table, textarea, tooltip |
| `common/` | ActionButton, BadgeStatus, CardStat, ConfirmDialog, EmptyState, ErrorCard, FilterBar, NoResults, PageTitle, RetryButton, SearchInput, SectionTitle, ThemeToggle |
| `feedback/` | ErrorPage, ErrorBoundary, SkeletonTable, SkeletonList, SkeletonForm, SkeletonCard |

**Reutilizar preferencialmente:**

- Card
- CardStat
- Button
- Badge
- BadgeStatus
- Dialog
- ConfirmDialog
- Sheet
- Select
- DropdownMenu
- Form
- Input
- SearchInput
- FilterBar
- EmptyState
- NoResults
- ErrorCard
- Skeletons
- ScrollArea
- Avatar

> **Regra:** NÃO criar um novo componente visual quando já existir um componente equivalente.

---

## 7. Padrão visual obrigatório

Novas telas devem preservar:

- `PageTitle`
- `space-y-6`
- Card padrão
- shadcn/ui
- Tailwind
- lucide-react (ícones)
- badges existentes
- estados de loading existentes (Skeletons)
- estados vazios existentes (EmptyState / NoResults)
- mensagens de erro existentes (ErrorCard / ErrorPage)
- responsividade atual
- dark mode
- formatação pt-BR
- valores monetários em BRL

> **Não criar um novo Design System.**

---

## 8. Fluxo obrigatório Frontend → Backend

Fluxo oficial de chamada de dados:

```text
page.tsx
 ↓
feature component
 ↓
hook TanStack Query
 ↓
feature service
 ↓
src/lib/api.ts
 ↓
auth-service / BFF
 ↓
backend Controller
 ↓
UseCase
 ↓
Repository
 ↓
PostgreSQL
```

> Novas implementações devem respeitar esse fluxo. Nenhuma chamada fora de `src/lib/api.ts`.

---

## 9. Multi-tenancy e segurança

Regra obrigatória:

- Não acessar diretamente o banco pelo frontend.
- Não criar chamadas fora de `src/lib/api.ts`.
- Respeitar `companyId`.
- Respeitar `TenantContext`.
- Respeitar RLS (Row Level Security).
- Respeitar `@PreAuthorize` (backend como autoridade de segurança).
- Criar permissões quando um novo módulo exigir.
- Nunca tratar `companies` como Account de CRM.
- Account/Conta de cliente deve ser uma entidade independente quando for implementada.

---

## 10. Backend como autoridade de segurança

- As permissões de menu no frontend são **UX apenas**.
- A validação/segurança final é sempre feita pelo backend (`@PreAuthorize`) e reforçada por RLS.
- O frontend não deve confiar exclusivamente nas permissões exibidas para liberar ações.

---

## 11. Relacionamentos conceituais do CRM

### 11.1 Modelo atual

```text
CONTACT
   │
   ├── LEAD
   │
   ├── OPPORTUNITY
   │      ├── PIPELINE
   │      └── STAGE
   │
   ├── TASK
   │
   └── ACTIVITY
```

### 11.2 Modelo futuro planejado (conceitual — NÃO implementar agora)

```text
ACCOUNT
   │
   ├── CONTACTS
   │
   ├── OPPORTUNITIES
   │       ├── PIPELINE
   │       ├── STAGES
   │       ├── PRODUCTS
   │       └── PROPOSALS
   │
   ├── PROJECTS
   │       ├── TASKS
   │       ├── ACTIVITIES
   │       └── ATTACHMENTS
   │
   └── ACTIVITIES
```

> Este modelo é **conceitual** e NÃO deve ser implementado neste momento.

### 11.3 Tenant (`companies`) ≠ Account (cliente)

- `companies` = tenant do SaaS.
- `Account` (futura) = cliente/empresa dentro do CRM.
- São conceitos distintos e não devem ser misturados.

---

## 12. Validação de componentes "PRECISA SER VALIDADO"

> Regra: a presença de uma **biblioteca instalada** NÃO significa que existe um **componente reutilizável do projeto**.

### 12.1 Tabs

```text
Existe?           NÃO confirmado (sem wrapper reutilizável próprio)
Arquivo:          — 
Onde é utilizado? —
É reutilizável?   —
Precisa ser criado? VALIDAR / possível criação de wrapper reutilizável de Tabs
```

Observação: a biblioteca **Radix** (base do shadcn) possui capacidades de `Tabs`, porém **não foi confirmado um wrapper reutilizável próprio de Tabs** no projeto.

**Status:** `BIBLIOTECA EXISTENTE / COMPONENTE DO PROJETO AUSENTE`

### 12.2 Pagination

```text
Existe?           NÃO confirmado (sem componente/padrão genérico reutilizável)
Arquivo:          —
Onde é utilizado? Paginação pontual em algumas telas (sem padrão compartilhado)
É reutilizável?   —
Precisa ser criado? SIM (padrão genérico reutilizável de paginação)
```

**Status:** `AUSENTE como componente reutilizável`

### 12.3 Combobox / Command

```text
Existe?           NÃO confirmado (sem componente reutilizável próprio)
Arquivo:          —
Onde é utilizado? —
É reutilizável?   —
Precisa ser criado? VALIDAR / possível criação de wrapper reutilizável de Combobox
```

Observação: a biblioteca **cmdk** está instalada, porém **não foi confirmado um componente Combobox reutilizável próprio** no projeto.

**Status:** `BIBLIOTECA EXISTENTE / COMPONENTE DO PROJETO AUSENTE`

### Resumo da validação

| Item      | Biblioteca instalada | Componente reutilizável do projeto | Status                                        |
| --------- | -------------------- | ---------------------------------- | --------------------------------------------- |
| Tabs      | Radix (shadcn)       | NÃO confirmado                     | BIBLIOTECA / COMPONENTE DO PROJETO AUSENTE     |
| Pagination| —                    | NÃO confirmado                     | AUSENTE (padrão genérico)                     |
| Combobox  | cmdk                 | NÃO confirmado                     | BIBLIOTECA / COMPONENTE DO PROJETO AUSENTE     |

---

## 13. Roadmap oficial

### Fase 1 — Organização do CRM

- Consolidar documentação.
- Padronizar componentes reutilizáveis.
- Validar Tabs.
- Validar Combobox.
- Criar paginação reutilizável.
- Centralizar formatadores (pt-BR / BRL).

### Fase 2 — Contas / Accounts

- Criar o conceito de empresa/cliente dentro do CRM sem confundir com tenant.

### Fase 3 — Evolução de Contatos

- Relacionamento com Account.
- Campos personalizados.
- Anexos.
- Histórico.

### Fase 4 — Evolução das Negociações

- Melhorias no pipeline.
- Drag & drop, se tecnicamente adequado.
- Produtos.
- Serviços.
- Valores.
- Propostas.

### Fase 5 — Projetos de Clientes

- Projetos.
- Tarefas.
- Atividades.
- Responsáveis.
- Acompanhamento.

### Fase 6 — Painel de Vendas

- KPIs.
- Gráficos.
- Forecast.
- Conversão.
- Win rate.
- Ticket médio.
- Ciclo de vendas.
- Filtros por período.
- Filtros por responsável/equipe.

---

## 14. Tecnologias utilizadas

- **Frontend:** Next.js (App Router), React, TypeScript, Tailwind CSS, shadcn/ui, lucide-react, TanStack Query, Radix (primitivos), cmdk (instalado).
- **Backend:** arquitetura hexagonal (application / presentation / infrastructure / domain).
- **Persistência:** PostgreSQL com relacionamentos por UUID/FK.
- **Segurança:** multi-tenancy via companyId, RLS, `@PreAuthorize`, RBAC.
- **Comunicação frontend/backend:** via `src/lib/api.ts` → auth-service / BFF.

---

## 15. REGRA MAIS IMPORTANTE — ATUALIZAÇÃO DA DOCUMENTAÇÃO

> Toda nova implementação, alteração estrutural ou evolução funcional do CRM DEVE atualizar esta documentação.

Sempre que for criado:

- novo módulo
- nova tela
- nova rota
- nova entidade
- nova tabela
- nova migration
- novo endpoint
- novo relacionamento
- novo componente reutilizável
- nova permissão
- nova funcionalidade relevante

deve existir uma atualização correspondente na documentação.

---

# INSTRUÇÃO PARA FUTURAS IMPLEMENTAÇÕES

Antes de implementar qualquer nova funcionalidade CRM:

1. Ler `docs/crm/CRM-ARCHITECTURE.md`.
2. Ler `docs/crm/CRM-DECISIONS.md`.
3. Verificar `docs/crm/CRM-CHANGELOG.md`.
4. Identificar módulos e componentes existentes que podem ser reutilizados.
5. Não criar arquitetura paralela.
6. Não criar Design System paralelo.
7. Não duplicar componentes existentes.
8. Preservar o padrão visual atual.
9. Após concluir a implementação, atualizar a documentação.
10. Registrar a alteração no `CRM-CHANGELOG.md`.
11. Se houver uma decisão arquitetural nova, registrar no `CRM-DECISIONS.md`.

> Esta regra deve ser considerada **obrigatória** para qualquer futura evolução do CRM.
