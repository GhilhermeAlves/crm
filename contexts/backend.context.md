# Backend Context

## Resumo do Módulo
Arquitetura Clean + Hexagonal + DDD. 4 camadas: Presentation→Application→Domain→Infrastructure. 8 bounded contexts. Java 21, Spring Boot 3, Maven. Modular monolith first.

## Objetivo
Fornecer API RESTful robusta com arquitetura limpa e separação de responsabilidades.

## Responsabilidades
- Clean Architecture com 4 camadas
- DDD com 8 bounded contexts
- REST API com Spring Boot 3
- Modular monolith (monolito modular)
- Java 21 com virtual threads

## Camadas (Clean Architecture)
```
Presentation → Application → Domain → Infrastructure
 Controllers    Services      Entities   Repositories
 DTOs           Use Cases     Events     External APIs
```

## 8 Bounded Contexts
1. **Auth** - Autenticação e autorização
2. **Tenant** - Multi-tenancy e empresas
3. **Contact** - Gestão de contatos
4. **Lead** - Captura e qualificação
5. **Pipeline** - Vendas e oportunidades
6. **Conversation** - Comunicação
7. **Campaign** - Marketing em massa
8. **Automation** - Workflows automáticos

## Stack
| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.x | Framework |
| Maven | 3.9+ | Build tool |
| Flyway | 10+ | Migrations |
| HikariCP | - | Connection pool |

## Componentes Backend
```
crm-backend/
├── src/main/java/com/crm/
│   ├── modules/
│   │   ├── auth/        (Presentation, Application, Domain, Infrastructure)
│   │   ├── tenant/
│   │   ├── contact/
│   │   ├── lead/
│   │   ├── pipeline/
│   │   ├── conversation/
│   │   ├── campaign/
│   │   └── automation/
│   ├── shared/          (cross-cutting concerns)
│   └── CrmApplication.java
```

## Eventos
- Domain events publicados via RabbitMQ
- Integration events para comunicação entre contextos
- Idempotência em todos handlers

## Fluxo Resumido
1. Request HTTP → Controller (Presentation) → valida DTO
2. Service (Application) → executa use case → chama Domain
3. Domain → entidades, regras, events → Infrastructure persiste

## Checklist de Implementação
- [ ] Java 21 com virtual threads
- [ ] Spring Boot 3 configurado
- [ ] 8 bounded contexts organizados
- [ ] Clean Architecture em cada contexto
- [ ] Maven multi-module (se aplicável)
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
