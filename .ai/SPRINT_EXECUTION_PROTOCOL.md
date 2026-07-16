# Protocolo de Execução de Sprints

**Autoridade Máxima:** Este arquivo define o fluxo obrigatório de desenvolvimento.
**Versão:** 1.0.0
**Última Atualização:** 2026-07-15

---

## 1. Regra de Ouro

> **NENHUMA ETAPA PODE SER IGNORADA.**
> Toda Sprint DEVE seguir rigorosamente este protocolo.

---

## 2. Ordem Obrigatória de Leitura

### Fase 1: Memória do Projeto
```
1. .ai/LAST_SESSION.md          ← PRIMEIRO (sempre)
2. .ai/CURRENT_SPRINT.md        ← Segundo
3. .ai/CURRENT_MODULE.md        ← Terceiro
4. .ai/IMPLEMENTATION_QUEUE.md  ← Quarto
```

### Fase 2: Knowledge Layer
```
5. docs-ai/AI_ROUTER.md         ← Quinto (roteador)
6. contexts/[modulo].context.md ← Sexto (contexto do módulo)
7. playbooks/implement-[modulo].md ← Sétimo (playbook)
```

### Fase 3: Documentação Oficial
```
8. docs/[caminho-indicado].md   ← Oitavo (apenas os indicados)
```

### ⛔ PROIBIDO
- Ler toda a documentação do projeto
- Pular etapas da Fase 1
- Implementar antes de completar todas as leituras

---

## 3. Ordem de Implementação

### Para Sprints Backend

```
1. Domain Layer
   ├── Entities
   ├── Value Objects
   ├── Domain Events
   └── Domain Exceptions

2. Application Layer
   ├── Ports (Input/Output)
   ├── Services
   └── DTOs

3. Infrastructure Layer
   ├── Persistence (JPA Entities, Repositories)
   ├── Security (JWT, Filters)
   └── Mappers (MapStruct)

4. Presentation Layer
   ├── REST Controllers
   └── Request/Response DTOs

5. Database
   └── Flyway Migrations
```

### Para Sprints Frontend

```
1. Types
   └── TypeScript interfaces

2. Lib
   └── Validations (Zod)

3. Hooks
   └── React Query hooks

4. Components
   └── UI components

5. Pages
   └── App Router pages
```

### Para Sprints de Testes

```
1. Unit Tests
   ├── Domain
   ├── Application
   └── Infrastructure

2. Integration Tests
   ├── Repository
   └── Service

3. E2E Tests (se aplicável)
```

---

## 4. Ordem de Atualização da Documentação

### Após cada tarefa:
```
1. .ai/CURRENT_TASK.md    ← Status da tarefa
2. .ai/WORKLOG.md         ← Diário de trabalho
3. .ai/LAST_SESSION.md    ← Última sessão
```

### Após cada Sprint:
```
4. docs/CHANGELOG.md              ← Changelog do projeto
5. backend/IMPLEMENTATION_REPORT.md  ← Relatório backend
6. frontend/IMPLEMENTATION_REPORT.md ← Relatório frontend
7. .ai/CURRENT_SPRINT.md        ← Sprint atual
8. .ai/PROJECT_STATUS.md        ← Status do projeto
9. .ai/PROJECT_TIMELINE.md      ← Linha do tempo
10. .ai/IMPLEMENTATION_QUEUE.md ← Fila de implementação
```

### Ao encontrar problema:
```
11. .ai/KNOWN_ISSUES.md   ← Problemas conhecidos
12. .ai/BLOCKERS.md       ← Bloqueios e riscos
```

### Ao tomar decisão:
```
13. .ai/CURRENT_DECISIONS.md ← Decisões de arquitetura
```

---

## 5. Critérios de Conclusão

### Para considerar uma Sprint CONCLUÍDA:

#### Backend
- [ ] Domain entities criadas com value objects
- [ ] Application services com casos de uso
- [ ] Infrastructure implementations (repositories, mappers)
- [ ] REST controllers com DTOs
- [ ] Migration Flyway criada e testada
- [ ] Testes unitários (≥80% coverage)
- [ ] Testes de integração (≥60% coverage)
- [ ] Lint sem erros (`mvn compile`)
- [ ] Swagger documentado

#### Frontend
- [ ] Components criados (≤200 linhas cada)
- [ ] Hooks criados (useQuery/useMutation)
- [ ] Pages criadas com layouts
- [ ] Types definidos
- [ ] Validações Zod implementadas
- [ ] Lint sem erros (`npm run lint`)
- [ ] TypeScript sem erros (`npm run typecheck`)

#### Documentação
- [ ] `docs/CHANGELOG.md` atualizado
- [ ] `IMPLEMENTATION_REPORT.md` atualizado
- [ ] `.ai/LAST_SESSION.md` atualizado
- [ ] `.ai/WORKLOG.md` atualizado
- [ ] `.ai/CURRENT_TASK.md` atualizado

#### Qualidade
- [ ] Código segue Clean Architecture
- [ ] Sem duplicação de lógica
- [ ] Sem secrets no código
- [ ] Naming conventions seguidas
- [ ] Soft delete implementado (deleted_at)
- [ ] UUID v4 como PK
- [ ] created_at/updated_at presentes

---

## 6. Checkpoints de Validação

### Checkpoint 1: Após Leitura
```
□ LAST_SESSION.md lido
□ CURRENT_SPRINT.md lido
□ CURRENT_MODULE.md lido
□ IMPLEMENTATION_QUEUE.md lido
□ AI_ROUTER.md lido
□ Contexto do módulo lido
□ Playbook lido
□ Documentação oficial lida
```

### Checkpoint 2: Antes de Implementar
```
□ Todas as dependências verificadas
□ Arquivos proibidos identificados
□ Arquivos que serão alterados listados
□ Ordem de implementação definida
```

### Checkpoint 3: Durante Implementação
```
□ Domain layer completa
□ Application layer completa
□ Infrastructure layer completa
□ Presentation layer completa
□ Database migration criada
```

### Checkpoint 4: Após Implementação
```
□ Testes unitários criados
□ Testes de integração criados
□ Lint executado sem erros
□ Documentação atualizada
□ Memória do projeto atualizada
```

### Checkpoint 5: Antes de Finalizar
```
□ Todos os critérios de conclusão atendidos
□ Todos os checkpoints anteriores validados
□ Próxima Sprint identificada
□ Pendências registradas
```

---

## 7. Status Padronizados

| Ícone | Status | Significado |
|-------|--------|-------------|
| ⏳ | Pendente | Sprint ainda não iniciada |
| 🚧 | Em andamento | Sprint em execução |
| ✅ | Concluída | Sprint completa e validada |
| ⛔ | Bloqueada | Sprint bloqueada por dependência |
| ❌ | Cancelada | Sprint cancelada |

---

## 8. Fluxo Visual

```
┌─────────────────────────────────────────────────────────────┐
│                    INÍCIO DA SPRINT                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  FASE 1: MEMÓRIA DO PROJETO                                │
│  1. LAST_SESSION.md                                         │
│  2. CURRENT_SPRINT.md                                       │
│  3. CURRENT_MODULE.md                                       │
│  4. IMPLEMENTATION_QUEUE.md                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  FASE 2: KNOWLEDGE LAYER                                   │
│  5. AI_ROUTER.md                                            │
│  6. context/[modulo].context.md                             │
│  7. playbooks/implement-[modulo].md                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  FASE 3: DOCUMENTAÇÃO OFICIAL                               │
│  8. docs/[indicados].md                                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  CHECKPOINT 1: LEITURA COMPLETA                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  FASE 4: IMPLEMENTAÇÃO                                      │
│  1. Domain Layer                                            │
│  2. Application Layer                                       │
│  3. Infrastructure Layer                                    │
│  4. Presentation Layer                                      │
│  5. Database Migration                                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  FASE 5: TESTES                                             │
│  1. Unit Tests                                              │
│  2. Integration Tests                                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  CHECKPOINT 2: IMPLEMENTAÇÃO COMPLETA                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  FASE 6: DOCUMENTAÇÃO                                       │
│  1. CHANGELOG.md                                            │
│  2. IMPLEMENTATION_REPORT.md                                │
│  3. Memória do projeto (.ai/)                                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  CHECKPOINT 3: SPRINT COMPLETA                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    FIM DA SPRINT                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. Regras Importantes

### Regra 1: Nunca Implementar sem Contexto
Antes de escrever código, o agente DEVE:
1. Ler o contexto do módulo
2. Ler o playbook correspondente
3. Ler a documentação oficial indicada
4. Verificar dependências

### Regra 2: Sempre Atualizar Memória
Após cada implementação, o agente DEVE atualizar:
- `.ai/CURRENT_TASK.md`
- `.ai/WORKLOG.md`
- `.ai/LAST_SESSION.md`
- `docs/CHANGELOG.md`
- `IMPLEMENTATION_REPORT.md`

### Regra 3: Nunca Pular Etapas
O agente DEVE seguir a ordem do playbook:
1. Pré-requisitos
2. Documentos que devem ser lidos
3. Ordem de implementação
4. Checklist Backend
5. Checklist Frontend
6. Checklist Banco
7. Checklist Testes
8. Checklist Documentação

### Regra 4: Sempre Verificar Dependências
Antes de implementar, o agente DEVE:
1. Verificar se dependências estão implementadas
2. Verificar se há bloqueios
3. Verificar se há riscos
4. Consultar IMPLEMENTATION_QUEUE.md

### Regra 5: Sempre Rodar Lint
Antes de finalizar, o agente DEVE:
1. `mvn compile` (backend)
2. `npm run lint` (frontend)
3. `npm run typecheck` (frontend)
4. Corrigir erros encontrados

---

## 10. Exemplo de Execução

### Sprint 4.1 — Infraestrutura Auth

```
1. Ler LAST_SESSION.md → OK
2. Ler CURRENT_SPRINT.md → OK
3. Ler CURRENT_MODULE.md → OK
4. Ler IMPLEMENTATION_QUEUE.md → OK
5. Ler AI_ROUTER.md → Roteou para Auth
6. Ler auth.context.md → Contexto carregado
7. Ler implement-auth.md → Playbook carregado
8. Ler docs/01-backend/Auth.md → Docs oficiais lidos
9. Ler docs/05-business-rules/Permissions.md → Docs oficiais lidos

--- CHECKPOINT 1: LEITURA COMPLETA ---

10. Criar domain/identity/ → OK
11. Criar application/identity/ → OK
12. Criar infrastructure/identity/ → OK
13. Criar presentation/rest/identity/ → OK
14. Criar migration V002__auth_tables.sql → OK

--- CHECKPOINT 2: IMPLEMENTAÇÃO COMPLETA ---

15. Criar testes unitários → OK
16. Criar testes de integração → OK
17. Executar mvn compile → OK

--- CHECKPOINT 3: TESTES COMPLETOS ---

18. Atualizar CHANGELOG.md → OK
19. Atualizar IMPLEMENTATION_REPORT.md → OK
20. Atualizar LAST_SESSION.md → OK
21. Atualizar WORKLOG.md → OK
22. Atualizar CURRENT_TASK.md → OK

--- CHECKPOINT 4: DOCUMENTAÇÃO COMPLETA ---

23. Sprint 4.1 CONCLUÍDA ✅
```

---

*Protocolo v1.0 — 2026-07-15*
