# AI — Inteligência Artificial

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Features](#features)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar as funcionalidades de Inteligência Artificial integradas ao CRM.

## Descrição

O CRM integra IA via OpenAI API para automatizar tarefas, sugerir respostas, classificar leads e gerar conteúdo. A IA é opcional e pode ser habilitada/desabilitada por empresa.

## Responsabilidades

- Sugerir respostas automáticas no chat
- Classificar e qualificar leads
- Gerar conteúdo para campanhas
- Analisar sentimento de mensagens
- Resumir conversas longas
- Extrair informações de documentos

## Features

### Chatbot IA

```
1. Mensagem recebida do contato
        │
2. IA analisa intenção e contexto
        │
3. IA gera resposta sugerida
        │
4. Agente revisa e aprova (ou edita)
        │
5. Resposta é enviada
```

### Lead Scoring com IA

```
1. Lead é criado/atualizado
        │
2. IA analisa dados do lead
        │
3. IA calcula score preditivo
        │
4. Score é atualizado no lead
        │
5. Ação automática baseada no score
```

### Sentiment Analysis

```
1. Mensagem recebida
        │
2. IA analisa sentimento
        │
3. Sentimento é classificado (positive/negative/neutral)
        │
4. Dashboard é atualizado
        │
5. Alerta se sentimento negativo
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| POST | `/api/v1/ai/suggest-reply` | Sugerir resposta | `ai:use` |
| POST | `/api/v1/ai/classify-lead` | Classificar lead | `ai:use` |
| POST | `/api/v1/ai/sentiment` | Análise de sentimento | `ai:use` |
| POST | `/api/v1/ai/summarize` | Resumir conversa | `ai:use` |
| POST | `/api/v1/ai/generate-content` | Gerar conteúdo | `ai:use` |
| GET | `/api/v1/ai/usage` | Uso de IA | `ai:read` |

## Dependências

- [04-integrations/OpenAI.md](../04-integrations/OpenAI.md) — OpenAI API
- [Conversations.md](./Conversations.md) — Contexto de conversas
- [Companies.md](./Companies.md) — Configuração por empresa

## Regras

- IA é opcional (configuração por empresa)
- Custo de IA é rate-limited por empresa
- Máximo de 100 chamadas de IA por hora por empresa
- Respostas sugeridas precisam de aprovação humana
- Dados enviados para OpenAI não devem conter dados sensíveis
- Cache de respostas similares (evitar chamadas duplicadas)
- Log de todas as chamadas de IA (para auditoria de custo)

## Futuras Melhorias

- Fine-tuning com dados da empresa
- IA multilíngue
- Chatbot totalmente autônomo
- IA para qualificação automática
- Análise preditiva de churn
- IA para roteamento de conversas
- Voice AI para chamadas

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
