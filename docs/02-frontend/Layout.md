# Layout — Layout e Estrutura Visual

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Layout Principal](#layout-principal)
- [Sidebar](#sidebar)
- [Header](#header)
- [Responsividade](#responsividade)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a estrutura de layout do CRM, incluindo sidebar, header, conteúdo principal e comportamento responsivo.

## Descrição

O layout segue o padrão "sidebar + header + content" com sidebar colapsável. O layout é responsivo e se adapta de desktop (sidebar fixa) para mobile (drawer).

## Layout Principal

```
┌──────────────────────────────────────────────┐
│                  Header                       │
│  Logo   Search   Notifications   User Menu   │
├────────┬─────────────────────────────────────┤
│        │                                     │
│  Side  │         Content Area                │
│  bar   │                                     │
│        │                                     │
│  • Dash │                                    │
│  • Lead │                                    │
│  • Chat │                                    │
│  • Pipe │                                    │
│        │                                     │
│        │                                     │
├────────┴─────────────────────────────────────┤
│                  Footer (optional)            │
└──────────────────────────────────────────────┘
```

## Sidebar

### Estados

| Estado | Largura | Descrição |
|---|---|---|
| Expandida | 240px | Menu completo com labels |
| Colapsada | 64px | Apenas ícones |
| Mobile | 100% | Drawer overlay |

### Itens do Menu

| Ícone | Rota | Label |
|---|---|---|
| LayoutDashboard | /dashboard | Dashboard |
| Users | /leads | Leads |
| UserCheck | /customers | Clientes |
| MessageSquare | /chat | Chat |
| Kanban | /pipeline | Pipeline |
| Megaphone | /campaigns | Campanhas |
| BarChart3 | /reports | Relatórios |
| Settings | /settings | Configurações |

## Header

### Componentes

| Posição | Componente | Função |
|---|---|---|
| Esquerda | Logo | Marca + nome do CRM |
| Centro | SearchInput | Busca global (Cmd+K) |
| Direita | NotificationBell | sino de notificações |
| Direita | ThemeToggle | Dark/Light mode |
| Direita | UserMenu | Avatar + menu do usuário |

## Responsividade

| Breakpoint | Largura | Comportamento |
|---|---|---|
| Desktop | > 1024px | Sidebar fixa, layout completo |
| Tablet | 768-1024px | Sidebar colapsada |
| Mobile | < 768px | Sidebar como drawer |

## Responsabilidades

- Fornecer navegação consistente
- Adaptar-se a diferentes tamanhos de tela
- Manter acessibilidade (WCAG 2.1 AA)
- Suportar dark/light mode
- Performance (sidebar não re-renderiza o conteúdo)

## Dependências

- [Theme.md](./Theme.md) — Design tokens
- [Components.md](./Components.md) — Componentes base
- [Permissions.md](./Permissions.md) — Itens visíveis por permissão
- [01-backend/Auth.md](../01-backend/Auth.md) — Dados do usuário

## Regras

- Sidebar deve ser colapsável
- Menu items devem respeitar permissões do usuário
- Header deve ser sticky (fixo no scroll)
- Active route deve ser destacada
- Keyboard navigation é obrigatória
- Sidebar state é persistido no localStorage

## Futuras Melhorias

- Breadcrumbs automáticos
- Command palette (Cmd+K) para navegação rápida
- Sidebar com seções colapsáveis
- Personalização do menu por usuário
- Modo fullscreen para chat

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
