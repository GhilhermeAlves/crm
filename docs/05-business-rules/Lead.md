# Lead — Regras de Leads

## Índice

- [Objetivo](#objetivo)
- [Regras de Criação](#regras-de-criação)
- [Regras de Qualificação](#regras-de-qualificação)
- [Regras de Conversão](#regras-de-conversão)
- [Regras de Distribuição](#regras-de-distribuição)
- [Regras de Scoring](#regras-de-scoring)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todas as regras de negócio relacionadas a leads.

## Regras de Criação

| # | Regra | Justificativa |
|---|---|---|
| L-001 | Lead deve ter pelo menos email OU phone | Contato é essencial |
| L-002 | Origem é obrigatória | Rastreabilidade |
| L-003 | Email deve ser único por empresa | Prevenir duplicatas |
| L-004 | Lead pode ser criado via API, WhatsApp, formulário ou importação | Múltiplas fontes |
| L-005 | Lead duplicado (mesmo email) é mergeado automaticamente | Consistência |

## Regras de Qualificação

| # | Regra | Justificativa |
|---|---|---|
| L-010 | Lead pode ser qualificado como: Hot, Warm, Cold, Disqualified | Segmentação |
| L-011 | Apenas agente atribuído pode qualificar | Controle |
| L-012 | Lead disqualified pode ser reaberto (volta para NEW) | Flexibilidade |
| L-013 | Lead qualified gera oportunidade no pipeline | Fluxo de vendas |
| L-014 | Qualificação é registrada no histórico | Auditoria |

## Regras de Conversão

| # | Regra | Justificativa |
|---|---|---|
| L-020 | Lead converte em oportunidade quando qualificado como Hot/Warm | Fluxo |
| L-021 | Conversão cria oportunidade no primeiro estágio do pipeline | Início do funil |
| L-022 | Dados do lead são copiados para a oportunidade | Consistência |
| L-023 | Lead convertido não pode ser reconvertido | Prevenir duplicatas |
| L-024 | Conversão pode ser manual (agente) ou automática (automação) | Flexibilidade |

## Regras de Distribuição

| # | Regra | Justificativa |
|---|---|---|
| L-030 | Leads são distribuídos round-robin entre agentes disponíveis | Equilíbrio |
| L-031 | Distribuição respeita horário de trabalho do agente | Performance |
| L-032 | Lead WhatsApp é atribuído imediatamente | Prioridade |
| L-033 | Máximo de leads ativos por agente: definido pela empresa | Capacidade |
| L-034 | Lead não atribuído fica na fila por 24h antes de reatribuir | SLA |

## Regras de Scoring

| # | Regra | Justificativa |
|---|---|---|
| L-040 | Score é calculado baseado em: origem, engajamento, dados, tempo, perfil | Qualificação |
| L-041 | Score é recalculado a cada interação | Dinâmico |
| L-042 | Faixas: 80-100 Hot, 50-79 Warm, 20-49 Cold, 0-19 Disqualified | Classificação |
| L-043 | Score é visível para o agente | Transparência |

### Fórmula de Cálculo

```
Score Total = Score Origem + Score Engajamento + Score Dados + Score Perfil

Score Origem (0-25 pontos):
  - WhatsApp:     25 pontos (canal primário, alta intenção)
  - Formulário:   20 pontos (procura ativa)
  - API:          15 pontos (integração automática)
  - Importação:   10 pontos (dados históricos)
  - Manual:        5 pontos (cadastro interno)

Score Engajamento (0-30 pontos):
  - Respondeu mensagem:        +10 pontos
  - Abriu mensagem:            +5 pontos
  - Clicou em link:            +8 pontos
  - Enviou múltiplas mensagens: +7 pontos (3+ mensagens)
  - Última interação < 24h:    +5 pontos
  - Última interação < 7d:     +3 pontos
  - Sem interação > 30d:       -10 pontos

Score Dados (0-25 pontos):
  - Email válido:        +8 pontos
  - Telefone válido:     +7 pontos
  - Nome completo:       +5 pontos
  - Empresa preenchida:  +3 pontos
  - Cargo preenchido:    +2 pontos

Score Perfil (0-20 pontos):
  - Cargo C-level/VP:    +20 pontos (decisor)
  - Cargo Diretor:       +15 pontos
  - Cargo Gerente:       +10 pontos
  - Cargo Analista:      +5 pontos
  - Cargo não informado: 0 pontos
```

### Exemplos de Cálculo

| Cenário | Origem | Engajamento | Dados | Perfil | Total | Classificação |
|---|---|---|---|---|---|---|
| Lead WhatsApp, respondeu, dados completos, CEO | 25 | 15 | 25 | 20 | **85** | Hot |
| Lead formulário, abriu email, dados parciais, Gerente | 20 | 5 | 15 | 10 | **50** | Warm |
| Lead importação, sem interação, dados mínimos | 10 | -10 | 8 | 0 | **8** | Cold |

## Responsabilidades

- Garantir que regras são implementadas corretamente
- Atualizar regras quando necessidades de negócio mudam
- Documentar exceções

## Dependências

- [01-backend/Leads.md](../01-backend/Leads.md) — Implementação
- [01-backend/Pipeline.md](../01-backend/Pipeline.md) — Pipeline
- [00-core/Decisions.md](../00-core/Decisions.md) — Decisões

## Futuras Melhorias

- IA para scoring preditivo
- Automação de qualificação
- A/B testing de regras de distribuição
- Regras customizáveis por empresa

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
