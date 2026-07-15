# Companies — Gestão de Empresas (Multi-Tenancy)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Multi-Tenancy](#multi-tenancy)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de gestão de empresas, incluindo criação, configurações, planos de assinatura e mecanismos de multi-tenancy.

## Descri Companies — Gestão de Empresas (Multi-Tenancy)

---

## Objetivo

Documentar o módulo de gestão de empresas (tenants), incluindo criação, configurações, billing e isolamento de dados.

## Descrição

Cada empresa é um tenant no sistema. A arquitetura multi-tenant garante isolamento de dados via PostgreSQL schemas separados. Uma empresa pode ter múltiplos usuários, cada um com roles e permissões específicas.

## Responsabilidades

- Criar e gerenciar empresas (tenants)
- Configurar schemas de database por tenant
- Gerenciar planos de assinatura e limits
- Configurar preferências da empresa
- Gerenciar convites e membros

## Fluxo

### Criação de Empresa

```
1. Usuário se cadastra (primeira empresa)
        │
2. Backend cria Company + schema PostgreSQL
        │
3. Usuário torna-se ADMIN da empresa
        │
4. Plano trial ativado (14 dias)
        │
5. Empresa configurada e pronta para uso
```

### Troca de Empresa

```
1. Usuário seleciona empresa no seletor
        │
2. Backend valida acesso do usuário à empresa
        │
3. JWT é atualizado com novo company_id
        │
4. Dados da nova empresa são carregados
```

## Multi-Tenancy

### Estratégia

| Aspecto | Implementação |
|---|---|
| Isolamento | PostgreSQL schemas separados |
| Identificação | JWT claim `company_id` |
| Row Security | RLS como camada adicional |
| Cache | Keys prefixadas com `tenant:{id}:` |
| File Storage | Paths prefixados com `tenant/{id}/` |

### Configurações por Empresa

| Config | Descrição | Default |
|---|---|---|
| `company.name` | Nome da empresa | — |
| `company.timezone` | Fuso horário | America/Sao_Paulo |
| `company.language` | Idioma | pt-BR |
| `company.max_users` | Limite de usuários | Definido pelo plano |
| `company.max_contacts` | Limite de contatos | Definido pelo plano |
| `company.whatsapp_enabled` | WhatsApp habilitado | false |
| `company.ai_enabled` | IA habilitada | false |

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/companies/current` | Dados da empresa atual | Autenticado |
| PUT | `/api/v1/companies/current` | Atualizar empresa | `company:write` |
| GET | `/api/v1/companies/current/settings` | Configurações | `company:read` |
| PUT | `/api/v1/companies/current/settings` | Atualizar configs | `company:write` |
| POST | `/api/v1/companies` | Criar nova empresa | Autenticado |
| GET | `/api/v1/companies/{id}` | Buscar empresa | `company:read` |
| GET | `/api/v1/companies/{id}/users` | Usuários da empresa | `company:read` |
| GET | `/api/v1/companies/{id}/subscription` | Plano atual | `company:read` |

## Dependências

- [Users.md](./Users.md) — Usuários da empresa
- [Auth.md](./Auth.md) — JWT com company_id
- [03-database/Overview.md](../03-database/Overview.md) — Schema per tenant

## Regras

- Todo dado é isolado por empresa (tenant)
- Usuário só acessa dados da empresa que pertence
- Empresa trial tem limites reduzidos
- Schema do database é criado automaticamente na criação da empresa
- Deletar empresa remove todos os dados (com confirmação)
- Empresa inativa bloqueia acesso de todos os usuários
- Máximo de 10 empresas por usuário (plano enterprise)

## Futuras Melhorias

- Billing e gestão de assinatura integrada
- Multi-tenancy hierárquico (empresa → departamentos)
- Migração de dados entre empresas
- White-label por empresa
- Limites customizáveis por empresa
- API de provisioning automático

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
