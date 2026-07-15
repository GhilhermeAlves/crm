# Versionamento de API

## Objetivo

Definir a estratégia de versionamento da API REST do CRM SaaS Omnichannel, garantindo evolução compatível, comunicação clara com consumidores e transição segura entre versões. O versionamento deve permitir introduzir mudanças sem quebrar clientes existentes, mantendo ao mesmo tempo capacidade de inovação contínua.

## Escopo

- Versionamento via URL (`/api/v1/`, `/api/v2/`)
- Política de depreciação de versões anteriores
- Definição de breaking changes vs. non-breaking changes
- Ciclo de vida de cada versão da API
- Guia de migração entre versões
- Documentação OpenAPI (Swagger) por versão
- Versionamento de contratos com o frontend (Next.js 14)

## Responsabilidades

| Área | Responsabilidade |
|---|---|
| Backend (Spring Boot) | Implementar versionamento, manter versões, gerar OpenAPI |
| Arquitetura | Definir política de versionamento, aprovar breaking changes |
| Frontend (Next.js) | Consumir versão estável, planejar migração para novas versões |
| Documentação | Manter OpenAPI atualizada, escrever guias de migração |
| QA | Testar compatibilidade entre versões |

## Fluxos

### Fluxo de Versãoamento de Requisição

```mermaid
sequenceDiagram
    participant Client as Cliente (Frontend/Integrador)
    participant GW as API Gateway / Load Balancer
    participant API as Spring Boot 3
    participant Router as Router de Versão

    Client->>GW: GET /api/v1/contacts
    GW->>API: Encaminhar requisição
    API->>Router: Identificar versão (v1)
    Router->>Router: Mapear para ContactControllerV1
    Router->>API: Executar handler v1
    API-->>Client: 200 OK (formato v1)

    Client->>GW: GET /api/v2/contacts
    GW->>API: Encaminhar requisição
    API->>Router: Identificar versão (v2)
    Router->>Router: Mapear para ContactControllerV2
    Router->>API: Executar handler v2
    API-->>Client: 200 OK (formato v2)
```

### Fluxo de Depreciação

```mermaid
flowchart LR
    A[Nova versão lançada] --> B[Notificar consumidores]
    B --> C[Período de sobreposição]
    C --> D{Versão antiga ainda utilizada?}
    D -->|Sim| E[Estender período]
    D -->|Não| F[Marcar como deprecated]
    F --> G[Adicionar header de aviso]
    G --> H[Monitorar uso]
    H --> I[Uso = 0]
    I --> J[Remover versão]

    style C fill:#fff3cd
    style F fill:#f8d7da
    style J fill:#d4edda
```

### Fluxo de Breaking Change

```mermaid
flowchart TD
    A[Mudança proposta] --> B{É breaking change?}
    B -->|Não| C[Adicionar à versão atual]
    C --> D[Publicar como minor/patch]
    D --> E[Sem necessidade de nova versão]

    B -->|Sim| F[Criar nova versão da API]
    F --> G[Implementar em nova branch]
    G --> H[Documentar migração]
    H --> I[Publicar versão beta]
    I --> J[Feedback de consumidores]
    J --> K[Publicar versão estável]
    K --> L[Iniciar depreciação da versão anterior]

    style B fill:#fff3cd
    style F fill:#cce5ff
    style K fill:#d4edda
```

### Ciclo de Vida da Versão

```mermaid
stateDiagram-v2
    [*] --> Alpha : Desenvolvimento
    Alpha --> Beta : Feature freeze
    Beta --> Stable : QA completo, aprovado
    Stable --> Deprecated : Nova versão estável lançada
    Deprecated --> Removed : Período de migração encerrado

    note right of Alpha : Não disponível para produção
    note right of Beta : Para testes por consumidores selecionados
    note right of Stable : Uso em produção, suporte completo
    note right of Deprecated : Ainda funcional, aviso de remoção
    note right of Removed : Endpoint removido, retorna 410
```

### Estrutura de URLs por Versão

```mermaid
flowchart TD
    A[Base URL] --> B[/api/v1/]
    A --> C[/api/v2/]
    A --> D[/api/v3/ - future]

    B --> B1[/contacts]
    B --> B2[/leads]
    B --> B3[/messages]
    B --> B4[/companies]

    C --> C1[/contacts]
    C --> C2[/leads]
    C --> C3[/messages]
    C --> C4[/companies]
    C --> C5[/search]

    style B fill:#d4edda
    style C fill:#cce5ff
    style D fill:#f8f9fa
```

## Dependências

| Dependência | Versão | Uso |
|---|---|---|
| Spring Boot | 3 | Framework REST com suporte a versionamento |
| SpringDoc OpenAPI | latest | Geração automática de documentação OpenAPI por versão |
| Next.js | 14 | Consumo da API versionada pelo frontend |
| Docker | 24 | Containerização de múltiplas versões |

## Boas Práticas

- **Versionamento na URL**: Utilizar `/api/v{n}/` para clareza e simplicidade de consumo
- **No breaking changes em versão estável**: Nunca alterar o contrato de uma versão estável sem criar nova versão
- **Header de deprecation**: Adicionar header `Deprecation: true` e `Sunset: {data}` quando uma versão estiver deprecated
- **Suporte mínimo**: Manter no máximo 2 versões ativas simultaneamente (estável + deprecated)
- **Período de migração**: Mínimo de 6 meses entre lançamento de nova versão e remoção da anterior
- **Changelog por versão**: Manter CHANGELOG.md com todas as mudanças de cada versão
- **OpenAPI por versão**: Gerar especificação OpenAPI independente para cada versão
- **Testes de compatibilidade**: Manter suite de testes para todas as versões ativas
- **Versão mínima do frontend**: Documentar qual versão da API cada versão do frontend consome

## Referências

- [Microsoft REST API Guidelines - Versioning](https://github.com/microsoft/api-guidelines/blob/vNext/azure/Guidelines.md#versioning)
- [Stripe API Versioning](https://stripe.com/blog/api-versioning)
- [OpenAPI Specification 3.1](https://spec.openapis.org/oas/v3.1.0)
- [RFC 8594 - Sunset Header Field](https://www.rfc-editor.org/rfc/rfc8594)

## Histórico de Revisão

| Data | Versão | Autor | Descrição |
|---|---|---|---|
| 15/07/2026 | 1.0 | Equipe de Arquitetura | Versão inicial da documentação de versionamento de API |
