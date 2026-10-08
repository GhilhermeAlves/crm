# Arquitetura do Agente de IA (WhatsApp)

> V088 · out/2026. Referência para evoluir o agente sem voltar ao "prompt gigante".

**Princípio:** o prompt diz quem o agente é e como ele se comporta. O CRM fornece
os dados reais. A memória traz o que foi relevante em interações anteriores, o
conhecimento traz informação consultável e as ferramentas executam ações. O
`AgentContextBuilder` junta tudo no momento de cada resposta.

## 1. Agente: configuração permanente

Um agente por empresa, em `agent_config` (RLS FORCE). Domínio: `automation.ai.domain.AgentConfig`.

| Camada | Conteúdo | Persistência |
|---|---|---|
| **Identidade** (`AgentIdentity`) | nome, descrição, persona | colunas `agent_name`, `agent_description`, `persona` |
| **Comportamento** (`AgentBehavior`) | objetivo, tom de voz, regras[], instruções[] | `objective`, `tone`, `rules` JSONB, `instructions` JSONB |
| **Modelo** | model, temperature, maxTokens (provider é global: `app.ai.provider`) | colunas V071 |
| **Conversação** | cooldown, limite de caracteres, voz, memória ligada/desligada | `cooldown_minutes`, `max_chars`, `voice_reply_mode`, `memory_enabled` |
| **Ferramentas** | agenda (automática), transferência humana (opt-in), memória | `human_transfer_enabled` + disponibilidade em runtime |
| **Conhecimento** | porta `KnowledgeRetriever`, hoje sem fontes (`NoKnowledgeRetriever`) | ainda sem tabela |
| **Prompt legado** | `system_prompt` | só usado enquanto Identidade e Comportamento estão vazios |

Regras e instruções ficam em JSONB (listas pequenas e ordenadas, sem ciclo de vida
próprio). A memória, que tem ciclo de vida, escopo por contato e recuperação, ganhou
tabela própria.

## 2. O que NÃO vai no prompt

Dados da clínica, dados do paciente, agenda, histórico, memórias e ferramentas.
Tudo isso é dado recuperável, montado em cada execução:

| Conceito | O que é | Fonte |
|---|---|---|
| `ClinicContext` | nome, telefone, endereço, horário, fuso, "agora" | `companies` + `company_settings` (`ClinicContextProvider`) |
| `PatientContext` | contato do CRM, nome do perfil, próximos agendamentos | `contacts` + `appointments` (`PatientContextProvider`) |
| `ConversationHistory` | sequência temporal de mensagens recentes (janela de 20, orçamento de tokens) | `omnichannel_messages` |
| `MemoryContext` | memórias relevantes do contato (até 5) | `agent_memory` (`MemoryRetriever`) |
| `KnowledgeContext` | trechos de documentos/FAQ relevantes | `KnowledgeRetriever` (vazio por ora) |
| `ToolContext` | definições estruturadas e regras de uso das ferramentas disponíveis | `AgentToolbox` |

**O CRM é a fonte da verdade.** Agendamento e status vêm da agenda, nunca da
memória. A `MemoryWritePolicy` rejeita memórias com cara de agendamento.

## 3. Runtime

```
WhatsApp (fila crm.whatsapp.auto-ai)
  → WhatsAppInboundAutoReplyProcessor   (safe defaults, cooldown, debounce, reserva idempotente)
  → AgentRuntimeInput                   (empresa/conversa/contato/telefone + histórico recente)
  → AgentContextBuilder.build()         (clínica, paciente, memórias, conhecimento, ferramentas)
  → AgentContextRenderer.render()       (ÚNICO lugar que vira mensagens do provider)
  → AiChatFailover (LLM) ⇄ ToolContext.execute()   (até 4 rodadas de ferramentas)
  → resposta → OUTBOUND pendente → fila de envio
```

Mensagens enviadas ao provider, nesta ordem:

1. `system`: Identidade + Comportamento, ou o prompt legado sem nenhuma alteração.
2. `system`: contexto dinâmico em seções (AGORA, CLÍNICA, PACIENTE, MEMÓRIAS RELEVANTES, CONHECIMENTO, CANAL).
3. `system`: regras de uso das ferramentas disponíveis. As definições vão no campo `tools` da API, não como texto.
4. histórico como mensagens `user`/`assistant`.
5. mensagem atual (`user`).

Fontes opcionais que falham (memória, conhecimento, CRM) degradam só a sua seção.
A resposta continua sendo gerada.

## 4. Ferramentas

Contrato `AgentToolProvider`: `isAvailable`, `definitions`, `guidance` e `execute`.
O escopo vem sempre da `AgentToolSession` (empresa, conversa, contato) e nunca dos
argumentos do modelo.

| Provider | Ferramentas | Disponível quando |
|---|---|---|
| `SchedulingToolProvider` | `consultar_horarios_livres`, `agendar_consulta` | há tipo de consulta ativo com profissional |
| `HumanTransferToolProvider` | `transferir_para_humano` | `human_transfer_enabled` (coloca a conversa em modo humano) |
| `MemoryToolProvider` | `registrar_memoria` | `memory_enabled` e contato identificado no CRM |

Para adicionar uma ferramenta, basta criar um `@Component` que implemente
`AgentToolProvider`. O `AgentToolbox` descobre o bean sozinho. Só crie ferramentas
para funcionalidades que o CRM realmente possui.

## 5. Memória

`agent_memory`: `company_id` (RLS FORCE), `agent_config_id`, `contact_id`, `memory_type`
(PREFERENCE | FACT | PROFILE | CONTEXTUAL), `content` (≤500), `importance` (1–5),
`source` (AGENT | USER), `source_message_id`, `metadata` JSONB, `expires_at`.

- **Não é histórico:** mensagens continuam em `omnichannel_messages`.
- **Seleção:** nada é salvo automaticamente. O modelo propõe via `registrar_memoria`
  e a `MemoryWritePolicy` decide: rejeita cumprimentos, dados de agendamento/status
  e documentos pessoais. Conteúdo equivalente é deduplicado.
- **Recuperação:** `MemoryRetriever.retrieveRelevant(MemoryQuery)`. A implementação
  atual (`RankedMemoryRetriever`) ordena por importância e recência, ignora memórias
  expiradas e tem limite. `MemoryQuery` já leva a mensagem atual, de modo que uma
  implementação semântica (embeddings/vector) pode substituí-la sem mudar o código
  que a chama.
- **Isolamento:** RLS por empresa, filtro explícito por empresa + contato no
  repositório e verificação `belongsTo` no domínio. Sem contato identificado não há
  memória. Teste: `AgentMemoryIsolationIT`.
- **Gestão pela equipe** (inclusive LGPD): `GET/POST/PUT/DELETE /api/v1/ai/agent-memories`
  (`ai:agent-config`).

## 6. Migração do prompt legado

- A V088 só adiciona colunas: nenhum agente muda de comportamento no deploy.
- Não existe parsing automático. Na tela, o botão "Copiar para a Persona" leva o
  texto legado para a Persona, e o usuário separa as regras e instruções.
- Assim que Identidade ou Comportamento é preenchido, o legado deixa de ser usado.
  Ele continua guardado até alguém apagar.
- A API mantém os campos planos antigos (`systemPrompt`, `model`, `cooldownMinutes`...)
  e acrescenta os blocos `identity`, `behavior`, `modelConfig`, `conversation.memory`,
  `tools` e `usesLegacyPrompt`.

## 7. Próximos passos (não implementados de propósito)

Base de conhecimento (documentos/FAQ + RAG), busca semântica de memórias, planner e
multi-agente. A arquitetura acima já tem os pontos de extensão (`KnowledgeRetriever`,
`MemoryRetriever`, `AgentToolProvider`), então nada disso exige reescrever o runtime.
