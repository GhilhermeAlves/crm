# Billing Model

## Objetivo

Definir o modelo de cobrança do CRM SaaS Omnichannel, incluindo planos, limites, ciclo de vida da assinatura, integração com pagamento (Stripe), período de teste e fluxos de upgrade/downgrade.

## Escopo

- Planos disponíveis e features por plano
- Limites de uso (usuários, contatos, mensagens, armazenamento)
- Ciclo de vida da assinatura (trial, ativação, renovação, cancelamento)
- Integração com Stripe (assinaturas, faturas, webhooks)
- Fluxos de upgrade e downgrade
- Geração de faturas e relatórios de uso

## Responsabilidades

| Papel | Responsabilidade |
|---|---|
| Product Owner | Definir features e limites de cada plano |
| Backend Developer | Implementar integração com Stripe e lógica de billing |
| Frontend Developer | Implementar UI do portal de cobrança e gestão de plano |
| DevOps | Configurar webhooks e monitorar pipeline de pagamento |
| QA | Validar fluxos de assinatura, cobrança e limites |

## Fluxos

### Planos e Limites

```mermaid
mindmap
  root((Billing Model))
    Free
      1 Usuário
      500 Contatos
      1.000 Mensagens/mês
      1 GB Armazenamento
      Suporte por email
    Starter
      5 Usuários
      5.000 Contatos
      25.000 Mensagens/mês
      5 GB Armazenamento
      Suporte por chat
      Relatórios básicos
    Professional
      25 Usuários
      50.000 Contatos
      200.000 Mensagens/mês
      50 GB Armazenamento
      Suporte prioritário
      API access
      Automações
    Enterprise
      Usuários ilimitados
      Contatos ilimitados
      Mensagens ilimitadas
      500 GB Armazenamento
      Suporte 24/7 dedicado
      SLA garantido
      SSO/SAML
      Auditoria avançada
```

### Ciclo de Vida da Assinatura

```mermaid
stateDiagram-v2
    [*] --> Trial : Cadastro (14 dias)
    Trial --> Ativo : Pagamento aprovado
    Trial --> Cancelado : Período expirado sem pagamento
    Ativo --> Em_Atraso : Falha no pagamento
    Ativo --> Pausado : Usuário pausa assinatura
    Ativo --> Cancelado : Usuário cancela
    Pausado --> Ativo : Usuário reativa
    Em_Atraso --> Ativo : Pagamento reprocessado
    Em_Atraso --> Suspensao_Grace : 7 dias sem pagamento
    Suspensao_Grace --> Cancelado : 7 dias sem pagamento
    Cancelado --> Ativo : Reativação com novo pagamento
    Ativo --> Upgrade_Pendente : Solicita upgrade
    Upgrade_Pendente --> Ativo : Upgrade processado
    Ativo --> Downgrade_Pendente : Solicita downgrade
    Downgrade_Pendente --> Ativo : Downgrade processado no ciclo atual
```

### Fluxo de Checkout (Stripe)

```mermaid
sequenceDiagram
    participant U as Usuário
    participant FE as Frontend (Next.js)
    participant BE as Backend (Spring Boot)
    participant Stripe as Stripe API
    participant DB as PostgreSQL

    U->>FE: Seleciona plano
    FE->>BE: POST /api/billing/checkout
    BE->>Stripe: Criar sessão de checkout
    Stripe-->>BE: session_id + url
    BE-->>FE: Redirect URL
    FE->>Stripe: Página de pagamento Stripe
    U->>Stripe: Informa dados de pagamento
    Stripe->>BE: Webhook checkout.session.completed
    BE->>DB: Criar/atualizar assinatura
    BE->>DB: Ativar features do plano
    BE-->>FE: WebSocket: plano atualizado
    FE-->>U: Plano ativado com sucesso
```

### Fluxo de Upgrade

```mermaid
sequenceDiagram
    participant U as Usuário
    participant FE as Frontend
    participant BE as Backend
    participant Stripe as Stripe API
    participant DB as PostgreSQL

    U->>FE: Solicita upgrade de plano
    FE->>BE: POST /api/billing/upgrade
    BE->>DB: Calcular valor proporcional
    BE->>Stripe: Criar sessão de checkout (upgrade)
    Stripe-->>BE: session_id + url
    BE-->>FE: Redirect URL
    U->>Stripe: Confirma pagamento
    Stripe->>BE: Webhook invoice.paid
    BE->>DB: Atualizar plano e limites
    BE->>DB: Registrar crédito proporcional
    BE-->>U: Notificação de upgrade efetivado
```

### Fluxo de Downgrade

```mermaid
sequenceDiagram
    participant U as Usuário
    participant FE as Frontend
    participant BE as Backend
    participant Stripe as Stripe API
    participant DB as PostgreSQL

    U->>FE: Solicita downgrade de plano
    FE->>BE: POST /api/billing/downgrade
    BE->>DB: Verificar uso atual vs novos limites
    alt Uso dentro dos novos limites
        BE->>Stripe: Atualizar assinatura (próximo ciclo)
        BE->>DB: Agendar downgrade para fim do ciclo
        BE-->>U: Downgrade agendado
    else Uso excede novos limites
        BE-->>U: Aviso: reduza o uso antes de fazer downgrade
    end
```

### Fluxo de Fallback de Pagamento

```mermaid
sequenceDiagram
    participant Stripe as Stripe API
    participant BE as Backend
    participant DB as PostgreSQL
    participant N as Notificação

    Stripe->>BE: Webhook invoice.payment_failed
    BE->>DB: Atualizar status para Em_Atraso
    BE->>N: Email: falha no pagamento
    Note over BE,DB: 1º retry: 3 dias
    BE->>Stripe: Reintentar cobrança
    alt Retry sucesso
        Stripe->>BE: Webhook invoice.paid
        BE->>DB: Status → Ativo
    else Retry falha
        Note over BE,DB: 2º retry: 5 dias
        BE->>Stripe: Reintentar cobrança
        alt Retry sucesso
            Stripe->>BE: Webhook invoice.paid
            BE->>DB: Status → Ativo
        else Retry falha
            BE->>DB: Status → Suspensao_Grace
            BE->>N: Email: conta será suspensa em 7 dias
        end
    end
```

## Dependências

| Dependência | Tipo | Uso |
|---|---|---|
| Stripe API | Externa | Processamento de pagamentos, assinaturas e faturas |
| PostgreSQL | Infra | Armazenamento de dados de billing e assinaturas |
| Redis | Infra | Cache de limites de uso e status de assinatura |
| RabbitMQ | Infra | Eventos de billing (upgrade, downgrade, cancelamento) |
| SendGrid / SES | Externa | Envio de emails de fatura e notificações de pagamento |

## Boas Práticas

- **Idempotência**: Todas as operações de billing devem ser idempotentes, usando idempotency keys do Stripe.
- **Webhook verification**: Sempre verificar a assinatura dos webhooks do Stripe para evitar fraudes.
- **Grace period**: Implementar período de carência de 7 dias antes de suspender contas com falha de pagamento.
- **Proration**: Cálculo proporcional automático em upgrades durante o ciclo vigente.
- **Audit trail**: Registrar todas as transações de billing para auditoria e suporte.
- **Rate limiting**: Proteger endpoints de billing contra abuso.
- **Segurança**: Nunca armazenar dados de cartão de crédito. Delegar ao Stripe.
- **Backup billing**: Manter registro local do estado da assinatura como fallback.
- **Test mode**: Usar Stripe test mode para ambiente de desenvolvimento e staging.

## Referências

- [Stripe Billing Documentation](https://stripe.com/docs/billing)
- [Stripe Webhooks](https://stripe.com/docs/webhooks)
- [Stripe Customer Portal](https://stripe.com/docs/customer-management/portal)
- [Stripe API Reference](https://stripe.com/docs/api)

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0 | 15/07/2026 | Paulo Alves | Criação inicial do documento |
