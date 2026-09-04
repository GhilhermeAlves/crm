# Frontend Architecture

Regras definitivas para a organização do frontend do CRM (Next.js App Router).

## Princípio central

```
APP         = rotas
FEATURES    = domínio / lógica
COMPONENTS  = componentes visuais reutilizáveis
LIB         = infraestrutura e utilitários
```

- **Nunca** criar uma pasta apenas porque existe uma página com aquele nome.
- **Nunca** criar pastas vazias "para preparar o futuro".
- **Nunca** duplicar componentes que já existem de forma compartilhada.

## `src/app` — Rotas

Contém **somente** a estrutura de rotas do Next.js (rotas/rota-groups/layouts).

- Não criar componentes reutilizáveis dentro de `app`.
- Não colocar lógica de domínio complexa em `page.tsx`.
- As páginas devem ser composição: receber parâmetros, chamar hooks, buscar dados
  e montar componentes.

Estrutura atual de route groups:

| Route group | Responsabilidade | Rotas |
|---|---|---|
| `(auth)` | Autenticação (público) | `/login`, `/register`, `/forgot-password`, `/reset-password`, `/link-account` |
| `(dashboard)` | Área autenticada (CRM + admin) | `/dashboard`, `/crm`, `/leads`, `/contacts`, `/pipeline`, `/tasks`, `/activities`, `/workflows`, `/notifications`, `/assistant`, `/inbox`, `/channels`, `/campaigns`, `/reports`, `/audit`, `/tenants`, `/members`, `/invitations`, `/profile`, `/users`, `/settings/users`, `/settings/roles`, `/storage` |
| `(authenticated)` | Permissões / Perfis (RBAC) | `/permissions`, `/roles` (+new/[id]/edit) |
| `(invitation)` | Aceite de convite | `/invitations/accept` |
| `(onboarding)` | Onboarding da empresa | `/onboarding` |
| `auth/callback` | Callback OAuth (compat) | `/auth/callback` |

### Exemplo — Dashboard

```
rota        → src/app/(dashboard)/dashboard/page.tsx
componentes → src/components/dashboard/
lógica      → src/features/dashboard/ (hooks/services/types)
```

## `src/features` — Domínio / Lógica

Cada feature concentra a lógica de um domínio. Subpastas típicas (criar **somente**
as que tiverem código real):

```
features/<name>/
├── hooks/
├── services/
├── schemas/
├── types/
└── components/   ← somente quando o componente for EXCLUSIVO da feature
```

- Criar uma feature **apenas** quando houver código de domínio suficiente.
- Não criar `features/tasks/hooks`, `features/tasks/services`, etc. se não existirem arquivos.
- Se o componente é reutilizado por vários módulos → `src/components/...`.
- Se é específico de uma feature → `src/features/<feature>/components/...`.

### Features existentes

| Feature | Responsabilidade |
|---|---|
| `auth` | Autenticação (login, registro, senha, provedores, autorização) |
| `dashboard` | Agregação do painel operacional (hook/service/types) |
| `crm` | Landing CRM (card de módulo) |
| `leads` | Gestão de leads |
| `contacts` | Gestão de contatos e visão 360 |
| `pipeline` | Negociações / pipeline de vendas |
| `tasks` | Tarefas |
| `activities` | Atividades / timeline |
| `workflows` | Automações / workflows |
| `notifications` | Notificações |
| `omnichannel` | Inbox / canais / chat |
| `campaigns` | Campanhas |
| `ai` | Assistente IA |
| `analytics` | Relatórios / analytics |
| `audit` | Auditoria |
| `tenants` | Empresas (tenants) |
| `members` | Membros |
| `invitations` | Convites |
| `users` | Usuários |
| `rbac` | Perfis/Permissões (roles) |
| `storage` | Arquivos |
| `onboarding` | Onboarding |
| `usage` | Uso da empresa |

A feature `auth` é a **única fonte de verdade** da lógica de autenticação
(hooks/services/schemas). Não duplicar lógica de auth em features de negócio.

## `src/components` — Componentes visuais

Organização:

```
components/
├── ui/         → primitivas do design system (shadcn/ui): Button, Card, Dialog, Input, Table, Badge, Select...
├── common/     → componentes reutilizáveis em várias áreas: PageTitle, SectionTitle, EmptyState,
│                 NoResults, ConfirmDialog, FilterBar, SearchInput, ErrorCard, LoadingState...
├── layout/     → infraestrutura visual autenticada/logada: DashboardLayout, Header, Sidebar,
│                 UserMenu, CompanySwitcher, ProfileAvatar, LoadingScreen, ProtectedRoute, AuthLayout
├── navigation/ → Breadcrumb etc.
├── dashboard/  → componentes específicos do Dashboard (ex.: AttentionList)
├── crm/        → componentes visuais compartilháveis por mais de um módulo do CRM
├── feedback/   → estados de erro / skeletons
└── brand/      → branding (ex.: LoginBrand)
```

### `components/ui`

Somente componentes base do design system / shadcn. **Nunca** colocar componentes
específicos de negócio aqui.

### `components/common`

Componentes reutilizáveis em várias áreas. **Antes de criar um**, procurar aqui,
em `components/ui` e em `components/crm`. Não duplicar.

### `components/dashboard`

Somente componentes específicos da página Dashboard.

### `components/crm`

Componentes visuais que podem ser usados por mais de um módulo do CRM
(filtros, status, cards, tabelas, pipeline, atividades...). Somente o que for
realmente compartilhável.

## Regras para criar componentes

1. procurar componentes semelhantes (`components/ui`, `components/common`, `components/crm`);
2. verificar se já existe um componente compartilhado;
3. verificar se ele pode ser generalizado;
4. reutilizar o componente existente.

Anti-exemplo (NÃO fazer):

```
components/common/EmptyState.tsx
components/leads/EmptyState.tsx      ❌
components/contacts/EmptyState.tsx   ❌
```

## Regras para criar features

Criar feature somente com código de domínio suficiente. Não criar subpastas vazias.
Pastas de domínio `components` devem existir somente com componentes exclusivos da feature.

## Alias

Usar sempre os aliases já configurados:

```
@/components/...
@/features/...
@/lib/...
```

Não criar outro sistema de aliases.

## Evitar duplicação / pastas vazias

- Remover pastas completamente vazias.
- Não criar pastas vazias para "preparar o futuro".
- Antes de mover/excluir um arquivo: verificar imports, exports, uso, rotas e testes.

## Observações da organização aplicada

- Famílias de diálogos de confirmação de exclusão (`DeleteLeadDialog`,
  `DeleteDealDialog`, `DeleteTenantDialog`, etc.) e filtros por feature
  (`LeadFilters`, `DealFilters`) ainda existem como componentes locais de cada
  feature. **Regra daqui em diante:** novos diálogos/filtros genéricos devem
  reutilizar `components/common/ConfirmDialog` / `components/common/FilterBar`
  (ou um componente compartilhado), em vez de duplicar o padrão.
