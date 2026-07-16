# Decision Tree — Árvore de Decisão para IA

## Objetivo

Guia passo a passo para determinar quais documentos um agente de IA deve ler para qualquer tarefa.

## Escopo

Qualquer solicitação de desenvolvimento ou manutenção no projeto.

## Como utilizar

Siga os passos na ordem apresentada. Cada passo leva ao próximo.

## Árvore de Decisão

### Passo 1 — Classificar a Solicitação

A solicitação é sobre:

- **Código** → Passo 2
- **Infraestrutura** → Passo 2F
- **Documentação** → Passo 2D
- **Banco de dados** → Passo 2B

---

### Passo 2 — É código backend ou frontend?

- **Backend** → Passo 3
- **Frontend** → Passo 3F
- **Ambos** → Passo 3 + Passo 3F

---

### Passo 3 — Backend: Qual módulo?

Consulte `AI_ROUTER.md` e localize o módulo na tabela.

| Se a tarefa é sobre... | Módulo |
|------------------------|--------|
| Login/auth/permissions | Auth, Permissions |
| Usuários | Users |
| Empresas/tenants | Company |
| Contatos | Contacts |
| Leads | Leads |
| Clientes | Customers |
| Pipeline/vendas | Pipeline, Stages |
| Kanban visual | Kanban |
| Conversas | Conversations |
| Chat tempo real | Chat |
| Mensagens | Messages |
| Templates | Templates |
| Campanhas | Campaigns |
| Automações | Automations |
| Webhooks | Webhooks |
| Notificações | Notifications |
| Dashboard/métricas | Dashboard |
| Relatórios | Reports |
| IA/ML | AI |
| Eventos | Events |
| Auditoria | Audit |
| Cache | Cache |
| Arquivos | File Storage |
| Agendamento | Scheduler |
| Permissões | Permissions |

**→ Passo 4**

---

### Passo 3F — Frontend: Qual módulo?

| Se a tarefa é sobre... | Módulo |
|------------------------|--------|
| Rotas/navegação | Frontend Architecture |
| Layouts | Frontend Architecture |
| Componentes | Frontend Architecture |
| Formulários | Frontend (Forms) |
| Kanban visual | Kanban |
| Chat interface | Chat |
| Dashboard | Dashboard |
| Relatórios | Reports |
| Notificações | Notifications |
| Permissões | Permissions |

**→ Passo 4**

---

### Passo 3B — Database: Qual tópico?

| Se a tarefa é sobre... | Documento |
|------------------------|-----------|
| Nova tabela/entidade | `docs/03-database/Entities.md`, `docs/03-database/ERD.md` |
| Migration | `docs/03-database/Migrations.md` |
| Índice/performance | `docs/03-database/Indexes.md`, `docs/03-database/Performance.md` |
| Relacionamentos | `docs/03-database/Relationships.md` |
| Soft delete | `docs/03-database/SoftDelete.md` |
| UUID | `docs/03-database/UUID.md` |
| Backup | `docs/03-database/Backup.md` |
| Configuração geral | `docs/03-database/Overview.md` |

**→ Passo 4**

---

### Passo 2F — Infraestrutura: Qual área?

| Se a tarefa é sobre... | Documento |
|------------------------|-----------|
| Docker/container | `docs/06-devops/Docker.md` |
| CI pipeline | `docs/06-devops/CI.md` |
| Deploy/CD | `docs/06-devops/CD.md` |
| Kubernetes | `docs/06-devops/Kubernetes.md` |
| Monitoramento | `docs/06-devops/Monitoring.md` |
| Integração externa | `docs/04-integrations/[Servico].md` |
| Arquitetura geral | `docs/00-core/Architecture.md` |

**→ Passo 4**

---

### Passo 2D — Documentação: Qual área?

| Se a tarefa é sobre... | Documento |
|------------------------|-----------|
| Criar/atualizar doc | `docs/00-core/` (convenções) |
| Regra de negócio | `docs/05-business-rules/[Modulo].md` |
| Roadmap | `docs/07-roadmap/` |

**→ Passo 4**

---

### Passo 4 — Verificar Dependências

1. Consulte `DEPENDENCIES_INDEX.md`
2. Identifique módulos impactados
3. Adicione documentos de módulos afetados à lista

---

### Passo 5 — Montar Lista Final de Documentos

Monte a lista mínima de documentos necessários. Exemplo típico:

```
1. AI_ROUTER.md (sempre primeiro)
2. docs/01-backend/[Modulo].md
3. docs/05-business-rules/[Modulo].md (se houver regras)
4. docs/02-frontend/[Modulo].md (se envolver UI)
5. docs/03-database/... (se envolver schema)
```

---

### Passo 6 — Implementar

1. Leia apenas os documentos da lista
2. Siga `IMPLEMENTATION_GUIDE.md`
3. Após implementar, siga `CHANGE_POLICY.md`

---

## Fluxo Rápido

```
Solicitação
  → Passo 1 (classificar)
  → Passo 2/3 (módulo)
  → Passo 4 (dependências)
  → Passo 5 (lista de docs)
  → Passo 6 (implementar)
  → CHANGE_POLICY.md
```

## Referências

- Roteador: [AI_ROUTER.md](AI_ROUTER.md)
- Dependências: [DEPENDENCIES_INDEX.md](DEPENDENCIES_INDEX.md)
- Implementação: [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- Mudanças: [CHANGE_POLICY.md](CHANGE_POLICY.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
