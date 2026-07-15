# Customer — Regras de Customers

## Índice

- [Objetivo](#objetivo)
- [Regras de Criação](#regras-de-criação)
- [Regras de Ciclo de Vida](#regras-de-ciclo-de-vida)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar regras de negócio de Customers.

## Regras de Criação

| # | Regra | Justificativa |
|---|---|---|
| CU-001 | Customer é criado quando oportunidade é WON | Conversão |
| CU-002 | Dados do Lead são migrados para Customer | Consistência |
| CU-003 | Customer não pode ser deletado se tem fatura ativa | Integridade |
| CU-004 | Email deve ser único por Company | Prevenir duplicatas |

## Regras de Ciclo de Vida

| # | Regra | Justificativa |
|---|---|---|
| CU-010 | Status: Active, Premium, Churned | Classificação |
| CU-011 | Churned = sem interação por 90 dias | Definição |
| CU-012 | LTV é recalculado a cada interação | Métrica |
| CU-013 | Churn rate é calculado mensalmente | KPI |

## Responsabilidades

- Criar Customer automaticamente na conversão
- Manter perfil completo
- Calcular métricas de relacionamento

## Dependências

- [01-backend/Customers.md](../01-backend/Customers.md) — Implementação
- [01-backend/Pipeline.md](../01-backend/Pipeline.md) — Conversão

## Futuras Melhorias

- Predição de churn com IA
- Customer health score
- Programa de fidelidade
- Gestão de contratos

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
