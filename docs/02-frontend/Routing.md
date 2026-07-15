# Routing — Rotas e Navegação

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Mapa de Rotas](#mapa-de-rotas)
- [Layouts](#layouts)
- [Autenticação](#autenticação)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todas as rotas da aplicação, incluindo layouts, proteção e navegação.

## Descrição

O Next.js App Router organiza rotas em pastas. Layouts são aninhados e compartilhados entre rotas. Rotas protegidas verificam autenticação.

## Mapa de Rotas

### Rotas Públicas

| Rota | Página | Descrição |
|---|---|---|
| `/login` | LoginPage | Tela de login |
| `/forgot-password` | ForgotPasswordPage | Esqueci senha |
| `/reset-password` | ResetPasswordPage | Resetar senha |

### Rotas Protegidas

| Rota | Página | Descrição |
|---|---|---|
| `/dashboard` | DashboardPage | Dashboard principal |
| `/leads` | LeadsPage | Lista de leads |
| `/leads/[id]` | LeadDetailPage | Detalhes do lead |
| `/customers` | CustomersPage | Lista de clientes |
| `/customers/[id]` | CustomerDetailPage | Detalhes do cliente |
| `/chat` | ChatPage | Chat principal |
| `/chat/[id]` | ConversationPage | Conversa específica |
| `/pipeline` | PipelinePage | Pipeline kanban |
| `/pipeline/[id]` | OpportunityPage | Detalhes da oportunidade |
| `/campaigns` | CampaignsPage | Lista de campanhas |
| `/campaigns/[id]` | CampaignDetailPage | Detalhes da campanha |
| `/reports` | ReportsPage | Relatórios |
| `/settings` | SettingsPage | Configurações |
| `/settings/profile` | ProfilePage | Meu profile |
| `/settings/company` | CompanyPage | Config da empresa |
| `/settings/users` | UsersPage | Gerenciar usuários |
| `/settings/integrations` | IntegrationsPage | Integrações |

## Layouts

```
app/
├── layout.tsx              # Root layout ( ThemeProvider, QueryProvider)
├── (auth)/
│   ├── layout.tsx          # Auth layout (sidebar, header)
│   └── dashboard/
│       └── page.tsx
├── (public)/
│   ├── layout.tsx          # Public layout (minimal)
│   └── login/
│       └── page.tsx
```

## Autenticação

```
1. Usuário acessa rota protegida
        │
2. Middleware verifica JWT no cookie
        │
3. Se autenticado → Acesso permitido
   Se não → Redirect para /login
4. Se tem permissão → Acesso à feature
   Se não → 403 Forbidden
```

## Responsabilidades

- Organizar rotas de forma lógica e intuitiva
- Proteger rotas que exigem autenticação
- Redirecionar usuários não autenticados
- Suportar navegação por breadcrumbs
- Manter SEO com metadata

## Dependências

- [Layout.md](./Layout.md) — Layouts de navegação
- [Context.md](./Context.md) — AuthProvider
- [Permissions.md](./Permissions.md) — Controle de acesso

## Regras

- Toda rota deve ter um layout
- Rotas protegidas verificam autenticação no middleware
- Redirects devem preservar a URL original (returnTo)
- Metadata deve ser definido para cada página
- Rotas devem ser estáticas quando possível (SSG)

## Futuras Melhorias

- Lazy loading por rota
- Prefetch de rotas prováveis
- Rotas com permissões dinâmicas
- Breadcrumbs automáticos
- Animações de transição de página

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
