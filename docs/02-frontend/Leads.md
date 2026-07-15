# Leads — Módulo de Leads (Frontend)

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

Documentar as páginas e componentes do módulo de leads no frontend.

## Descrição

O módulo de leads permite visualizar, criar, editar, qualificar e converter leads. Inclui listagem com filtros, detalhes do lead e ações de qualificação.

## Páginas

### Lista de Leads (`/leads`)

- DataTable com busca, filtros e paginação
- Colunas: Nome, Email, Phone, Origem, Score, Status, Data
- Ações: Criar, Editar, Qualificar, Converter, Deletar
- Filtros: Status, Origem, Score, Responsável, Período

### Detalhe do Lead (`/leads/[id]`)

- Card com dados do lead
- Timeline de atividades
- Ações: Qualificar, Converter, Atribuir, Deletar
- Sidebar com informações de contato
- Histórico de mensagens

## Componentes

| Componente | Descrição |
|---|---|
| LeadCard | Card de lead na listagem |
| LeadForm | Formulário de criação/edição |
| LeadList | Lista de leads com filtros |
| LeadDetail | Página de detalhes |
| LeadFilter | Filtros avançados |
| LeadScore | Badge com score |
| LeadTimeline | Timeline de atividades |
| LeadQualification | Modal de qualificação |
| LeadConvert | Modal de conversão |

## Responsabilidades

- CRUD completo de leads
- Qualificação com score visual
- Conversão para oportunidade
- Importação de leads
- Filtros avançados e busca

## Dependências

- [01-backend/Leads.md](../01-backend/Leads.md) — API de leads
- [Tables.md](./Tables.md) — DataTable
- [Forms.md](./Forms.md) — Formulários

## Regras

- Score é exibido como badge colorido
- Status é exibido com ícone e cor
- Lead CONVERTED não pode ser editado
- Lead LOST pode ser reaberto
- Importação suporta CSV e Excel

## Futuras Melhorias

- Kanban view para leads
- Importação via drag-and-drop
- Merge de leads duplicados
- IA para sugestão de qualificação
- Filtros salvos pelo usuário

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
