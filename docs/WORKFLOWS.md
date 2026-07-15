# Workflows

## Objetivo

Documentar os principais fluxos de negócio do CRM SaaS Omnichannel, incluindo o ciclo de vida de Leads, Opportunities, Campaigns, Automations e atendimento ao Customer via chat.

## Escopo

Cobre os 5 workflows fundamentais do sistema: Lead Lifecycle, Opportunity Lifecycle, Campaign Execution, Automation Processing e Chat Flow.

## Responsabilidades

| Workflow | Owner Principal | Participantes |
|---|---|---|
| **Lead Lifecycle** | Contact Service | Pipeline Service, Analytics, Campaign |
| **Opportunity Lifecycle** | Pipeline Service | Communication, Analytics, Integration |
| **Campaign Execution** | Campaign Service | Communication, Contact, Analytics |
| **Automation Processing** | Automation Engine | Todos os bounded contexts |
| **Chat Flow** | Communication Service | Pipeline Service, Contact Service, Notification |

## Fluxos

### 1. Lead Lifecycle

```mermaid
sequenceDiagram
    participant U as Usuário
    participant API as API Gateway
    participant CS as Contact Service
    participant PS as Pipeline Service
    participant AS as Analytics Service
    participant CAMP as Campaign Service
    participant MQ as RabbitMQ

    U->>API: Cadastra novo contato
    API->>CS: CreateContact
    CS->>CS: Valida dados e regras
    CS->>MQ: ContactCreated
    MQ-->>AS: Atualiza métricas
    MQ-->>CAMP: Verifica segmentação

    U->>API: Classifica como Lead
    API->>CS: QualificaLead
    CS->>CS: Calcula score inicial
    CS->>MQ: LeadScoreChanged
    MQ-->>PS: Cria oportunidade automática

    Note over CS: Lead: NEW

    CS->>CS: Envia primeira contato
    CS->>MQ: ContactFirstTouch
    Note over CS: Lead: NEW → CONTACTED

    U->>API: Registra interação qualificadora
    API->>CS: QualifyLead
    CS->>CS: Atualiza score e critérios
    CS->>MQ: LeadScoreChanged
    Note over CS: Lead: CONTACTED → QUALIFIED

    alt Lead convertido
        U->>API: Converte Lead
        API->>CS: ConvertLead
        CS->>MQ: LeadConverted
        MQ-->>PS: Promove para Opportunity
        Note over CS: Lead: QUALIFIED → CONVERTED
    else Lead perdido
        U->>API: Marca como perdido
        API->>CS: LoseLead
        CS->>MQ: LeadLost
        Note over CS: Lead: QUALIFIED → LOST
    end
```

### 2. Opportunity Lifecycle

```mermaid
sequenceDiagram
    participant U as Usuário
    participant API as API Gateway
    participant PS as Pipeline Service
    participant CS as Contact Service
    participant COMM as Communication
    participant AS as Analytics Service
    participant INT as Integration Service
    participant MQ as RabbitMQ

    U->>API: Cria oportunidade
    API->>PS: CreateOpportunity
    PS->>PS: Valida pipeline e estágio
    PS->>MQ: OpportunityCreated
    MQ-->>AS: Registra nova oportunidade
    MQ-->>COMM: Inicia follow-up
    Note over PS: Opportunity: OPEN

    U->>API: Avança para negociação
    API->>PS: MoveStage(negotiation)
    PS->>PS: Valida transição
    PS->>MQ: StageChanged
    MQ-->>AS: Atualiza funil
    Note over PS: Opportunity: OPEN → NEGOTIATION

    U->>API: Envia proposta
    API->>PS: MoveStage(proposal)
    PS->>PS: Valida dados da proposta
    PS->>MQ: StageChanged
    MQ-->>COMM: Envia proposta ao cliente
    MQ-->>AS: Atualiza pipeline
    Note over PS: Opportunity: NEGOTIATION → PROPOSAL

    alt Oportunidade ganha
        U->>API: Fecha como ganha
        API->>PS: WinOpportunity
        PS->>PS: Registra valor final e motivo
        PS->>MQ: OpportunityWon
        MQ-->>AS: Calcula métricas de conversão
        MQ-->>INT: Sincroniza com CRM externo
        Note over PS: Opportunity: PROPOSAL → WON
    else Oportunidade perdida
        U->>API: Fecha como perdida
        API->>PS: LoseOpportunity
        PS->>PS: Registra motivo da perda
        PS->>MQ: OpportunityLost
        MQ-->>AS: Registra motivo para análise
        Note over PS: Opportunity: PROPOSAL → LOST
    end
```

### 3. Campaign Execution

```mermaid
sequenceDiagram
    participant U as Usuário
    participant API as API Gateway
    participant CAMP as Campaign Service
    participant CS as Contact Service
    participant COMM as Communication
    participant AS as Analytics Service
    participant MQ as RabbitMQ

    U->>API: Cria campanha
    API->>CAMP: CreateCampaign
    CAMP->>CAMP: Valida configurações
    CAMP->>MQ: CampaignCreated
    Note over CAMP: Campaign: DRAFT

    U->>API: Agenda ou inicia campanha
    API->>CAMP: StartCampaign
    CAMP->>CS: Consulta audiência-alvo
    CS-->>CAMP: Retorna contatos qualificados
    CAMP->>MQ: CampaignStarted
    Note over CAMP: Campaign: DRAFT → RUNNING

    loop Para cada contato da audiência
        CAMP->>CAMP: Seleciona próximo contato
        CAMP->>CAMP: Verifica rate limit e horário
        CAMP->>COMM: Envia mensagem via canal
        COMM-->>CAMP: Confirma envio
        CAMP->>MQ: CampaignExecutionSent
    end

    loop Recebe interações
        COMM->>CAMP: Registra interação
        alt Conversão detectada
            CAMP->>CAMP: Registra conversão
            CAMP->>MQ: CampaignConverted
            MQ-->>AS: Atualiza métricas
        else Opt-out registrado
            CAMP->>CS: Remove da audiência
        end
    end

    U->>API: Pausa campanha
    API->>CAMP: PauseCampaign
    CAMP->>MQ: CampaignPaused
    Note over CAMP: Campaign: RUNNING → PAUSED

    U->>API: Retoma campanha
    API->>CAMP: ResumeCampaign
    CAMP->>MQ: CampaignStarted
    Note over CAMP: Campaign: PAUSED → RUNNING

    CAMP->>CAMP: Todos os contatos processados
    CAMP->>MQ: CampaignCompleted
    Note over CAMP: Campaign: RUNNING → COMPLETED
```

### 4. Automation Processing

```mermaid
sequenceDiagram
    participant TRIGGER as Event Trigger
    participant MQ as RabbitMQ
    participant AE as Automation Engine
    participant CS as Contact Service
    participant PS as Pipeline Service
    participant COMM as Communication
    participant CAMP as Campaign Service
    participant AS as Analytics Service

    TRIGGER->>MQ: Domain Event
    MQ->>AE: Evento recebido

    AE->>AE: Busca regras ativas para o evento

    loop Para cada regra aplicável
        AE->>AE: Avalia condições (predicados)

        alt Condições atendidas
            alt Ação: Enviar mensagem
                AE->>COMM: SendMessage(template, contactId)
                COMM-->>AE: Mensagem enviada
            else Ação: Mover oportunidade
                AE->>PS: MoveStage(opportunityId, targetStage)
                PS-->>AE: Estágio atualizado
            else Ação: Atualizar score
                AE->>CS: CalculateLeadScore(contactId, rules)
                CS-->>AE: Score recalculado
            else Ação: Adicionar a campanha
                AE->>CAMP: AddToCampaign(contactId, campaignId)
                CAMP-->>AE: Contato adicionado
            else Ação: Criar tarefa
                AE->>PS: CreateActivity(activityData)
                PS-->>AE: Tarefa criada
            else Ação: Notificar equipe
                AE->>COMM: SendNotification(teamId, message)
                COMM-->>AE: Notificação enviada
            end

            AE->>AS: LogAutomationExecution(ruleId, result)
        else Condições não atendidas
            AE->>AE: Registra skip com motivo
        end
    end

    AE->>MQ: AutomationProcessed(eventId, ruleIds, results)
```

### 5. Chat Flow

```mermaid
sequenceDiagram
    participant C as Cliente
    participant CH as Chat Widget
    participant API as API Gateway
    participant COMM as Communication Service
    participant CS as Contact Service
    participant PS as Pipeline Service
    participant BOT as Chatbot/AI
    participant A as Agente
    participant MQ as RabbitMQ

    C->>CH: Inicia conversa
    CH->>API: StartConversation(channel, contactInfo)
    API->>COMM: CreateConversation
    COMM->>CS: LookupOrCreateContact(contactInfo)
    CS-->>COMM: Contact identificado
    COMM->>MQ: ConversationStarted
    Note over COMM: Conversation: OPEN

    COMM->>BOT: Avalia se responde automaticamente

    alt Bot responde
        BOT->>BOT: Processa intenção do cliente
        BOT->>COMM: BotResponse(message)
        COMM->>CH: Envia resposta
        C->>CH: Continua conversa
        CH->>COMM: Nova mensagem
        BOT->>BOT: Avalia complexidade

        alt Complexidade alta
            BOT->>COMM: Escala para agente
            COMM->>MQ: ConversationAssigned(agentId)
            Note over COMM: Conversation: OPEN → PENDING
            COMM->>A: Notifica agente disponível
        else Bot resolve
            BOT->>COMM: Resolve conversa
            COMM->>MQ: ConversationResolved
            Note over COMM: Conversation: OPEN → RESOLVED
        end
    else Roteamento direto
        COMM->>COMM: Roteamento por regras
        COMM->>A: Atribui ao agente
        COMM->>MQ: ConversationAssigned(agentId)
        Note over COMM: Conversation: OPEN → PENDING
    end

    loop Interação Agente-Cliente
        C->>CH: Envia mensagem
        CH->>COMM: SendMessage
        COMM->>MQ: MessageReceived
        COMM->>A: Exibe no painel do agente

        A->>COMM: Resposta do agente
        COMM->>MQ: MessageSent
        COMM->>CH: Exibe para o cliente
    end

    A->>COMM: Resolve conversa
    COMM->>MQ: ConversationResolved
    Note over COMM: Conversation: PENDING → RESOLVED

    COMM->>PS: Verifica se há oportunidade vinculada

    alt Oportunidade vinculada
        PS->>PS: Atualiza atividade da oportunidade
    else Sem oportunidade
        PS->>PS: Cria oportunidade automática
    end

    COMM->>COMM: Fecha sessão
    COMM->>MQ: ConversationClosed
    Note over COMM: Conversation: RESOLVED → CLOSED
```

## Dependências

- **RabbitMQ 3**: Transporte assíncrono de eventos entre workflows
- **Redis 7**: Cache de sessões de chat, rate limiting de campanhas
- **PostgreSQL 16**: Persistência de estado de todos os workflows
- **JWT**: Autenticação e propagação de contexto de tenant

## Boas práticas

- Workflows devem ser idempotentes — processamento repetido não deve criar duplicatas
- Transições de estado devem ser validadas antes de serem persistidas
- Eventos são publicados após a transação ser commitada com sucesso
- Timeouts configurados para chamadas entre services (máximo 30s)
- Circuit breaker em chamadas para serviços externos
- Logging estruturado com correlationId para rastreabilidade completa
- Workflows de longa duração usam filas separadas com prioridade
- Rollback compensatório quando etapas falham em workflows compostos

## Referências

- Workflow Patterns — workflowpatterns.com
- Saga Pattern — Chris Richardson
- Choreography vs Orchestration — Cam Jackson
- Spring State Machine Documentation

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0 | 2026-07-15 | Equipe de Arquitetura | Versão inicial dos workflows |
