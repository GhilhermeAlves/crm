# Feature Flags

## Objetivo

Documentar o sistema de feature flags do CRM SaaS Omnichannel, definindo tipos, configuração, fluxo de avaliação e casos de uso para rollout gradual, testes A/B e modo de manutenção.

## Escopo

- Tipos de feature flags (boolean, percentage, user segment, plan-based)
- Configuração e armazenamento das flags
- Fluxo de avaliação das flags
- Casos de uso: rollout gradual, testes A/B, modo de manutenção
- Ciclo de vida das flags (criação, ativação, desativação, remoção)
- Interface de administração

## Responsabilidades

| Papel | Responsabilidade |
|---|---|
| Product Owner | Definir quais features terão flags e critérios de rollout |
| Backend Developer | Implementar engine de avaliação e API de gestão |
| Frontend Developer | Consumir flags na UI e implementar alternativas visuais |
| DevOps | Gerenciar configuração das flags em produção |
| QA | Testar comportamento com flags ativadas e desativadas |

## Fluxos

### Tipos de Feature Flags

```mermaid
mindmap
  root((Feature Flags))
    Boolean
      on/off simples
      Manutenção
      Feature toggle
    Percentage
      Rollout gradual
      0-100%
      Rampa de liberação
    User Segment
      Por empresa
      Por role
      Por tenant
    Plan-Based
      Free vs Starter
      Professional vs Enterprise
      Feature gating
```

### Ciclo de Vida de uma Feature Flag

```mermaid
stateDiagram-v2
    [*] --> Criada : Dev cria flag
    Criada --> Desenvolvimento : Flag em ambiente dev
    Desenvolvimento --> Staging : Deploy para staging
    Staging --> Rollout_Gradual : Deploy para produção
    Rollout_Gradual --> Ativada_100pct : 100% dos usuários
    Rollout_Gradual --> Revertida : Problema detectado
    Ativada_100pct --> Removida : Código permanente
    Revertida --> Desenvolvimento : Investigar e corrigir
    Removida --> [*] : Flag obsoleta removida
```

### Fluxo de Avaliação

```mermaid
flowchart TD
    A[Requisição com contexto do usuário] --> B{Flag existe?}
    B -->|Não| C[Retornar valor default]
    B -->|Sim| D{Flag ativa?}
    D -->|Não| C
    D -->|Sim| E{Tipo da flag?}
    E -->|Boolean| F[Retornar true/false]
    E -->|Percentage| G[Calcular hash do user_id]
    G --> H{Dentro do percentual?}
    H -->|Sim| F
    H -->|Não| C
    E -->|User Segment| I{Usuário no segmento?}
    I -->|Sim| F
    I -->|Não| C
    E -->|Plan-Based| J{Plano do usuário?}
    J -->|Plano elegível| F
    J -->|Plano não elegível| C
    F --> K[Aplicar feature]
    C --> L[Aplicar alternativa/default]
```

### Rollout Gradual

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant FE as Frontend
    participant BE as Backend (Feature Flag Service)
    participant Cache as Redis Cache
    participant DB as PostgreSQL

    Admin->>BE: PUT /api/flags/new-dashboard {percentage: 10}
    BE->>DB: Atualizar flag
    BE->>Cache: Invalidar flag
    loop A cada 24h ou sob demanda
        Admin->>BE: PUT /api/flags/new-dashboard {percentage: 25}
        BE->>DB: Atualizar flag
        BE->>Cache: Invalidar flag
    end
    Note over Admin,BE: Monitore métricas entre cada incremento
    FE->>BE: GET /api/flags/evaluate
    BE->>Cache: Buscar flag
    Cache-->>BE: Flag atualizada
    BE-->>FE: {new_dashboard: true}
    FE->>FE: Renderizar novo dashboard
```

### Testes A/B

```mermaid
flowchart LR
    A[Usuário acessa feature] --> B{Hash do user_id}
    B -->|Par| C[Variante A - Layout Atual]
    B -->|Ímpar| D[Variante B - Novo Layout]
    C --> E[Registrar evento: variant_a]
    D --> F[Registrar evento: variant_b]
    E --> G[Metricas Analytics]
    F --> G
    G --> H{Suficientes dados?}
    H -->|Não| I[Continuar teste]
    H -->|Sim - Variante B vence| J[Promover Variante B]
    H -->|Sim - Sem vencedor| K[Estender teste ou reverter]
```

### Modo de Manutenção

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant BE as Backend
    participant FE as Frontend
    participant User as Usuário

    Admin->>BE: PUT /api/flags/maintenance_mode {enabled: true, message: "Manutenção 02:00-04:00"}
    BE->>BE: Ativar flag global maintenance_mode
    User->>FE: Acessa aplicação
    FE->>BE: GET /api/flags/evaluate
    BE-->>FE: {maintenance_mode: true}
    FE->>FE: Exibir tela de manutenção
    FE-->>User: "Sistema em manutenção. Volte às 04:00."
    Note over Admin,BE: Após manutenção
    Admin->>BE: PUT /api/flags/maintenance_mode {enabled: false}
    User->>FE: Acessa aplicação
    FE->>BE: GET /api/flags/evaluate
    BE-->>FE: {maintenance_mode: false}
    FE->>FE: Renderizar aplicação normalmente
    FE-->>User: Acesso restaurado
```

## Dependências

| Dependência | Tipo | Uso |
|---|---|---|
| PostgreSQL | Infra | Armazenamento persistente das flags e regras |
| Redis | Infra | Cache de flags para avaliação de baixa latência |
| RabbitMQ | Infra | Eventos de mudança de flags para invalidação distribuída |
| Admin Dashboard (Next.js) | Interna | Interface de gestão das feature flags |
| Analytics Service | Interna | Métricas de uso por variante em testes A/B |

## Boas Práticas

- **Naming convention**: Usar formato `namespace.action.target` (ex: `dashboard.new_layout.rollout`).
- **Default seguro**: Sempre definir um valor default que mantém o comportamento anterior.
- **Documentação**: Cada flag deve ter descrição, responsável, data de criação e data de expiração esperada.
- **Cleanup**: Remover flags obsoletas do código e do banco periodicamente.
- **Monitoramento**: Acompanhar métricas de erro e uso antes, durante e após rollout.
- **Rollback rápido**: Ter processo documentado para desativar uma flag em caso de problema.
- **Ambientes separados**: Flags devem poder ter valores diferentes por ambiente (dev, staging, production).
- **Auditoria**: Registrar todas as mudanças de flags com timestamp e responsável.
- **Segmentação estável**: Usar hash determinístico do user_id para segmentação, não randomização.

## Referências

- [Feature Flags Best Practices](https://launchdarkly.com/blog/feature-flag-best-practices/)
- [Martin Fowler - Feature Toggles](https://martinfowler.com/bliki/FeatureToggle.html)
- [Unleash Feature Flag Documentation](https://docs.getunleash.io/)

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0 | 15/07/2026 | Paulo Alves | Criação inicial do documento |
