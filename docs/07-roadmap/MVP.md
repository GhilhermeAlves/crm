# MVP — Mínimo Produto Viável

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Features do MVP](#features-do-mvp)
- [Cronograma](#cronograma)
- [Critérios de Sucesso](#critérios-de-sucesso)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Definir o escopo do MVP (Mínimo Produto Viável) do CRM SaaS Omnichannel.

## Descrição

O MVP é a versão mais básica do produto que pode ser entregue a clientes iniciais para validação. Foco em WhatsApp-first CRM com funcionalidades essenciais.

## Features do MVP

### Must Have (P0)

| Feature | Descrição | Prioridade |
|---|---|---|
| Auth básico | Login/senha, JWT | P0 |
| Gestão de contatos | CRUD de contatos | P0 |
| Chat WhatsApp | Envio/recebimento de mensagens | P0 |
| Conversas | Lista e detalhe de conversas | P0 |
| Pipeline básico | Kanban com 3 estágios | P0 |
| Dashboard básico | KPIs principais | P0 |

### Should Have (P1)

| Feature | Descrição | Prioridade |
|---|---|---|
| Leads | Captação e qualificação | P1 |
| Templates | Templates de mensagem | P1 |
| Notificações | In-app notifications | P1 |
| Multi-tenant | Empresas separadas | P1 |

### Nice to Have (P2)

| Feature | Descrição | Prioridade |
|---|---|---|
| Campanhas | Envio em massa básico | P2 |
| Automações | Automações simples | P2 |
| Relatórios | Relatórios básicos | P2 |
| Integrações | Email, Google | P2 |

## Cronograma

| Fase | Duração | Entregáveis |
|---|---|---|
| Setup & Arquitetura | 2 semanas | Infraestrutura, CI/CD |
| Auth & Users | 2 semanas | Login, registro, profiles |
| Contacts & Leads | 2 semanas | CRUD, scoring básico |
| Chat & WhatsApp | 3 semanas | Envio/recebimento, webhooks |
| Pipeline & Kanban | 2 semanas | Pipeline, kanban |
| Dashboard & UI | 2 semanas | Dashboard, layout |
| Testes & QA | 1 semana | Testes, fixes |
| **Total** | **14 semanas** | **MVP pronto** |

## Critérios de Sucesso

| Critério | Target |
|---|---|
| Usuários beta | 10 empresas |
| Mensagens/dia | 1.000 |
| Uptime | 99% |
| NPS beta | > 40 |
| Bugs críticos | 0 |

## Responsabilidades

- Priorizar features
- Manter escopo focado
- Validar com clientes beta
- Iterar baseado em feedback

## Dependências

- [00-core/Vision.md](../00-core/Vision.md) — Visão
- [00-core/Architecture.md](../00-core/Architecture.md) — Arquitetura
- [04-integrations/WhatsApp.md](../04-integrations/WhatsApp.md) — WhatsApp

## Regras

- MVP não pode ter mais de 10 features P0
- Nenhum P0 pode ser removido
- P1 pode ser movido para v1.0
- P2 pode ser removido se necessário
- Beta testing com 5-10 empresas selecionadas

## Futuras Melhorias

- App mobile (React Native)
- IA integrada
- Multi-canal (email, SMS)
- API pública

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
