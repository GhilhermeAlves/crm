# Overview — Visão Geral do Frontend

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Stack](#stack)
- [Arquitetura](#arquitetura)
- [Estrutura de Pastas](#estrutura-de-pastas)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Fornecer uma visão geral do frontend do CRM SaaS Omnichannel, incluindo stack, arquitetura e convenções.

## Descrição

O frontend é uma aplicação SPA/SSR construída com Next.js 14 e App Router. Utiliza React Server Components para performance, Shadcn UI para componentes base, e Tailwind CSS para estilização.

## Stack

| Tecnologia | Versão | Finalidade |
|---|---|---|
| Next.js | 14.x | Framework React com SSR/SSG |
| React | 18.x | Biblioteca UI |
| TypeScript | 5.x | Tipagem estática |
| Tailwind CSS | 3.x | CSS utility-first |
| Shadcn UI | latest | Component library |
| React Query (TanStack) | 5.x | Server state management |
| React Hook Form | 7.x | Formulários |
| Zod | 3.x | Validação de schemas |
| Axios | 1.x | HTTP client |
| date-fns | 3.x | Manipulação de datas |
| Recharts | 2.x | Gráficos |
| cmdk | latest | Command palette |

## Arquitetura

### Padrão

- **App Router** — Server Components + Client Components
- **Feature-Based** — Organização por módulo de negócio
- **Container/Presentation** — Separação de lógica e UI
- **HOC/Hooks** — Lógica compartilhada via hooks

### Server vs Client Components

```
Server Components (Default)
├── Dados são fetchados no servidor
├── Zero JavaScript enviado ao cliente
├── SEO-friendly
└── Não suportam hooks de interatividade

Client Components ("use client")
├── Dados são fetchados no cliente
├── JavaScript é enviado ao cliente
├── Suportam hooks, eventos, estado
└── Usar apenas quando necessário
```

## Estrutura de Pastas

```
frontend/src/
├── app/                    # Next.js App Router
├── components/             # Componentes React
│   ├── ui/                 # Shadcn base components
│   ├── layout/             # Layout components
│   ├── {module}/           # Feature components
│   └── shared/             # Shared components
├── hooks/                  # Custom hooks
├── lib/                    # Utilities
├── providers/              # Context providers
├── types/                  # TypeScript types
└── styles/                 # Global styles
```

## Responsabilidades

- Interface do usuário responsiva e acessível
- Comunicação com o backend via API
- Gerenciamento de estado (server e client)
- Validação de dados no cliente
- Performance e otimização
- Experiência do usuário (UX)

## Dependências

- [00-core/TechStack.md](../00-core/TechStack.md) — Stack tecnológico
- [01-backend/Overview.md](../01-backend/Overview.md) — API endpoints
- [00-core/CodingStandards.md](../00-core/CodingStandards.md) — Padrões de codificação

## Regras

- Components devem ser small e focused (< 200 lines)
- Hooks devem ter uma única responsabilidade
- Usar TypeScript strict mode
- Zero `any` types
- ESLint + Prettier para formatação
- 100% de cobertura em componentes críticos

## Futuras Melhorias

- Micro-frontends para módulos independentes
- PWA para uso offline
- Mobile app com React Native (mesmos componentes)
- Storybook para documentação de componentes
- Visual regression testing

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
