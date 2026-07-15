# Customers — Módulo de Clientes (Frontend)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Páginas](#páginas)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar as páginas e componentes do módulo de clientes no frontend.

## Descrição

O módulo de clientes exibe informações de clientes que foram convertidos de leads. Inclui perfil completo, histórico de compras e métricas.

## Páginas

### Lista de Clientes (`/customers`)

- DataTable com busca e filtros
- Colunas: Nome, Email, Phone, Empresa, Desde, Status
- Ações: Ver perfil, Exportar

### Detalhe do Cliente (`/customers/[id]`)

- Card com dados do cliente
- Histórico de interações
- Métricas (LTV, compras, etc.)
- Tags e segmentação

## Componentes

| Componente | Descrição |
|---|---|
| CustomerCard | Card de cliente na listagem |
| CustomerList | Lista com filtros |
| CustomerDetail | Página de detalhes |
| CustomerMetrics | Métricas do cliente |
| CustomerTimeline | Histórico de interações |
| CustomerTags | Tags do cliente |

## Responsabilidades

- Visualizar perfil completo do cliente
- Histórico de interações e compras
- Métricas de relacionamento
- Segmentação e tags

## Dependências

- [01-backend/Customers.md](../01-backend/Customers.md) — API de clientes
- [Tables.md](./Tables.md) — DataTable

## Regras

- Clientes são criados automaticamente (conversão de lead)
- Perfil do cliente nunca é deletado
- Histórico de interações é imutável
- Métricas são recalculadas periodicamente

## Futuras Melhorias

- Dashboard do cliente
- Score de satisfação
- Programa de fidelidade
- Export de dados do cliente

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
