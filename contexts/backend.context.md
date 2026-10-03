# Backend Context

## Resumo do Módulo
Modular monolith com Java 25, Spring Boot 3 e Maven. Cada bounded context agrupa seus módulos com 4 camadas internas (Presentation, Application, Domain, Infrastructure). Os contexts espelham a organização de features do frontend.

## Objetivo
Fornecer API RESTful robusta com arquitetura limpa, separação por bounded context e alinhamento direto com o frontend.

## Responsabilidades
- Clean Architecture com 4 camadas por módulo
- DDD com 6 bounded contexts
- REST API com Spring Boot 3
- Modular monolith (monolito modular)
- Java 25 com virtual threads

## Stack
| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 25 | Linguagem principal (virtual threads) |
| Spring Boot | 3.x | Framework |
| Maven | 3.9+ | Build tool |
| Flyway | 10+ | Migrations |
| HikariCP | - | Connection pool |

## Camadas por módulo
```
Web → Application → Domain → Infrastructure
 Controller   Service       Entity    JPA Repository
 REST DTOs    UseCase       VO        External APIs
              Ports         Event     Adapters
                            Exception
```

Cada módulo dentro de um bounded context contém suas próprias 4 camadas:
```
sales/lead/
├── web/               LeadController.java
├── application/       LeadService.java, DTOs, ports (in/, out/)
├── domain/            Lead.java, LeadStatus.java, exceptions
└── infrastructure/    LeadJpaEntity.java, LeadJpaRepository.java, persistence/
```

## 6 Bounded Contexts

### 1. Identity
- **auth** — Autenticação, JWT, login
- **invitation** — Convites de acesso
- **membership** — Membros e permissões
- **onboarding** — Fluxo de primeira configuração
- **me** — Perfil do usuário logado

### 2. Masterdata
- **contact** — Gestão de contatos
- **company** — Empresas
- **catalog** — Catálogo de produtos/serviços
- **storage** — Armazenamento de arquivos

### 3. Sales
- **lead** — Captura e qualificação
- **pipeline** — Vendas e oportunidades (kanban)
- **activity** — Atividades comerciais
- **task** — Tarefas
- **followup** — Sequências de follow-up
- **agenda** — Agenda de compromissos ← NEW

### 4. Communication
- **omnichannel** — WhatsApp, e-mail, chat
- **notification** — Notificações in-app e push
- **template** — Templates de mensagens

### 5. Automation
- **campaign** — Campanhas de marketing
- **workflow** — Automações e regras
- **ai** — Assistente de IA

### 6. Analytics
- **audit** — Auditoria e logs
- **dashboard** — Dashboard operacional
- **reporting** — Relatórios
- **customer360** — Visão consolidada do cliente

## Estrutura de diretórios
```
backend/src/main/java/com/becommerce/crm/
├── identity/
│   ├── auth/                  web/ application/ domain/ infrastructure/
│   ├── invitation/            web/ application/ domain/ infrastructure/
│   ├── membership/            web/ application/ domain/ infrastructure/
│   ├── onboarding/            web/ application/ domain/ infrastructure/
│   └── me/                    web/ application/ domain/ infrastructure/
│
├── masterdata/
│   ├── contact/               web/ application/ domain/ infrastructure/
│   ├── company/               web/ application/ domain/ infrastructure/
│   ├── catalog/               web/ application/ domain/ infrastructure/
│   └── storage/               web/ application/ domain/ infrastructure/
│
├── sales/
│   ├── lead/                  web/ application/ domain/ infrastructure/
│   ├── pipeline/              web/ application/ domain/ infrastructure/
│   ├── activity/              web/ application/ domain/ infrastructure/
│   ├── task/                  web/ application/ domain/ infrastructure/
│   ├── followup/              web/ application/ domain/ infrastructure/
│   └── agenda/                web/ application/ domain/ infrastructure/ ← NEW
│
├── communication/
│   ├── omnichannel/           web/ application/ domain/ infrastructure/
│   ├── notification/          web/ application/ domain/ infrastructure/
│   └── template/              web/ application/ domain/ infrastructure/
│
├── automation/
│   ├── campaign/              web/ application/ domain/ infrastructure/
│   ├── workflow/              web/ application/ domain/ infrastructure/
│   └── ai/                    web/ application/ domain/ infrastructure/
│
├── analytics/
│   ├── audit/                 web/ application/ domain/ infrastructure/
│   ├── dashboard/             web/ application/ domain/ infrastructure/
│   ├── reporting/             web/ application/ domain/ infrastructure/
│   └── customer360/           web/ application/ domain/ infrastructure/
│
├── shared/
│   ├── config/                SchedulerConfig, WebConfig
│   ├── security/              SecurityConfig, JwtFilter, Authorization
│   ├── tenant/                TenantContext, TenantFilter, DataSource routing
│   ├── messaging/             RabbitMQ config, event bus
│   ├── websocket/             WebSocket config, security
│   └── otp/                   OTP rate limiting
│
└── CrmApplication.java
```

## Alinhamento Backend ↔ Frontend
| Backend (modules/) | Frontend (features/) | Bounded Context |
|--------------------|---------------------|-----------------|
| `identity/` | `identity/` | Autenticação, usuários, tenants, RBAC |
| `masterdata/` | `masterdata/` | Contatos, empresas, catálogo, arquivos |
| `sales/` | `sales/` | Leads, pipeline, atividades, tarefas, follow-ups, agenda |
| `communication/` | `communication/` | Omnichannel, notificações, templates |
| `automation/` | `automation/` | Campanhas, workflows, IA |
| `analytics/` | `analytics/` | Auditoria, dashboard, relatórios, 360° |

## Eventos
- Domain events publicados via RabbitMQ
- Integration events para comunicação entre contexts
- Idempotência em todos os handlers

## Fluxo de request
1. Request HTTP → **Controller** (Presentation) → valida DTO de entrada
2. **Service** (Application) → executa use case → orquestra Domain
3. **Domain** → entidades, regras de negócio, domain events
4. **Infrastructure** → persiste via JPA, dispara events via RabbitMQ

## Checklist de Implementação
- [ ] Java 25 com virtual threads
- [ ] Spring Boot 3 configurado
- [ ] 6 bounded contexts organizados em `modules/`
- [ ] Clean Architecture em cada módulo (4 camadas)
- [ ] Shared para cross-cutting concerns
- [ ] Exception handler global
- [ ] Swagger/OpenAPI documentado
- [ ] Health checks configurados

## Checklist de Testes
- [ ] Unit tests em Domain layer
- [ ] Integration tests em Application layer
- [ ] API tests em Presentation layer
- [ ] Test containers para DB
- [ ] CI/CD pipeline funcionando

## Documentação Oficial Relacionada
- `docs/backend/ARCHITECTURE.md`
- `docs/backend/BOUNDED-CONTEXTS.md`
- `docs/backend/DEPLOYMENT.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
| 2026-10-02 | GhilhermeAlves | Reestruturação por bounded context (modules/), alinhamento com frontend, adição de agenda em sales |
