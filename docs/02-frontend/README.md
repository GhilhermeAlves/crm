# 02-Frontend — Documentação do Frontend

## Índice

| Documento | Descrição |
|---|---|
| [Overview.md](./Overview.md) | Visão geral do frontend |
| [Layout.md](./Layout.md) | Layout e estrutura visual |
| [Theme.md](./Theme.md) | Tema e design tokens |
| [Components.md](./Components.md) | Biblioteca de componentes |
| [Hooks.md](./Hooks.md) | Custom React hooks |
| [Context.md](./Context.md) | React Context providers |
| [Routing.md](./Routing.md) | Rotas e navegação |
| [Forms.md](./Forms.md) | Formulários e inputs |
| [Validation.md](./Validation.md) | Validação de dados |
| [Dashboard.md](./Dashboard.md) | Página do dashboard |
| [Leads.md](./Leads.md) | Módulo de leads |
| [Customers.md](./Customers.md) | Módulo de clientes |
| [Chat.md](./Chat.md) | Módulo de chat |
| [Kanban.md](./Kanban.md) | Quadro kanban |
| [Calendar.md](./Calendar.md) | Calendário |
| [Reports.md](./Reports.md) | Módulo de relatórios |
| [Settings.md](./Settings.md) | Configurações |
| [Tables.md](./Tables.md) | Componentes de tabela |
| [Charts.md](./Charts.md) | Gráficos |
| [Upload.md](./Upload.md) | Upload de arquivos |
| [Notifications.md](./Notifications.md) | Notificações UI |
| [Permissions.md](./Permissions.md) | Controle de permissões UI |

---

## Objetivo

Documentar todos os componentes, páginas, hooks e funcionalidades do frontend do CRM SaaS Omnichannel.

## Descrição

O frontend é construído com Next.js 14 (App Router), React 18, TypeScript e Tailwind CSS. Componentes base são do Shadcn UI.

## Regras

- Seguir padrões React Server Components quando possível
- Componentes devem ser client components apenas quando necessário (interatividade)
- Usar `zod` para todas as validações
- Manter componentes small e focused

## Dependências

- [00-core/TechStack.md](../00-core/TechStack.md) — Stack tecnológico
- [00-core/CodingStandards.md](../00-core/CodingStandards.md) — Padrões de codificação
- [01-backend/Overview.md](../01-backend/Overview.md) — API endpoints

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial da documentação frontend |
