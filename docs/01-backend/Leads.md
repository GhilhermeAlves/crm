# Leads — Gestão de Leads

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Lifecycle do Lead](#lifecycle-do-lead)
- [Scoring](#scoring)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de gestão de leads, incluindo captação, qualificação, scoring e distribuição.

## Descrição

Leads representam potenciais clientes identificados pela empresa. O módulo gerencia todo o ciclo de vida do lead, desde a captação até a qualificação e conversão em oportunidade no pipeline.

## Responsabilidades

- Captar leads de múltiplas fontes (WhatsApp, formulário, API, importação)
- Qualificar leads com base em critérios configuráveis
- Calcular score de lead automaticamente
- Distribuir leads entre agentes
- Converter lead em oportunidade no pipeline
- Rastrear origem e histórico do lead

## Fluxo

### Captação

```
1. Lead chega por canal (WhatsApp, formulário, API)
        │
2. Backend cria/identifica contato existente
        │
3. Lead é criado com origem e fonte
        │
4. Score inicial é calculado
        │
5. Lead entra na fila de distribuição
```

### Qualificação

```
1. Agente recebe lead
        │
2. Agente qualifica (hot/warm/cold/disqualified)
        │
3. Se hot/warm → Move para pipeline como oportunidade
   Se cold → Mantém para nurturamento
   Se disqualified → Marca como perdido
```

### Distribuição

```
1. Lead entra na fila
        │
2. Backend verifica regras de distribuição
        │
3. Lead é atribuído ao agente disponível
        │
4. Agente recebe notificação
```

## Lifecycle do Lead

```
New → Contacted → Qualified → Converted/Lost
 │         │          │              │
 │         │          │              ├── Oportunidade criada
 │         │          │              └── Lead descartado
 │         │          │
 │         │          ├── Hot (pronto para venda)
 │         │          ├── Warm (precisa nurturing)
 │         │          └── Cold (interesse baixo)
 │         │
 │         └── Primeiro contato realizado
 │
 └── Lead recém-chegado
```

### Status

| Status | Descrição |
|---|---|
| NEW | Lead recém-chegado, aguardando contato |
| CONTACTED | Primeiro contato realizado |
| QUALIFIED | Lead qualificado pelo agente |
| UNQUALIFIED | Lead não qualificado |
| CONVERTED | Convertido em oportunidade |
| LOST | Lead descartado |

## Scoring

### Critérios

| Critério | Peso | Exemplo |
|---|---|---|
| Origem | 20% | WhatsApp = 20, Formulário = 10, API = 5 |
| Engajamento | 30% | Mensagens enviadas, tempo de resposta |
| Dados completos | 20% | Email + phone + empresa preenchidos |
| Tempo de resposta | 15% | Respondeu em < 1h = alto, > 24h = baixo |
| Perfil | 15% | Tamanho da empresa, cargo, localização |

### Faixas de Score

| Score | Classificação | Ação |
|---|---|---|
| 80-100 | Hot | Prioridade máxima, distribuir imediatamente |
| 50-79 | Warm | Distribuir normalmente |
| 20-49 | Cold | Nurturamento automático |
| 0-19 | Disqualified | Não qualificado |

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/leads` | Listar leads | `lead:read` |
| GET | `/api/v1/leads/{id}` | Buscar lead | `lead:read` |
| POST | `/api/v1/leads` | Criar lead | `lead:write` |
| PUT | `/api/v1/leads/{id}` | Atualizar lead | `lead:write` |
| DELETE | `/api/v1/leads/{id}` | Deletar lead | `lead:delete` |
| POST | `/api/v1/leads/{id}/qualify` | Qualificar lead | `lead:write` |
| POST | `/api/v1/leads/{id}/convert` | Converter lead | `lead:write` |
| POST | `/api/v1/leads/{id}/assign` | Atribuir lead | `lead:assign` |
| POST | `/api/v1/leads/import` | Importar leads | `lead:import` |
| GET | `/api/v1/leads/origin` | Leads por origem | `lead:read` |
| GET | `/api/v1/leads/scoring` | Score dos leads | `lead:read` |

## Dependências

- [Contacts.md](./Contacts.md) — Lead é um contato qualificado
- [Pipeline.md](./Pipeline.md) — Conversão cria oportunidade
- [Campaigns.md](./Campaigns.md) — Captação via campanhas
- [04-integrations/WhatsApp.md](../04-integrations/WhatsApp.md) — Captação via WhatsApp
- [05-business-rules/Lead.md](../05-business-rules/Lead.md) — Regras de negócio

## Regras

- Lead deve ter pelo menos um contato (email ou phone)
- Origem é obrigatória (rastreabilidade)
- Lead converted não pode ser reaberto
- Lead lost pode ser reaberto (muda status para NEW)
- Distribuição respeita horário de trabalho do agente
- Máximo de leads ativos por agente: definido pela empresa
- Score é recalculado a cada interação
- Leads duplicados (mesmo email/phone) sãomergeados automaticamente

## Futuras Melhorias

- IA para scoring preditivo
- Automação de qualificação com chatbot
- Lead scoring baseado em comportamento (web scraping)
- Distribuição round-robin com pesos
- Integração com plataformas de geração de leads
- A/B testing de origens de lead
- Funil de nutruramento automatizado

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
