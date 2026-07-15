# Tratamento de Erros

## Objetivo

Definir a estratégia centralizada de tratamento de erros do CRM SaaS Omnichannel, garantindo respostas consistentes, informativas e seguras em todos os cenários de falha. A estratégia deve cobrir erros de validação, regras de negócio, infraestrutura e serviços externos, com mecanismos de retry, circuit breaker e fallback para resiliência.

## Escopo

- Categorias de erro: Validação, Regra de Negócio, Infraestrutura, Externo
- Formato padronizado de resposta de erro (JSON)
- Mapeamento de códigos HTTP para categorias de erro
- Registro centralizado de códigos de erro (error codes)
- Políticas de retry com backoff exponencial
- Circuit breaker com Resilience4j
- Fallback e degradação graciosa
- Tratamento de erros globais via `@ControllerAdvice` (Spring Boot)
- Tratamento de erros no frontend (Next.js 14)

## Responsabilidades

| Área | Responsabilidade |
|---|---|
| Backend (Spring Boot) | Implementar exception handlers, categorizar erros, registrar códigos |
| Frontend (Next.js) | Tratar erros HTTP, exibir mensagens amigáveis, retry de requisições |
| Arquitetura | Definir catálogo de códigos de erro, políticas de resiliência |
| SRE / Infraestrutura | Monitorar taxas de erro, configurar alertas |
| QA | Testar cenários de erro, validar formatos de resposta |

## Fluxos

### Fluxo de Tratamento Global de Erros

```mermaid
flowchart TD
    A[Requisição HTTP] --> B{Controller processa}
    B -->|Sucesso| C[200 OK + dados]
    B -->|Exceção| D[Exception Handler Global]
    D --> E{Categoria da exceção}
    E -->|ValidationException| F[400 Bad Request]
    E -->|BusinessException| G[422 Unprocessable Entity]
    E -->|NotFoundException| H[404 Not Found]
    E -->|UnauthorizedException| I[401 Unauthorized]
    E -->|ExternalServiceException| J[502 Bad Gateway]
    E -->|InfrastructureException| K[503 Service Unavailable]
    E -->|UnhandledException| L[500 Internal Server Error]
    F --> M[Formatar ErrorResponse JSON]
    G --> M
    H --> M
    I --> M
    J --> M
    K --> M
    L --> M
    M --> N[Log estruturado]
    N --> O[Retornar resposta ao cliente]
```

### Fluxo de Retry com Backoff Exponencial

```mermaid
sequenceDiagram
    participant Client as Cliente
    participant API as API (Spring Boot)
    participant Ext as Serviço Externo

    Client->>API: Requisição
    API->>Ext: Tentar chamada externa
    alt Sucesso
        Ext-->>API: 200 OK
        API-->>Client: 200 OK
    else Falha temporária (5xx / timeout)
        Ext-->>API: Erro
        API->>API: Retry 1 (delay 1s)
        API->>Ext: Tentar novamente
        alt Sucesso
            Ext-->>API: 200 OK
            API-->>Client: 200 OK
        else Falha
            Ext-->>API: Erro
            API->>API: Retry 2 (delay 2s)
            API->>Ext: Tentar novamente
            alt Sucesso
                Ext-->>API: 200 OK
                API-->>Client: 200 OK
            else Falha
                Ext-->>API: Erro
                API->>API: Retry 3 (delay 4s)
                API->>Ext: Tentar novamente
                alt Sucesso
                    Ext-->>API: 200 OK
                    API-->>Client: 200 OK
                else Falha
                    Ext-->>API: Erro
                    API->>API: Circuit breaker abre
                    API-->>Client: 502 Bad Gateway + fallback
                end
            end
        end
    end
```

### Fluxo de Circuit Breaker

```mermaid
stateDiagram-v2
    [*] --> Closed : Estado inicial

    Closed --> Open : Falhas ≥ threshold (ex: 5)
    Closed --> Closed : Requisição com sucesso

    Open --> HalfOpen : Após timeout (ex: 30s)
    Open --> Open : Requisição rejeitada imediatamente

    HalfOpen --> Closed : Tentativa de probe com sucesso
    HalfOpen --> Open : Tentativa de probe com falha

    note right of Closed : Chamadas passam normalmente
    note right of Open : Chamadas retornam fallback
    note right of HalfOpen : Uma requisição de probe permitida
```

### Fluxo de Tratamento de Erros no Frontend

```mermaid
flowchart TD
    A[Requisição fetch] --> B{Resposta HTTP}
    B -->|2xx| C[Processar dados]
    B -->|400| D[Exibir erro de validação inline]
    B -->|401| E[Redirecionar para login]
    B -->|403| F[Exibir mensagem de acesso negado]
    B -->|404| G[Exibir página de não encontrado]
    B -->|422| H[Exibir erros de negócio ao usuário]
    B -->|429| I[Retry automático com delay]
    B -->|5xx| J[Tentar retry uma vez]
    J -->|Sucesso| C
    J -->|Falha| K[Exibir erro genérico + suporte]

    D --> L[Destacar campos com erro]
    E --> M[Limpar sessão]
    I --> N[Exibir toast de aguardando]
```

### Registro Central de Códigos de Erro

```mermaid
flowchart TD
    A[Código de Erro] --> B{Prefixo}
    B -->|VAL_| C[Validação]
    B -->|BIZ_| D[Regra de Negócio]
    B -->|INF_| E[Infraestrutura]
    B -->|EXT_| F[Serviço Externo]
    B -->|AUTH_| G[Autenticação/Autorização]

    C --> C1[VAL-001: Campo obrigatório]
    C --> C2[VAL-002: Formato inválido]
    C --> C3[VAL-003: Tamanho excedido]

    D --> D1[BIZ-001: Lead já existe]
    D --> D2[BIZ-002: Pipeline inválido]
    D --> D3[BIZ-003: Status incompatível]

    E --> E1[INF-001: Banco indisponível]
    E --> E2[INF-002: Cache indisponível]
    E --> E3[INF-003: Fila indisponível]

    F --> F1[EXT-001: WhatsApp API timeout]
    F --> F2[EXT-002: Email provider erro]
    F --> F3[EXT-003: CRM externo falha]

    G --> G1[AUTH-001: Token expirado]
    G --> G2[AUTH-002: Permissão insuficiente]
    G --> G3[AUTH-003: Conta bloqueada]

    style C fill:#d4edda
    style D fill:#fff3cd
    style E fill:#f8d7da
    style F fill:#cce5ff
    style G fill:#e2d5f1
```

## Dependências

| Dependência | Versão | Uso |
|---|---|---|
| Spring Boot | 3 | `@ControllerAdvice`, `@ExceptionHandler` |
| Resilience4j | latest | Circuit breaker, retry, rate limiter |
| Next.js | 14 | Error boundaries, tratamento de fetch errors |
| React | 18 | Error boundaries, fallback UI |

## Boas Práticas

- **Formato JSON padronizado**: Toda resposta de erro deve seguir o formato `ErrorResponse` com campos `code`, `message`, `details`, `timestamp`, `traceId`
- **Nunca expor stack traces**: Em produção, respostas de erro não devem conter detalhes internos da exceção
- **Códigos de erro estáveis**: Códigos como `VAL-001` não podem ser alterados; são contratos públicos
- **Mensagens amigáveis**: Mensagens de erro devem ser claras para o usuário final e em português
- **Log completo**: O log estruturado deve conter stack trace completa, mas a resposta HTTP não
- **Correlation ID**: Incluir `traceId` em toda resposta de erro para facilitar diagnóstico
- **Rate limiting**: Utilizar Resilience4j RateLimiter para proteger endpoints contra abuso
- **Fallback graceful**: Quando um serviço dependente falhar, retornar dados parciais quando possível
- **Monitoramento de taxas de erro**: Alertar quando a taxa de erro 5xx exceder 1% em 5 minutos
- **Tratamento assíncrono**: Erros em filas (RabbitMQ) devem ter dead-letter queue e retry configurado

## Referências

- [Spring Boot - Exception Handling](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-controller-advise.html)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [RFC 7807 - Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc7807)
- [Microsoft REST API Guidelines - Errors](https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#7102-error-handling)

## Histórico de Revisão

| Data | Versão | Autor | Descrição |
|---|---|---|---|
| 15/07/2026 | 1.0 | Equipe de Arquitetura | Versão inicial da documentação de tratamento de erros |
