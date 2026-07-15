# AI — Regras de IA

## Índice

- [Objetivo](#objetivo)
- [Regras](#regras)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar regras de uso de IA no CRM.

## Regras

| # | Regra | Justificativa |
|---|---|---|
| AI-001 | IA é opcional (config por empresa) | Flexibilidade |
| AI-002 | Rate limit: 100 chamadas/hora por empresa | Custo |
| AI-003 | Respostas sugeridas precisam de aprovação humana | Qualidade |
| AI-004 | Dados sensíveis não são enviados para OpenAI | Privacidade |
| AI-005 | Cache de respostas similares | Performance |
| AI-006 | Log de todas as chamadas (custo) | Auditoria |

## Responsabilidades

- Garantir uso responsável de IA
- Controlar custos
- Proteger dados dos clientes

## Dependências

- [01-backend/AI.md](../01-backend/AI.md) — Implementação
- [04-integrations/OpenAI.md](../04-integrations/OpenAI.md) — OpenAI API

## Futuras Melhorias

- Fine-tuning
- IA totalmente autônoma
- Predição de churn
- Voice AI

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
