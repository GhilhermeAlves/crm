# Campaign — Regras de Campanhas

## Índice

- [Objetivo](#objetivo)
- [Regras de Criação](#regras-de-criação)
- [Regras de Disparo](#regras-de-disparo)
- [Regras de Compliance](#regras-de-compliance)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar regras de negócio de campanhas de marketing.

## Regras de Criação

| # | Regra | Justificativa |
|---|---|---|
| C-001 | Campanha precisa de pelo menos 1 template | Mensagem obrigatória |
| C-002 | Template deve estar aprovado (WhatsApp) | Compliance |
| C-003 | Segmento é obrigatório | Público-alvo |
| C-004 | Campanha pode ser multicanal | Flexibilidade |

## Regras de Disparo

| # | Regra | Justificativa |
|---|---|---|
| C-010 | Máximo 100 msgs/minuto (configurável) | Rate limit |
| C-011 | Contatos opt-out são excluídos | LGPD |
| C-012 | Campanha pode ser pausada e retomada | Controle |
| C-013 | Horário de envio: 8h-22h (horário do contato) | Respeito |
| C-014 | Dom/Feridos: sem envio (a menos que configurado) | Respeito |

## Regras de Compliance

| # | Regra | Justificativa |
|---|---|---|
| C-020 | Opt-out é obrigatório em toda mensagem | LGPD |
| C-021 | Consentimento deve ser registrado | LGPD |
| C-022 | Dados de contato podem ser solicitados | LGPD |
| C-023 | Histórico de campanhas é mantido por 5 anos | Compliance |

## Responsabilidades

- Garantir compliance com LGPD
- Respeitar rate limits dos canais
- Rastrear métricas de campanha

## Dependências

- [01-backend/Campaigns.md](../01-backend/Campaigns.md) — Implementação
- [04-integrations/WhatsApp.md](../04-integrations/WhatsApp.md) — WhatsApp limits
- [04-integrations/Email.md](../04-integrations/Email.md) — Email limits

## Futuras Melhorias

- A/B testing automático
- IA para melhor horário
- Segmentação comportamental
- Multi-canal com orçamento

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
