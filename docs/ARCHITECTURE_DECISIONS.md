# ARCHITECTURE_DECISIONS — Consolidação das Decisões Arquiteturais (ADRs)

## Objetivo

Consolidar todas as decisões arquiteturais (Architecture Decision Records) em um único documento, com o motivo de cada decisão, alternativas consideradas e impactos. Documento valioso quando a equipe cresce ou quando, meses depois, alguém precisa entender por que determinada solução foi adotada.

## Índice

- [Como Usar Este Documento](#como-usar-este-documento)
- [ADR-001: Arquitetura de Referência](#adr-001-arquitetura-de-referência)
- [ADR-002: Multi-tenancy via Schema Isolation](#adr-002-multi-tenancy-via-schema-isolation)
- [ADR-003: Mensageria Assíncrona com RabbitMQ](#adr-003-mensageria-assíncrona-com-rabbitmq)
- [ADR-004: Migrations com Flyway](#adr-004-migrations-com-flyway)
- [ADR-005: Frontend com Next.js 14](#adr-005-frontend-com-nextjs-14)
- [ADR-006: Primary Keys com UUID v4](#adr-006-primary-keys-com-uuid-v4)
- [ADR-007: Autenticação JWT com Refresh Token Rotation](#adr-007-autenticação-jwt-com-refresh-token-rotation)
- [ADR-008: Cache com Redis](#adr-008-cache-com-redis)
- [ADR-009: Monolito Modular First](#adr-009-monolito-modular-first)
- [ADR-010: Containerização com Docker](#adr-010-containerização-com-docker)
- [ADR-011: Validação de Input com Bean Validation + Zod](#adr-011-validação-de-input-com-bean-validation---zod)
- [Resumo das Decisões](#resumo-das-decisões)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## Como Usar Este Documento

Cada ADR segue o formato:

```
## ADR-XXXX: Título

**Status:** Aceita | Proposta | Depreciada
**Data:** YYYY-MM-DD
**Decisor:** Nome/Cargo

### Contexto
O problema ou situação que levou à decisão.

### Decisão
O que foi decidido.

### Alternativas Consideradas
Opções avaliadas e por que foram rejeitadas.

### Impactos
Consequências positivas e negativas da decisão.

### Consequências
O que acontece se a decisão for revertida no futuro.
```

---

## ADR-001: Arquitetura de Referência

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

O sistema precisava de uma arquitetura que suportasse:
- Crescimento de 10+ anos
- Centenas de milhares de clientes
- Equipe crescente
- Manutenibilidade a longo prazo
- Separação clara de responsabilidades

### Decisão

Adotar **Clean Architecture + Hexagonal Architecture + Domain-Driven Design (DDD)**.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| MVC Clássico | Simples, rápido para começar | Acoplamento alto, difícil de escalar | Não suporta crescimento |
| Microservices desde o início | Escalabilidade independente | Complexidade operacional alta | Equipe pequena no início |
| Arquitetura em Camadas simples | Fácil de entender | Boundaries difusas | Não enforce separação |
| **Clean + Hexagonal + DDD** | **Boundaries claros, testável, escalável** | **Curva de aprendizado** | **Melhor para longo prazo** |

### Impactos

- **Positivo:** Testes unitários fáceis, mudança de infraestrutura sem impactar domínio, equipe pode trabalhar em paralelo
- **Negativo:** Mais boilerplate no início, curva de aprendizado para devs juniores

### Consequências

Se revertido, exigiria refatoração massiva de todos os módulos.

**Fonte:** [00-core/Architecture.md](./00-core/Architecture.md), [00-core/Decisions.md](./00-core/Decisions.md)

---

## ADR-002: Multi-tenancy via Schema Isolation

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

O sistema é SaaS multi-tenant. Precisávamos de isolamento de dados entre tenants com balance entre segurança e performance.

### Decisão

**Schema isolation no PostgreSQL** — cada tenant tem seu próprio schema.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| Database separado por tenant | Isolamento total | Alto custo operacional | Não escalável para muitos tenants |
| **Schema por tenant** | **Bom isolamento, custo moderado** | **Complexidade de migrations** | **Escolhido** |
| shared DB + tenant_id column | Simples | Risco de vazamento, queries lentas | Não atende requisitos de segurança |
| Citus (distributed) | Escalabilidade horizontal | Complexidade, custo | Prematuro para fase atual |

### Impactos

- **Positivo:** Isolamento de dados, backup/restore por tenant, performance adequada
- **Negativo:** Migrations precisam rodar em N schemas, connection pool cresce com tenants

### Consequências

Se o número de tenants superar 10.000, pode ser necessário migrar para Citus ou database separado.

**Fonte:** [03-database/Overview.md](./03-database/Overview.md), [00-core/Decisions.md](./00-core/Decisions.md)

---

## ADR-003: Mensageria Assíncrona com RabbitMQ

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

O sistema precisa processar eventos de forma assíncrona (notificações, cache invalidation, auditoria) sem bloquear o request do usuário.

### Decisão

Usar **RabbitMQ** como broker de mensagens para eventos assíncronos.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| **RabbitMQ** | **Maduro, reliable, Exchange types** | **Cluster complexo** | **Escolhido** |
| Apache Kafka | Alta throughput, event sourcing | Overhead para nosso volume | Prematuro |
| Amazon SQS/SNS | Managed, sem infra | Vendor lock-in, custo | Preferência por self-hosted |
| Redis Streams | Simples, já temos Redis | Não é um broker completo | Não atende necessidades |
| Sincrono (HTTP) | Simples | Acoplamento, latência | Não escala |

### Impactos

- **Positivo:** Desacoplamento, processamento em background, retry automático
- **Negativo:** Mais infraestrutura, debugging mais difícil, eventual consistency

### Consequências

Se precisar de event sourcing, pode ser necessário migrar para Kafka.

**Fonte:** [01-backend/Events.md](./01-backend/Events.md), [00-core/Decisions.md](./00-core/Decisions.md)

---

## ADR-004: Migrations com Flyway

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

Precisamos de versionamento de schema no PostgreSQL, suporte a multi-tenancy (migrations em N schemas), e integração com CI/CD.

### Decisão

Usar **Flyway** para versionamento de schema.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| **Flyway** | **Simples, Java native, migrate em N schemas** | **Sem rollback automático** | **Escolhido** |
| Liquibase | Rollback, XML/YAML | Mais complexo, overhead | Flyway mais simples |
| Manual (scripts SQL) | Controle total | Sem versionamento, erros humanos | Não recomendado |
| Hibernate auto DDL | Automático | Perde controle, não para produção | Perigoso |

### Impactos

- **Positivo:** Versionamento auditável, integração com Spring Boot, teste de migrations
- **Negativo:** Sem rollback automático (precisa escrever migration de correção)

### Consequências

Reverter uma migration exige criar uma nova migration de correção.

**Fonte:** [03-database/Migrations.md](./03-database/Migrations.md), [00-core/Decisions.md](./00-core/Decisions.md)

---

## ADR-005: Frontend com Next.js 14

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

O frontend precisa de performance, SEO (landing pages), rendering flexível (SSR/SSG/CSR), e ecossistema maduro.

### Decisão

Usar **Next.js 14 (App Router) + React 18 + TypeScript 5**.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| React SPA (Vite) | Simples, rápido | Sem SSR/SEO, sem rotas server | Não atende landing pages |
| **Next.js 14** | **SSR/SSG/CSR, App Router, ecossistema** | **Curva de aprendizado (RSC)** | **Escolhido** |
| Nuxt.js (Vue) | Simples, SSR | Ecossistema menor que React | Preferência por React |
| Angular | Enterprise-ready | Verboso, curva de aprendizado | Muito pesado |
| Remix | Web standards | Ecossistema menor | Next.js mais maduro |

### Impactos

- **Positivo:** Performance (SSR/SSG), SEO, Server Components, loading states nativos
- **Negativo:** Curva de aprendizado com App Router, Server Components vs Client Components

### Consequências

Migrar para outro framework exigiria reescrever toda a camada de frontend.

**Fonte:** [02-frontend/Overview.md](./02-frontend/Overview.md), [00-core/TechStack.md](./00-core/TechStack.md)

---

## ADR-006: Primary Keys com UUID v4

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

O sistema é multi-tenant com necessidade de:
- IDs únicos globalmente
- Não expor sequência (segurança)
- Merge de dados entre ambientes
- Client-side generation (opcional)

### Decisão

Usar **UUID v4** como primary key em todas as tabelas.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| SERIAL (auto-increment) | Simples, performance | Expõe sequência, colisão entre tenants | Não seguro |
| **UUID v4** | **Único global, seguro, client-gen** | **16 bytes, índice maior** | **Escolhido** |
| ULID | Ordenável, menor que UUID | Menos suporte nativo | UUID mais maduro |
| Nano ID | Curto, legível | Colisão potencial | UUID mais seguro |
| UUID v7 | Ordenável, time-based | Novo, menos suporte | Prematuro |

### Impactos

- **Positivo:** Segurança (não expõe sequência), merge fácil, distribuído
- **Negativo:** Ocupa mais espaço (16 bytes vs 4 bytes), índices maiores

### Consequências

Se performance de índice for crítica, considerar UUID v7 (ordenável por tempo).

**Fonte:** [03-database/UUID.md](./03-database/UUID.md), [00-core/Decisions.md](./00-core/Decisions.md)

---

## ADR-007: Autenticação JWT com Refresh Token Rotation

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

Precisávemos de autenticação stateless, segura, com suporte a multi-device e proteção contra token theft.

### Decisão

**JWT com Access Token (15min) + Refresh Token (7 dias) com rotation**.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| Sessões (server-side) | Simples, revogação fácil | Stateful, não escala | Não atende multi-device |
| **JWT + Refresh Rotation** | **Stateless, seguro, multi-device** | **Complexidade** | **Escolhido** |
| OAuth 2.0 (full) | Padrão, social login | Overhead para nosso caso | SSO no futuro |
| API Keys | Simples | Sem user context | Não para usuários |
| Magic Link | Sem senha | UX ruim, não para todos | Não para CRM |

### Impactos

- **Positivo:** Stateless, proteção contra token theft, multi-device
- **Negativo:** Tokens não são revogáveis imediatamente (blacklist necessária)

### Consequências

Se precisar de revogação imediata, usar curto TTL no access token + blacklist no Redis.

**Fonte:** [01-backend/Auth.md](./01-backend/Auth.md), [00-core/Decisions.md](./00-core/Decisions.md)

---

## ADR-008: Cache com Redis

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

O sistema precisa de cache para performance, rate limiting, sessões e distributed locks.

### Decisão

Usar **Redis 7** como cache layer.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| **Redis** | **Multi-purpose, maduro, performance** | **Memória** | **Escolhido** |
| Ehcache (in-memory) | Simples, sem infra | Não distribuído | Não atende multi-instance |
| Caffeine (in-memory) | Performance | Não distribuído | Mesmo problema |
| Memcached | Simples, performance | Sem data structures | Redis mais completo |
| Database cache | Sem infra extra | Lento, não para rate limiting | Não recomendado |

### Impactos

- **Positivo:** Performance, rate limiting, distributed locks, pub/sub
- **Negativo:** Mais infraestrutura, gestão de memória

### Consequências

Se Redis cair, o sistema funciona mas com performance degradada (fallback para DB).

**Fonte:** [01-backend/Cache.md](./01-backend/Cache.md), [00-core/TechStack.md](./00-core/TechStack.md)

---

## ADR-009: Monolito Modular First

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

Equipe pequena no início, mas com visão de crescimento. Precisávamos de algo que fosse simples agora e escalável no futuro.

### Decisão

**Monolito modular com boundaries claros**, pronto para se tornar microsserviços no futuro.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| **Monolito Modular** | **Simples, deploy único, pronto para microsserviços** | **Acoplamento potencial** | **Escolhido** |
| Microsserviços desde o início | Escalabilidade independente | Complexidade operacional alta | Equipe pequena |
| Serverless (Lambda) | Sem gestão de infra | Vendor lock-in, cold start | Não para CRM |
| Monolito tradicional | Simples | Difícil de escalar | Não para longo prazo |

### Impactos

- **Positivo:** Deploy simples, debugging fácil, refatoração gradual
- **Negativo:** Pode crescer demais se não mantiver boundaries

### Consequências

Se o monolito crescer demais, extrair módulos para microsserviços (já com boundaries definidos).

**Fonte:** [00-core/Architecture.md](./00-core/Architecture.md), [01-backend/Modules.md](./01-backend/Modules.md)

---

## ADR-010: Containerização com Docker

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

Precisamos de consistência entre ambientes (dev, staging, prod), facilitar onboarding de devs e preparar para orquestração.

### Decisão

Usar **Docker + Docker Compose** para containerização.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| **Docker** | **Padrão, consistência, ecossistema** | **Curva de aprendizado** | **Escolhido** |
| Podman | Rootless, sem daemon | Menos suporte | Docker mais maduro |
| Vagrant | VMs completas | Pesado, lento | Docker mais leve |
| Sem containers | Sem overhead | Inconsistência entre ambientes | Não recomendado |

### Impactos

- **Positivo:** Consistência, onboarding fácil, CI/CD simplificado
- **Negativo:** Overhead de memória, learning curve

### Consequências

Se precisar de orquestração, migrar para Kubernetes (já planejado na roadmap).

**Fonte:** [06-devops/Docker.md](./06-devops/Docker.md), [00-core/TechStack.md](./00-core/TechStack.md)

---

## ADR-011: Validação de Input com Bean Validation + Zod

**Status:** Aceita
**Data:** 2026-07-15
**Decisor:** Architect

### Contexto

Precisamos de validação robusta em backend (Java) e frontend (TypeScript) com reutilização de regras.

### Decisão

**Backend: Bean Validation (JSR 380) + Frontend: Zod**.

### Alternativas Consideradas

| Alternativa | Prós | Contras | Motivo da Rejeição |
|---|---|---|---|
| **Bean Validation + Zod** | **Padrão Java, type-safe TS, reutilizável** | **Duas stacks** | **Escolhido** |
| Apenas Bean Validation | Uma stack | Validação fraca no frontend | Não atende |
| Apenas Zod | Uma stack | Validação fraca no backend | Não atende |
| Hibernate Validator | Maduro | Não funciona no frontend | Parcial |
| Yup | Simples | Menos type-safe que Zod | Zod melhor |

### Impactos

- **Positivo:** Validação robusta em ambas as camadas, type safety
- **Negativo:** Manter regras sincronizadas entre backend e frontend

### Consequências

Se precisar de regras compartilhadas, usar JSON Schema (futuro).

**Fonte:** [02-frontend/Validation.md](./02-frontend/Validation.md), [00-core/CodingStandards.md](./00-core/CodingStandards.md)

---

## Resumo das Decisões

| # | Decisão | Escolha | Status |
|---|---|---|---|
| ADR-001 | Arquitetura | Clean + Hexagonal + DDD | ✅ Aceita |
| ADR-002 | Multi-tenancy | Schema Isolation | ✅ Aceita |
| ADR-003 | Mensageria | RabbitMQ | ✅ Aceita |
| ADR-004 | Migrations | Flyway | ✅ Aceita |
| ADR-005 | Frontend | Next.js 14 | ✅ Aceita |
| ADR-006 | Primary Keys | UUID v4 | ✅ Aceita |
| ADR-007 | Autenticação | JWT + Refresh Rotation | ✅ Aceita |
| ADR-008 | Cache | Redis 7 | ✅ Aceita |
| ADR-009 | Arquitetura Deploy | Monolito Modular | ✅ Aceita |
| ADR-010 | Containerização | Docker | ✅ Aceita |
| ADR-011 | Validação | Bean Validation + Zod | ✅ Aceita |

---

## Referências

| Documento | Caminho |
|---|---|
| Decisões originais | [00-core/Decisions.md](./00-core/Decisions.md) |
| TechStack | [00-core/TechStack.md](./00-core/TechStack.md) |
| Architecture | [00-core/Architecture.md](./00-core/Architecture.md) |
| Constitution | [00-core/Constitution.md](./00-core/Constitution.md) |
| SUMMARY | [SUMMARY.md](./SUMMARY.md) |

---

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial da consolidação de ADRs |
