# Permissions — Regras de Permissões

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Matriz de Permissões](#matriz-de-permissões)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a matriz de permissões por role.

## Descrição

Permissões seguem modelo RBAC (Role-Based Access Control). Cada role tem um conjunto de permissões que define o que pode ver e fazer.

## Matriz de Permissões

### SUPER_ADMIN

| Módulo | Ver | Criar | Editar | Deletar | Gerenciar |
|---|---|---|---|---|---|
| Tudo | ✅ | ✅ | ✅ | ✅ | ✅ |

### ADMIN

| Módulo | Ver | Criar | Editar | Deletar | Gerenciar |
|---|---|---|---|---|---|
| Leads | ✅ | ✅ | ✅ | ✅ | ✅ |
| Contacts | ✅ | ✅ | ✅ | ✅ | ✅ |
| Pipeline | ✅ | ✅ | ✅ | ✅ | ✅ |
| Chat | ✅ | ✅ | ✅ | ✅ | ✅ |
| Campaigns | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reports | ✅ | ✅ | ✅ | ✅ | ✅ |
| Users | ✅ | ✅ | ✅ | ✅ | ✅ |
| Settings | ✅ | ✅ | ✅ | ✅ | ✅ |
| Audit | ✅ | — | — | — | ✅ |

### MANAGER

| Módulo | Ver | Criar | Editar | Deletar | Gerenciar |
|---|---|---|---|---|---|
| Leads | ✅ | ✅ | ✅ | — | — |
| Contacts | ✅ | ✅ | ✅ | — | — |
| Pipeline | ✅ | ✅ | ✅ | — | — |
| Chat | ✅ | ✅ | ✅ | — | — |
| Campaigns | ✅ | ✅ | ✅ | — | — |
| Reports | ✅ | — | — | — | — |
| Users | ✅ | — | — | — | — |

### AGENT

| Módulo | Ver | Criar | Editar | Deletar | Gerenciar |
|---|---|---|---|---|---|
| Leads (atribuídos) | ✅ | ✅ | ✅ | — | — |
| Contacts | ✅ | ✅ | ✅ | — | — |
| Pipeline (atribuídos) | ✅ | ✅ | ✅ | — | — |
| Chat | ✅ | ✅ | ✅ | — | — |
| Reports | ✅ | — | — | — | — |

### VIEWER

| Módulo | Ver | Criar | Editar | Deletar | Gerenciar |
|---|---|---|---|---|---|
| Tudo | ✅ | — | — | — | — |

## Responsabilidades

- Garantir que permissões são verificadas no backend
- Atualizar permissões quando roles mudam
- Auditar mudanças de permissão

## Dependências

- [01-backend/Auth.md](../01-backend/Auth.md) — Autenticação
- [01-backend/Users.md](../01-backend/Users.md) — Gestão de usuários
- [02-frontend/Permissions.md](../02-frontend/Permissions.md) — UI

## Futuras Melhorias

- Permissões customizáveis
- Permissões por módulo
- Audit de acessos
- Feature flags

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
