# Customers — Gestão de Clientes

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

Documentar o módulo de gestão de clientes, incluindo conversão de lead, perfil do cliente e histórico de relacionamento.

## Descrição

Clientes são contatos que converteram — realizaram uma compra ou assinaram um contrato. O módulo mantém o perfil completo do cliente, histórico de interações e status de relacionamento.

## Responsabilidades

- Converter lead em cliente (junto com Pipeline)
- Manter perfil completo do cliente
- Rastrear histórico de compras e interações
- Gerenciar ciclo de vida do cliente (onboarding, ativo, churned)
- Calcular métricas de cliente (LTV, CAC, churn rate)

## Fluxo

### Conversão Lead → Cliente

```
1. Oportunidade no pipeline é marcada como WON
        │
2. Lead é convertido em Customer
        │
3. Customer é criado com dados do Lead
        │
4. Evento CustomerCreated é publicado
        │
5. Fluxo de onboarding é iniciado
```

### Ciclo de Vida

```
Lead → Customer (ativo) → Customer (premium) → Customer (churned)
                                    │
                                    └── Retenção via automação
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/customers` | Listar clientes | `customer:read` |
| GET | `/api/v1/customers/{id}` | Buscar cliente | `customer:read` |
| PUT | `/api/v1/customers/{id}` | Atualizar cliente | `customer:write` |
| GET | `/api/v1/customers/{id}/history` | Histórico | `customer:read` |
| GET | `/api/v1/customers/{id}/metrics` | Métricas | `customer:read` |
| POST | `/api/v1/customers/{id}/tags` | Adicionar tag | `customer:write` |

## Dependências

- [Contacts.md](./Contacts.md) — Dados base do contato
- [Pipeline.md](./Pipeline.md) — Conversão vem do pipeline
- [Conversations.md](./Conversations.md) — Histórico de comunicação
- [Analytics.md](./Reports.md) — Métricas de cliente

## Regras

- Cliente é criado automaticamente quando oportunidade é WON
- Dados do lead são migrados para o cliente
- Cliente não pode ser deletado se tem fatura ativa
- Churn rate é calculado mensalmente
- LTV é recalculado a cada interação

## Futuras Melhorias

- Gestão de contratos e assinaturas
- Programa de fidelidade
- NPS e CSAT integrados
- Predição de churn com IA
- Customer health score
- Gestão de account managers

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
