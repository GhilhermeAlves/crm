# AI Context

## Resumo do Módulo
Integração com IA para chatbot (respostas sugeridas com aprovação humana), scoring de leads, análise de sentimento e resumo de conversas. GPT-4/GPT-3.5. 100 calls/hora/empresa.

## Objetivo
Aumentar produtividade com assistência inteligente em vendas e atendimento.

## Responsabilidades
- Chatbot com respostas sugeridas (approvação humana obrigatória)
- Lead scoring via IA
- Análise de sentimento de mensagens
- Resumo automático de conversas
- Rate limit: 100 calls/hora/empresa

## Funcionalidades
| Feature | Modelo | Descrição |
|---------|--------|-----------|
| Suggested Replies | GPT-3.5 | Respostas sugeridas no chat |
| Lead Scoring AI | GPT-4 | Scoring qualitativo além do calculado |
| Sentiment Analysis | GPT-3.5 | Classificação: positive/neutral/negative |
| Conversation Summary | GPT-4 | Resumo da conversa para handoff |

## APIs Relacionadas
- `POST /ai/suggest-reply` - Sugerir resposta
- `POST /ai/lead-score` - Scoring IA do lead
- `POST /ai/sentiment` - Analisar sentimento
- `POST /ai/summarize` - Resumir conversa
- `GET /ai/usage` - Uso atual (calls/hora)

## Componentes Frontend
- AISuggestionCard (respostas sugeridas)
- SentimentBadge (positive/neutral/negative)
- ConversationSummaryPanel
- AIUsageIndicator

## Componentes Backend
- `ai` module (Controllers, Services, Domain)
- `openai-client` (integração GPT-4/GPT-3.5)
- `prompt-builder` (templates de prompt)
- `approval` module (workflow de aprovação)
- `rate-limiter` (100 calls/hora/empresa)

## Eventos
- `AISuggestionGenerated` - Resposta sugerida criada
- `AISuggestionAccepted/Rejected` - Aprovação/rejeição
- `AISentimentAnalyzed` - Sentimento analisado
- `AISummaryGenerated` - Resumo criado

## Permissões
- `ai:suggest-reply` - AGENT
- `ai:lead-score` - ADMIN, MANAGER
- `ai:sentiment` - SYSTEM (automático)
- `ai:summarize` - ADMIN, MANAGER, AGENT

## Dependências
- **OpenAI Integration** - API externa
- **Conversations** - Dados para análise
- **Companies** - Rate limit por empresa

## Segurança
- **NENHUM dado sensível enviado à OpenAI**
- Apenas texto da conversa (sem PII)
- Logs de todas as chamadas para auditoria
- Consentimento do contato para análise

## Fluxo Resumido
1. Agente abre conversa → AI sugere resposta → agente aprova/rejeita
2. Lead criado → AI scoring complementar → combinado com scoring calculado
3. Conversa encerrada → AI gera resumo → disponível para handoff

## Checklist de Implementação
- [ ] Integração OpenAI (GPT-4/GPT-3.5)
- [ ] Suggested replies com aprovação
- [ ] Lead scoring AI complementar
- [ ] Sentiment analysis automatizado
- [ ] Conversation summary
- [ ] Rate limit 100 calls/hora/empresa
- [ ] Nenhum PII enviado à OpenAI
- [ ] Audit log de todas chamadas

## Checklist de Testes
- [ ] Resposta sugerida é relevante
- [ ] Aprovação/rejeição funciona
- [ ] Rate limit 100/hora respeitado
- [ ] Nenhum dado sensível é enviado
- [ ] Summary contém informações úteis

## Documentação Oficial Relacionada
- `docs/ai/OPENAI-INTEGRATION.md`
- `docs/ai/PROMPT-TEMPLATES.md`
- `docs/ai/DATA-PRIVACY.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
