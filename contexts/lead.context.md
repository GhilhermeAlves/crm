# Lead Context

## Resumo do Módulo
Gestão do ciclo de vida de leads com scoring automático (0-100) baseado em 4 dimensões e classificação por temperatura. Distribuição round-robin.

## Objetivo
Capturar, qualificar e converter leads em oportunidades no pipeline.

## Responsabilidades
- Lifecycle: New → Contacted → Qualified → Converted/Lost
- Scoring automático 0-100 com 4 dimensões
- Classificação: Hot(80-100), Warm(50-79), Cold(20-49), Disqualified(0-19)
- Distribuição round-robin entre agentes
- Integração com campanhas e WhatsApp

## Scoring (0-100)
| Dimensão | Peso | Fatores |
|----------|------|---------|
| Source | 25% | Canal de origem, campanha |
| Engagement | 30% | Interações, aberturas, cliques |
| Data | 25% | Completude dos dados |
| Profile | 20% | Fit com ICP definido |

## Classificação
- **Hot** (80-100) - Alta conversão, prioridade máxima
- **Warm** (50-79) - Potencial moderado
- **Cold** (20-49) - Baixo engajamento
- **Disqualified** (0-19) - Não qualificado

## APIs Relacionadas
- `GET /leads` - Listar leads (filtros: status, score, classificação)
- `POST /leads` - Criar lead
- `GET /leads/:id` - Detalhes com score
- `PUT /leads/:id` - Atualizar lead
- `PUT /leads/:id/status` - Avançar status
- `POST /leads/:id/convert` - Converter em oportunidade
- `POST /leads/distribute` - Round-robin manual

## Banco Relacionado
- `leads` - Dados do lead, score, status, classificação, assigned_to

## Componentes Frontend
- LeadsList, LeadDetail, LeadScore
- LeadConvertModal, LeadDistributionPanel
- LeadTimeline (histórico de interações)

## Componentes Backend
- `lead` module (Controllers, Services, Domain, Repository)
- `scoring` module (engine de scoring com regras configuráveis)
- `distribution` module (round-robin balanceado)

## Eventos
- `LeadCreated` - Novo lead capturado
- `LeadScoreChanged` - Score recalculado
- `LeadClassified` - Classificação alterada
- `LeadConverted` - Convertido em oportunidade
- `LeadLost` - Marcado como perdido
- `LeadAssigned` - Distribuído para agente

## Permissões
- `lead:create` - ADMIN, MANAGER, AGENT
- `lead:read` - Todos
- `lead:update` - ADMIN, MANAGER, AGENT
- `lead:convert` - ADMIN, MANAGER
- `lead:distribute` - ADMIN, MANAGER
- `lead:delete` - ADMIN

## Dependências
- **Contacts** - Lead vinculado a contato
- **Pipeline** - Conversão cria oportunidade
- **Campaigns** - Leads de campanhas
- **WhatsApp** - Interações via WhatsApp

## Fluxo Resumido
1. Lead capturado (form/API/campanha) → scoring automático → classificação
2. Lead atribuído via round-robin → agente contata → status avança
3. Lead qualificado → `POST /leads/:id/convert` → oportunidade criada no pipeline

## Checklist de Implementação
- [ ] Lifecycle: New→Contacted→Qualified→Converted/Lost
- [ ] Scoring: 4 dimensões (Source 25%, Engagement 30%, Data 25%, Profile 20%)
- [ ] Classificação automática por faixa de score
- [ ] Round-robin balanceado para distribuição
- [ ] Integração com pipeline (conversão)
- [ ] Timeline de interações
- [ ] Filtros avançados (score, status, classe)
- [ ] Re-score automático em mudanças de dados

## Checklist de Testes
- [ ] Scoring calcula corretamente as 4 dimensões
- [ ] Classificação atribui faixa correta
- [ ] Round-robin distribui igualmente
- [ ] Conversão cria oportunidade no pipeline
- [ ] Re-score atualiza ao modificar dados

## Documentação Oficial Relacionada
- `docs/lead/LEAD-SCORING.md`
- `docs/lead/LIFECYCLE.md`
- `docs/lead/DISTRIBUTION.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
