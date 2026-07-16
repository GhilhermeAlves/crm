# Playbook: Implementação do Módulo AI

## Objetivo
Implementar a integração com OpenAI para funcionalidades de IA: sugestão de respostas, classificação de leads, análise de sentimento, sumarização de conversas, e geração de conteúdo.

## Pré-requisitos
- Módulo Conversations implementado (dados de conversas para IA)
- Módulo Auth implementado (rate limiting por empresa)
- Conta OpenAI configurada (API key)
- Variável de ambiente: OPENAI_API_KEY, OPENAI_MODEL (gpt-4o-mini por padrão)
- Rate limiting configurado: 100 chamadas/hora/empresa

## Documentos que DEVEM ser lidos
- `docs/AI.md`
- `docs/OpenAI.md`
- `docs/Integration.md`
- `contexts/integration-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/infrastructure/integration/openai/` — OpenAIClient, PromptTemplates, RateLimiter
- `packages/backend/src/application/ai/` — SuggestReplyUseCase, ClassifyLeadUseCase, SentimentAnalysisUseCase, SummarizeConversationUseCase, GenerateContentUseCase
- `packages/backend/src/presentation/rest/controller/AIController.ts`
- `packages/backend/src/infrastructure/persistence/` — AIRateLimitRepository

### Frontend
- `packages/frontend/src/components/ai/` — SuggestReply, LeadClassification, SentimentBadge, SummaryPanel, ContentGenerator
- `packages/frontend/src/hooks/useAI.ts`
- `packages/frontend/src/app/(auth)/ai/` — Páginas (se aplicável)

## Arquivos proibidos
- `packages/backend/src/infrastructure/integration/whatsapp/` — WhatsApp integration não deve ser alterada
- `packages/backend/src/presentation/websocket/` — WebSocket gateways não devem ser alterados
- `packages/backend/src/domain/` — Nenhuma entidade de domínio deve ser alterada

## Ordem de implementação
1. Configurar OpenAIClient com retry e timeout
2. Implementar RateLimiter (100 chamadas/hora/empresa)
3. Definir PromptTemplates para cada feature
4. Implementar SuggestReplyUseCase (sugere resposta baseada no contexto da conversa)
5. Implementar ClassifyLeadUseCase (classifica lead: hot/warm/cold)
6. Implementar SentimentAnalysisUseCase (analisa sentimento: positive/neutral/negative)
7. Implementar SummarizeConversationUseCase (resume conversa longa)
8. Implementar GenerateContentUseCase (gera conteúdo para follow-up, proposta)
9. Implementar AIController com endpoints
10. Criar componentes frontend: SuggestReply no ChatWindow, SentimentBadge no LeadDetail
11. Integrar com hook useAI

## Checklist Backend
- [ ] OpenAIClient: wrapper com retry (3 tentativas), timeout (30s), logging
- [ ] OpenAIClient: tratamento de erros (rate limit, quota, invalid response)
- [ ] RateLimiter: 100 chamadas/hora/empresa (Redis-based)
- [ ] RateLimiter: retorna 429 com Retry-After quando excedido
- [ ] PromptTemplates: template para cada feature com system message otimizada
- [ ] **SuggestReplyUseCase:**
  - Input: conversationId, messageHistory (últimas 20 mensagens)
  - Output: suggested_reply (string), confidence (float)
  - Prompt: "Com base na conversa, sugira uma resposta profissional e empática"
- [ ] **ClassifyLeadUseCase:**
  - Input: leadId, contactData, interactionHistory
  - Output: classification (hot/warm/cold), reasoning (string)
  - Prompt: "Classifique este lead baseado em engajamento, fit, e intenção de compra"
- [ ] **SentimentAnalysisUseCase:**
  - Input: text (mensagem ou conversa)
  - Output: sentiment (positive/neutral/negative), score (float -1 to 1)
  - Prompt: "Analise o sentimento desta mensagem"
- [ ] **SummarizeConversationUseCase:**
  - Input: conversationId, messageHistory
  - Output: summary (string), key_points (string[]), action_items (string[])
  - Prompt: "Resuma esta conversa, liste pontos-chave e itens de ação"
- [ ] **GenerateContentUseCase:**
  - Input: type (follow-up/proposal/thank-you), context (contact info, conversation)
  - Output: content (string)
  - Prompt: "Gere conteúdo profissional para [tipo] baseado no contexto"
- [ ] AIController: POST /ai/suggest-reply, POST /ai/classify-lead, POST /ai/sentiment, POST /ai/summarize, POST /ai/generate-content
- [ ] Cache de respostas (mesma conversa/texto = mesma resposta por 1h)
- [ ] Logs de chamadas OpenAI (custo, tokens, latência)
- [ ] Multi-tenancy: rate limit por company_id

## Checklist Frontend
- [ ] SuggestReply: componente no ChatWindow que mostra resposta sugerida com botão "Usar"
- [ ] LeadClassification: badge/classificação no LeadDetail (hot 🔥, warm ⚡, cold ❄️)
- [ ] SentimentBadge: indicador de sentimento na mensagem (emoji ou cor)
- [ ] SummaryPanel: painel de resumo da conversa (pontos-chave, itens de ação)
- [ ] ContentGenerator: formulário para gerar conteúdo (selecionar tipo, contexto)
- [ ] Hook useAI: suggestReply, classifyLead, analyzeSentiment, summarize, generateContent
- [ ] Loading states durante chamadas de IA
- [ ] Tratamento de erro (rate limit, falha da API)
- [ ] Indicador de confiança (quando aplicável)
- [ ] Botão "Copiar" para respostas sugeridas

## Checklist Banco
- [ ] Tabela `ai_rate_limits`: id, company_id (FK), action_type, count, window_start, created_at
- [ ] Índice: ai_rate_limits.company_id + action_type + window_start
- [ ] Tabela `ai_usage_logs`: id, company_id (FK), action_type, tokens_used, cost, latency_ms, created_at
- [ ] Índice: ai_usage_logs.company_id + created_at
- [ ] TTL automático em ai_rate_limits (cleanup a cada hora)
- [ ] TTL automático em ai_usage_logs (cleanup após 30 dias)

## Checklist Testes
- [ ] Testes unitários: RateLimiter (contagem, reset, excedido)
- [ ] Testes unitários: PromptTemplates (formatação correta)
- [ ] Testes de integração: SuggestReplyUseCase (mock OpenAI)
- [ ] Testes de integração: ClassifyLeadUseCase (mock OpenAI)
- [ ] Testes de integração: SentimentAnalysisUseCase (mock OpenAI)
- [ ] Testes de integração: SummarizeConversationUseCase (mock OpenAI)
- [ ] Testes de integração: GenerateContentUseCase (mock OpenAI)
- [ ] Testes de integração: Rate limiting bloqueia após 100 chamadas
- [ ] Testes de integração: Cache retorna resposta cacheada
- [ ] Testes E2E: Sugerir回复 no chat → usar resposta → verificar envio
- [ ] Testes de retry: falha temporária retry funciona

## Checklist Documentação
- [ ] Atualizar `docs/AI.md` com features, endpoints, exemplos
- [ ] Atualizar `docs/OpenAI.md` com configuração e uso
- [ ] Documentar prompts utilizados para cada feature
- [ ] Documentar rate limiting (100 chamadas/hora/empresa)
- [ ] Documentar variáveis de ambiente
- [ ] Documentar custos estimados por feature

## Checklist Final
- [ ] Sugestão de respostas funciona no chat
- [ ] Classificação de leads retorna resultado coerente
- [ ] Análise de sentimento funciona
- [ ] Sumarização de conversas funciona
- [ ] Geração de conteúdo funciona
- [ ] Rate limiting bloqueia após 100 chamadas/hora/empresa
- [ ] Cache reduz chamadas à API
- [ ] Retry funciona em caso de falha temporária
- [ ] Logs de uso são registrados
- [ ] Multi-tenancy isola rate limits por empresa
- [ ] Todos os testes passam
