# Campaign Context

## Resumo do Módulo
Gestão de campanhas de mensagens em massa com lifecycle DRAFT→SCHEDULED→RUNNING→PAUSED/COMPLETED. Rate 100 msgs/min, janela 8h-22h, LGPD compliant.

## Objetivo
Criar e executar campanhas de comunicação em massa com conformidade legal.

## Responsabilidades
- Lifecycle: DRAFT→SCHEDULED→RUNNING→PAUSED/COMPLETED
- Envio: max 100 mensagens/minuto
- Janela de envio: 8h-22h (horário local)
- Exclusão automática de opt-out (LGPD)
- Métricas de entrega e engajamento

## Lifecycle
`DRAFT` → `SCHEDULED` → `RUNNING` → `PAUSED` | `COMPLETED`

## APIs Relacionadas
- `GET /campaigns` - Listar campanhas
- `POST /campaigns` - Criar campanha
- `GET /campaigns/:id` - Detalhes + métricas
- `PUT /campaigns/:id` - Atualizar (DRAFT only)
- `POST /campaigns/:id/start` - Iniciar envio
- `POST /campaigns/:id/pause` - Pausar
- `POST /campaigns/:id/resume` - Retomar
- `GET /campaigns/:id/metrics` - Métricas em tempo real

## Banco Relacionado
- `campaigns` - Configuração da campanha
- `campaign_steps` - Etapas sequenciais

## Componentes Frontend
- CampaignsList, CampaignForm
- CampaignMetrics (real-time dashboard)
- CampaignScheduler (agendamento)
- OptOutManager

## Componentes Backend
- `campaign` module (Controllers, Services, Domain)
- `scheduler` module (agendamento de envios)
- `rate-limiter` (100 msgs/min)
- `lgpd` module (exclusão de opt-outs)
- `delivery-tracker` (métricas em tempo real)

## Eventos
- `CampaignCreated` - Campanha criada
- `CampaignScheduled` - Agendada
- `CampaignStarted` - Envio iniciado
- `CampaignPaused/Resumed` - Pausada/Retomada
- `CampaignCompleted` - Finalizada
- `CampaignMessageSent` - Mensagem individual enviada

## Permissões
- `campaign:create` - ADMIN, MANAGER
- `campaign:read` - Todos
- `campaign:start/pause` - ADMIN, MANAGER
- `campaign:delete` - ADMIN

## Dependências
- **Contacts** - Destinatários da campanha
- **Templates** - Mensagens de template
- **Messages** - Envio individual
- **Automations** - Triggers de campanha
- **WhatsApp** - Canal de envio

## Fluxo Resumido
1. Usuário cria campanha (DRAFT) → seleciona público → escolhe template
2. Agenda ou inicia → scheduler processa → rate limiter 100/min → janela 8h-22h
3. LGPD remove opt-outs → envia → métricas coletadas em tempo real

## Checklist de Implementação
- [ ] Lifecycle completo (DRAFT→SCHEDULED→RUNNING→PAUSED/COMPLETED)
- [ ] Rate limiting 100 msgs/min
- [ ] Janela de envio 8h-22h
- [ ] LGPD: exclusão automática de opt-outs
- [ ] Métricas em tempo real
- [ ] Pausa/retoma de envio
- [ ] Integração com WhatsApp
- [ ] Dashboard de campanha

## Checklist de Testes
- [ ] Rate limit 100 msgs/min respeitado
- [ ] Janela 8h-22h bloqueia envio fora do horário
- [ ] LGPD remove opt-outs corretamente
- [ ] Pausa interrompe envio imediatamente
- [ ] Métricas contabilizam corretamente

## Documentação Oficial Relacionada
- `docs/campaign/CAMPAIGN-LIFECYCLE.md`
- `docs/campaign/LGPD-COMPLIANCE.md`
- `docs/campaign/SENDING-STRATEGY.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
