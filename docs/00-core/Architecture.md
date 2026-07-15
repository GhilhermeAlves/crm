# Architecture — Arquitetura do Sistema

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Visão Geral](#visão-geral)
- [Clean Architecture](#clean-architecture)
- [DDD — Domain-Driven Design](#ddd--domain-driven-design)
- [Hexagonal Architecture](#hexagonal-architecture)
- [Event-Driven Architecture](#event-driven-architecture)
- [Multi-Tenancy](#multi-tenancy)
- [Microsserviços](#microsserviços)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Definir a arquitetura de software do CRM SaaS Omnichannel, estabelecendo camadas, limites de domínio, padrões de comunicação e princípios de design.

## Descrição

O sistema é construído sobre uma arquitetura hexagonal inspirada em Clean Architecture, com domínios modelados segundo DDD (Domain-Driven Design). A arquitetura é modular e microservices-ready, permitindo operar como monolito modular no início e ser decomposto em microsserviços quando necessário.

## Visão Geral

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│              (Next.js Frontend + API Gateway)            │
├─────────────────────────────────────────────────────────┤
│                    APPLICATION LAYER                     │
│         (Use Cases / Application Services)               │
├─────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER                       │
│        (Entities / Value Objects / Domain Events)        │
├─────────────────────────────────────────────────────────┤
│                 INFRASTRUCTURE LAYER                     │
│  (Database / Cache / Message Queue / External APIs)      │
└─────────────────────────────────────────────────────────┘
```

## Clean Architecture

O sistema segue os princípios da Clean Architecture de Robert C. Martin:

### Camadas

| Camada | Descrição | Dependências |
|---|---|---|
| **Domain** | Entidades, objetos de valor, interfaces de repositório | Nenhuma (core puro) |
| **Application** | Casos de uso, orquestração, DTOs, mappers | Domain |
| **Infrastructure** | Implementações de repositórios, clientes HTTP, cache | Domain (via interfaces) |
| **Presentation** | Controllers, endpoints, validação de entrada | Application |

### Regra de Dependência

> As dependências sempre apontam de fora para dentro. A camada de Domain nunca depende de nenhuma outra camada.

### Portas e Adaptadores

```
         ┌──────────────┐
         │   Domain      │
         │   (Core)      │
         │               │
         │  ┌─────────┐  │
         │  │  Ports   │  │  ← Interfaces definidas pelo domínio
         │  └─────────┘  │
         └───────┬───────┘
                 │
    ┌────────────┼────────────┐
    │            │            │
┌───▼───┐  ┌────▼────┐  ┌───▼────┐
│Primary│  │Secondary│  │Driving │
│Adapter│  │Adapter  │  │Adapter │
│(API)  │  │(DB/Cache)│  │(CLI)  │
└───────┘  └─────────┘  └────────┘
```

## DDD — Domain-Driven Design

### Bounded Contexts

| Contexto | Descrição | Responsabilidade Principal |
|---|---|---|
| **Identity** | Autenticação e autorização | Usuários, permissões, tokens |
| **Company** | Gestão de empresas | Multi-tenancy, configurações |
| **Contact** | Gestão de contatos | Leads, clientes, segmentação |
| **Pipeline** | Gestão de vendas | Oportunidades, pipeline, kanban |
| **Communication** | Comunicação multicanal | Chat, mensagens, templates |
| **Campaign** | Campanhas e automações | Disparo em massa, workflows |
| **Analytics** | Relatórios e dashboards | Métricas, KPIs, relatórios |
| **Integration** | Integrações externas | WhatsApp, Email, APIs |

### Strategic Design

- **Ubiquitous Language**: Cada bounded context mantém seu próprio vocabulário documentado
- **Context Map**: Define relacionamentos entre contextos (upstream/downstream, conformist, etc.)
- **Aggregates**: Cada entidade raiz de aggregate é consistente transacionalmente

## Hexagonal Architecture

### Portas (Interfaces)

```java
// Porta primária (input) - Exemplo conceitual
interface LeadUseCase {
    Lead createLead(CreateLeadCommand command);
    Lead qualifyLead(UUID leadId);
}

// Porta secundária (output) - Exemplo conceitUAL
interface LeadRepository {
    Lead findById(UUID id);
    List<Lead> findByCompany(UUID companyId);
    Lead save(Lead lead);
}
```

### Adaptadores

- **Driving (Primários)**: REST Controllers, GraphQL Resolvers, CLI commands
- **Driven (Secundários)**: Database repositories, Cache managers, API clients, Message publishers

## Event-Driven Architecture

### Tipos de Eventos

| Tipo | Descrição | Exemplo |
|---|---|---|
| **Domain Event** | Ocorreu algo significativo no domínio | `LeadCreated`, `MessageReceived` |
| **Integration Event** | Precisa ser comunicado a outros contextos | `ContactUpdated`, `CampaignSent` |
| **Command** | Solicitação de ação | `SendWhatsAppMessage`, `UpdateLeadStatus` |

### Fluxo de Eventos

```
Domain Event → Event Publisher → RabbitMQ → Event Consumers → Side Effects
     │                                              │
     │                                              ├── Atualizar cache
     │                                              ├── Enviar notificação
     │                                              └── Atualizar read model
     │
     └── Event Store (audit log de todos os eventos)
```

## Multi-Tenancy

### Estratégia: Shared Database, Separate Schema

| Opção | Prós | Contras |
|---|---|---|
| Separate Database | Isolamento total | Custo alto, hard de manter |
| Separate Schema | Bom isolamento, custo moderado | Complexidade de migration |
| Shared Schema | Custo baixo | Risco de vazamento de dados |

**Decisão**: Separate Schema — equilíbrio entre isolamento e custo.

### Row-Level Security

```sql
-- Exemplo conceitUAL de RLS no PostgreSQL
CREATE POLICY tenant_isolation ON leads
    USING (company_id = current_setting('app.current_company_id')::uuid);
```

## Microsserviços

### Princípio: Modular Monolith First

O sistema começa como um monolito modular com boundaries claros que permitem decomposição futura.

### Critérios para Decomposição

- Quando um módulo precisa de escala independente
- Quando equipes diferentes precisam deployar independentemente
- Quando um módulo tem requisitos de tecnologia diferentes

### Comunicação entre Serviços

| Tipo | Quando Usar | Tecnologia |
|---|---|---|
| Síncrona | Queries que precisam de resposta imediata | REST / gRPC |
| Assíncrona | Commands e events que não precisam de resposta | RabbitMQ |

## Responsabilidades

- Definir e manter os limites de domínio
- Documentar decisões arquiteturais
- Revisar mudanças que afetam a arquitetura
- Garantir consistência entre bounded contexts

## Dependências

- [Constitution.md](./Constitution.md) — Princípios que governam a arquitetura
- [TechStack.md](./TechStack.md) — Tecnologias que compõem a arquitetura
- [DesignPatterns.md](./DesignPatterns.md) — Padrões utilizados na implementação
- [01-backend/Overview.md](../01-backend/Overview.md) — Implementação backend
- [03-database/Overview.md](../03-database/Overview.md) — Modelagem de dados

## Regras

- Toda mudança arquitetural deve ser registrada em [Decisions.md](./Decisions.md)
- Novos bounded contexts devem ser aprovados pelo Arquiteto Principal
- A comunicação entre contextos deve ser sempre via interfaces (portas)
- Não é permitido acesso direto ao database de outro contexto
- Eventos devem ser idempotentes

## Futuras Melhorias

- Event Sourcing para contextos críticos (Audit, Communication)
- CQRS para separar reads e writes no contexto de Analytics
- Service Mesh (Istio) quando houver decomposição em microsserviços
- API Gateway centralizado para autenticação e rate limiting
- Distributed tracing com OpenTelemetry

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial da arquitetura |
