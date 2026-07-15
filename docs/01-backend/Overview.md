# Overview — Visão Geral do Backend

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Arquitetura](#arquitetura)
- [Camadas](#camadas)
- [Tecnologias](#tecnologias)
- [Fluxo Geral de Requisição](#fluxo-geral-de-requisição)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Fornecer uma visão geral do backend do CRM SaaS Omnichannel, incluindo arquitetura, camadas, tecnologias e fluxos principais.

## Descrição

O backend é uma aplicação Java 21 com Spring Boot 3, construída seguindo Clean Architecture e DDD (Domain-Driven Design). O sistema opera como um monolito modular com boundaries claros, preparado para decomposição em microsserviços quando necessário.

## Arquitetura

### Padrão Arquitetural

- **Clean Architecture** — Separação clara de camadas
- **Hexagonal Architecture** — Portas e adaptadores
- **DDD** — Bounded contexts para domínios
- **SOLID** — Princípios de design
- **Event-Driven** — Comunicação assíncrona via RabbitMQ

### Detalhes Técnicos

| Aspecto | Especificação |
|---|---|
| Linguagem | Java 21 LTS |
| Framework | Spring Boot 3.x |
| Build Tool | Maven |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Message Broker | RabbitMQ 3 |
| Migrations | Flyway |
| API Docs | OpenAPI 3.1 |
| Auth | JWT + Refresh Token |
| Container | Docker |

## Camadas

```
┌─────────────────────────────────────────────────┐
│              PRESENTATION LAYER                  │
│   REST Controllers → Request/Response DTOs       │
├─────────────────────────────────────────────────┤
│              APPLICATION LAYER                   │
│   Use Cases → Commands/Queries → Services        │
├─────────────────────────────────────────────────┤
│                DOMAIN LAYER                      │
│   Entities → Value Objects → Domain Events       │
│   Repository Interfaces (Ports)                  │
├─────────────────────────────────────────────────┤
│            INFRASTRUCTURE LAYER                  │
│   JPA Repositories → Cache → MQ → External APIs  │
└─────────────────────────────────────────────────┘
```

### Detalhes por Camada

| Camada | Responsabilidade | Dependências |
|---|---|---|
| **Domain** | Lógica de negócio, entidades, regras | Nenhuma |
| **Application** | Orquestração, casos de uso, DTOs | Domain |
| **Infrastructure** | Persistência, cache, messaging, APIs externas | Domain (via interfaces) |
| **Presentation** | HTTP endpoints, validação de entrada | Application |

## Tecnologias

| Componente | Tecnologia | Versão | Finalidade |
|---|---|---|---|
| Runtime | Java | 21 LTS | Execução da aplicação |
| Framework | Spring Boot | 3.x | Framework principal |
| Build | Maven | 3.9+ | Build e dependências |
| ORM | Hibernate/JPA | 6.x | Mapeamento objeto-relacional |
| Database | PostgreSQL | 16 | Database transacional |
| Cache | Redis | 7 | Cache e sessões |
| Queue | RabbitMQ | 3 | Messaging assíncrono |
| Migration | Flyway | 10+ | Versionamento de schema |
| Auth | Spring Security + JWT | - | Autenticação e autorização |
| Validation | Jakarta Validation | 3.0 | Validação de beans |
| Docs | SpringDoc OpenAPI | 2.x | Documentação da API |
| Testing | JUnit 5 + Mockito | - | Testes unitários |
| Container | Docker | 24+ | Containerização |

## Fluxo Geral de Requisição

```
1. Cliente envia HTTP Request
        │
2. Spring Security Filter (JWT Validation)
        │
3. Tenant Filter (Identifica company_id)
        │
4. Rest Controller (Recebe e valida Request DTO)
        │
5. Application Service (Orquestra caso de uso)
        │
6. Domain Service / Entity (Aplica regras de negócio)
        │
7. Repository Interface (Define operação de dados)
        │
8. Repository Implementation (Executa no database/cache)
        │
9. Application Service (Retorna resultado)
        │
10. Rest Controller (Serializa Response DTO)
        │
11. HTTP Response ao cliente
```

### Fluxo Assíncrono

```
1. Domain Event é criado
        │
2. Event Publisher publica no RabbitMQ
        │
3. Event Consumer recebe o evento
        │
4. Handler processa side effects
        │
5. Resultado: Atualização de cache, notificação, etc.
```

## Responsabilidades

- Servir como ponto de entrada para toda a documentação backend
- Definir a estrutura e organização do código
- Documentar os fluxos principais do sistema
- Referenciar documentos específicos de cada módulo

## Dependências

- [00-core/Architecture.md](../00-core/Architecture.md) — Arquitetura detalhada
- [00-core/TechStack.md](../00-core/TechStack.md) — Stack tecnológico
- [03-database/Overview.md](../03-database/Overview.md) — Modelagem de dados
- [04-integrations/WhatsApp.md](../04-integrations/WhatsApp.md) — Integração WhatsApp

## CORS

```java
// Configuração CORS (exemplo conceitual)
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",     // Desenvolvimento
            "https://app.crm.com"        // Produção
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600); // 1 hora
        return new UrlBasedCorsConfigurationSource();
    }
}
```

### Headers de Segurança HTTP

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

## Regras

- Toda feature nova deve ser documentada antes da implementação
- APIs devem ser versionadas desde o v1
- Handlers de exception devem tratar todas as exceções conhecidas
- Logging estruturado é obrigatório em todas as camadas
- HTTPS é obrigatório em produção
- CORS deve ser configurado com origens explícitas
- Headers de segurança devem ser aplicados em todas as respostas

## Futuras Melhorias

- Adicionar métricas de performance por endpoint
- Implementar distributed tracing com OpenTelemetry
- Adicionar circuit breaker para chamadas externas
- Considerar GraphQL para consultas complexas
- Implementar rate limiting por tenant

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial do overview backend |
