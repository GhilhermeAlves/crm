# OpenAI — Integração OpenAI

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Configuração](#configuração)
- [Features](#features)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a integração com OpenAI API para funcionalidades de IA.

## Descrição

OpenAI é usada para chatbot, classificação de leads, análise de sentimento, geração de conteúdo e resumo de conversas.

## Responsabilidades

- Sugerir respostas automáticas
- Classificar leads
- Analisar sentimento de mensagens
- Gerar conteúdo para campanhas
- Resumir conversas longas

## Configuração

```properties
openai.api-key=${OPENAI_API_KEY}
openai.model=gpt-4
openai.max-tokens=1000
openai.temperature=0.7
```

## Features

| Feature | Modelo | Tokens | Uso |
|---|---|---|---|
| Chat Reply | GPT-4 | 1000 | Sugerir resposta |
| Classification | GPT-3.5 | 500 | Classificar lead |
| Sentiment | GPT-3.5 | 200 | Análise de sentimento |
| Summarize | GPT-4 | 2000 | Resumo de conversa |
| Content Gen | GPT-4 | 1500 | Gerar conteúdo |

## Dependências

- [01-backend/AI.md](../01-backend/AI.md) — Módulo de IA
- [01-backend/Messages.md](../01-backend/Messages.md) — Contexto de mensagens

## Regras

- Rate limit: 100 chamadas/hora por empresa
- Dados sensíveis não são enviados para OpenAI
- Respostas precisam de aprovação humana
- Cache de respostas similares
- Log de todas as chamadas (custo)
- Timeout: 30 segundos

## Futuras Melhorias

- Fine-tuning com dados da empresa
- Function calling para ações
- Embeddings para busca semântica
- DALL-E para geração de imagens
- Whisper para transcrição de áudio

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
