# Sprint 20 — IA (Assistente Inteligente do CRM)

> **Status: ✅ CONCLUÍDA** — regularização documental pós-Sprint 16.
> Implementação entregue incrementalmente entre 2026-08-15 e 2026-08-19 e implantada na VPS
> junto das releases posteriores; este REPORT consolida o estado real comprovado em código/Git.

## Escopo

Assistente de IA integrado ao CRM (chat contextual, sugestões, análise, ações com confirmação),
com provider OpenAI real.

## Entregas (evidências no Git/código)

### AI-01 — Orquestrador
- Commit `07db73b` — `feat(ai): AI-01 Foundation - orquestrador do assistente de IA`.
- `application/ai/service/AiAssistantService`, portas `AiAssistantUseCase`/`AiProvider`,
  persistência de conversas (`V050__ai_chat_tables.sql`,
  `AiConversationJpaRepository`/`AiMessageJpaRepository`).

### AI-02 — Context Engine
- Commit `f522280` — `feat(ai): implementar context engine (AI-02)`.
- `application/ai/context/` (`AiContextResolver`, resolvers por entidade,
  `Customer360ContextBuilder`) + `AiContextualAnalysisService`.

### AI-03 — CRM Read Tools
- Mesmo commit `f522280`.
- `application/ai/tool/` — `AiToolRegistry`, `AbstractAiReadTool` e tools de leitura
  (contacts, opportunities, activities, tasks, pipeline, customer360, searches).

### AI-04 / AI-05 — Write Tools com confirmação + proteção
- Commits `68690d8` (AI-04/AI-05) e `42273b9` (fix dependência circular).
- `application/ai/action/` (`AiActionService`, `AiActionExecutor`),
  `application/ai/tool/write/` (CreateTask/CreateActivity/UpdateOpportunity),
  `V051__ai_actions.sql`; execução de ação exige confirmação do usuário;
  permissões `ai:chat`/`ai:suggest` concedidas a todos os papéis (`V052` + `e294817`).

### AI-06 — Inteligência contextual
- Commits `32be5fb`/`554aed6` (backend + frontend).

### Provider OpenAI (fake removido)
- Commit `f180ad9` — `refactor(ai): remove providers fake e adota OpenAI como default`;
  `772a980` passa `AI_PROVIDER`/`OPENAI_API_KEY` ao backend no deploy.
- Integração em `infrastructure/ai/openai`; correções de tool calling para o protocolo
  da OpenAI (`39edf6f`, `a19f9ef`).

### Assistente "Leo"
- Commit `5a7db2f` — assistente nomeado **Leo** (frontend: `AiChatAssistant`, Header/Sidebar).

## Qualidade

- Backend: testes de IA verdes no CI (`AiAssistantServiceTest`, `AiActionServiceTest`,
  `AiReadToolsTest`, `AiWriteToolsTest`, `AiToolRegistryTest`, `AiContextResolverTest`,
  `AiContextualAnalysisServiceTest`, etc.).
- Frontend: componentes com testes (`useAi.test.ts`, `ai.service.test.ts`,
  `AiAnalysisCard.test.tsx`, `AiActionProposalCard.test.tsx`, `AiChatAssistant.test.tsx`).
- Deploy: imagens publicadas via GHCR e validadas na VPS (smoke tests das rodadas de deploy);
  CI/CD GREEN no estado atual (`2a8c597`).

## Débitos conhecidos

- Sem memória de longo prazo entre conversas além do histórico persistido.
- Sugestões dependem da qualidade/contexto disponível; sem avaliação automática de acurácia.
