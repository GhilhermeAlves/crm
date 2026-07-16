# Frontend Context

## Resumo do Módulo
Next.js 14 App Router, React 18, TypeScript 5, Tailwind CSS 3, Shadcn UI. Estrutura feature-based. 5 providers, 22 rotas protegidas, 3 públicas. 28 componentes base.

## Objetivo
Fornecer interface moderna, responsiva e performática para todas funcionalidades do CRM.

## Responsabilidades
- SPA com Server Components (Next.js 14)
- UI consistente com Shadcn UI + Tailwind
- 5 providers transversais
- Feature-based structure
- Responsivo (mobile-first)

## Stack
| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Next.js | 14 | Framework React |
| React | 18 | UI library |
| TypeScript | 5 | Type safety |
| Tailwind CSS | 3 | Styling |
| Shadcn UI | - | Component library |

## Rotas (25 total)
| Tipo | Rotas |
|------|-------|
| **Públicas** (3) | `/login`, `/forgot-password`, `/reset-password` |
| **Protegidas** (22) | `/dashboard`, `/contacts`, `/leads`, `/pipeline`, `/conversations`, `/campaigns`, `/automations`, `/reports`, `/settings/*`, `/users`, etc. |

## Providers (5)
1. **AuthProvider** - Autenticação e JWT
2. **ThemeProvider** - Modo claro/escuro
3. **QueryProvider** - React Query (cache, server state)
4. **WebSocketProvider** - Conexão real-time
5. **NotificationProvider** - Notificações in-app

## 28 Componentes Base
- **Layout**: Header, Sidebar, Footer, PageContainer
- **Forms**: Input, Select, Checkbox, Radio, DatePicker, Textarea
- **Data**: Table, DataTable, Pagination, SortableHeader
- **Feedback**: Alert, Toast, Badge, Skeleton, Spinner
- **Overlay**: Dialog, Modal, Dropdown, Tooltip, Popover
- **Navigation**: Tabs, Breadcrumb, CommandPalette

## Estrutura
```
src/
├── app/              (App Router - rotas)
├── components/
│   ├── ui/           (28 componentes base)
│   ├── layout/       (Header, Sidebar)
│   └── shared/       (componentes reutilizáveis)
├── features/         (feature-based organization)
│   ├── auth/
│   ├── contacts/
│   ├── leads/
│   ├── pipeline/
│   └── ...
├── hooks/            (custom hooks)
├── lib/              (utilities, API client)
├── providers/        (5 providers)
└── types/            (TypeScript types)
```

## Eventos
- WebSocket events → atualizam UI em tempo real
- React Query mutations → invalidam cache
- Form submissions → validação client-side

## Fluxo Resumido
1. Usuário acessa rota → Next.js carrega Server Component
2. Client Component monta → providers inicializados → dados carregados
3. Interação → React Query mutation → invalida cache → re-render

## Checklist de Implementação
- [ ] Next.js 14 App Router configurado
- [ ] TypeScript strict mode
- [ ] Tailwind CSS + Shadcn UI
- [ ] 5 providers funcionando
- [ ] 22 rotas protegidas
- [ ] 3 rotas públicas
- [ ] 28 componentes base
- [ ] Mobile responsive

## Checklist de Testes
- [ ] Build sem erros de TypeScript
- [ ] Lint passa (ESLint)
- [ ] Rotas públicas acessíveis sem login
- [ ] Rotas protegidas redirecionam
- [ ] Componentes renderizam corretamente

## Documentação Oficial Relacionada
- `docs/frontend/SETUP.md`
- `docs/frontend/COMPONENTS.md`
- `docs/frontend/ROUTES.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
