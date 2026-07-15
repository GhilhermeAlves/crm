# Pipeline — Pipeline de Vendas

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de pipeline de vendas, incluindo gestão de oportunidades, movimentação entre estágios e conversão.

## Descrição

O pipeline representa o fluxo de vendas da empresa, desde a captção de um lead até o fechamento de uma oportunidade. Cada empresa pode ter múltiplos pipelines para diferentes produtos ou linhas de negócio.

## Responsabilidades

- Criar e gerenciar pipelines
- Gerenciar oportunidades dentro do pipeline
- Controlar movimentação entre estágios
- Rastrear histórico de mudanças
- Calcular métricas de vendas (win rate, ciclo médio, etc.)
- Calcular previsão de receita

## Fluxo

### Criação de Oportunidade

```
1. Lead é qualificado como hot/warm
        │
2. Oportunidade é criada no primeiro estágio
        │
3. Dados do lead são copiados para a oportunidade
        │
4. Evento OpportunityCreated é publicado
```

### Movimentação

```
1. Agente move oportunidade para próximo estágio
        │
2. Backend valida transição permitida
        │
3. Oportunidade é atualizada
        │
4. Evento OpportunityMoved é publicado
        │
5. Dashboard é atualizado
```

### Fechamento

```
1. Oportunidade chega ao estágio final
        │
2. Agente marca como WON ou LOST
        │
3. Se WON → Contact vira Customer
   Se LOST → Motivo da perda é registrado
4. Evento correspondente é publicado
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/pipelines` | Listar pipelines | `pipeline:read` |
| POST | `/api/v1/pipelines` | Criar pipeline | `pipeline:write` |
| GET | `/api/v1/pipelines/{id}` | Buscar pipeline | `pipeline:read` |
| PUT | `/api/v1/pipelines/{id}` | Atualizar pipeline | `pipeline:write` |
| GET | `/api/v1/pipelines/{id}/opportunities` | Oportunidades | `pipeline:read` |
| POST | `/api/v1/pipelines/{id}/opportunities` | Criar oportunidade | `pipeline:write` |
| PUT | `/api/v1/pipelines/opportunities/{id}` | Atualizar oportunidade | `pipeline:write` |
| POST | `/api/v1/pipelines/opportunities/{id}/move` | Mover oportunidade | `pipeline:write` |
| POST | `/api/v1/pipelines/opportunities/{id}/won` | Marcar como ganha | `pipeline:write` |
| POST | `/api/v1/pipelines/opportunities/{id}/lost` | Marcar como perdida | `pipeline:write` |
| GET | `/api/v1/pipelines/metrics` | Métricas do pipeline | `pipeline:read` |

## Dependências

- [Stages.md](./Stages.md) — Estágios do pipeline
- [Kanban.md](./Kanban.md) — Visualização kanban
- [Contacts.md](./Contacts.md) — Contato associado
- [Leads.md](./Leads.md) — Conversão lead → oportunidade
- [Customers.md](./Customers.md) — Conversão oportunidade → cliente

## Regras

- Pipeline deve ter pelo menos 2 estágios
- Oportunidade só pode ser movida para estágio seguinte/anterior
- Won/Lost só pode ser marcado no último estágio
- Valor da oportunidade é obrigatório
- Motivo da perda é obrigatório quando marked as LOST
- Histórico de movimentação é imutável
- Um lead pode gerar múltiplas oportunidades

## Futuras Melhorias

- Previsão de receita com IA
- Múltiplos pipelines com diferentes regras
- Pipeline de renovação/retenção
- Weighted pipeline por estágio
- Competição entre oportunidades
- Aprovações em estágios específicos

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
