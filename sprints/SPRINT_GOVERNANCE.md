# Governança de Sprints

## Definition of Ready (DoR)

Uma sprint está pronta para começar quando:

- [ ] Objetivo da sprint está claramente definido
- [ ] Escopo está delimitado
- [ ] Módulos envolvidos estão identificados
- [ ] Playbook(s) foram selecionados
- [ ] Contexto(s) foram identificados
- [ ] Documentação oficial foi mapeada
- [ ] Dependências foram verificadas
- [ ] Nenhum blocker impede o início
- [ ] Sprint anterior foi concluída

## Definition of Done (DoD)

Uma sprint é considerada CONCLUÍDA quando:

### Código
- [ ] Código segue Clean Architecture
- [ ] Código segue naming conventions
- [ ] Sem duplicação de lógica
- [ ] Sem secrets no código
- [ ] Soft delete implementado (se aplicável)
- [ ] UUID v4 como PK
- [ ] created_at/updated_at presentes

### Backend
- [ ] Domain entities com value objects
- [ ] Application services com casos de uso
- [ ] Infrastructure implementations
- [ ] REST controllers com DTOs
- [ ] Migration Flyway criada
- [ ] Testes unitários (≥80%)
- [ ] Testes de integração (≥60%)
- [ ] Lint sem erros (`mvn compile`)

### Frontend
- [ ] Components criados (≤200 linhas)
- [ ] Hooks criados (useQuery/useMutation)
- [ ] Pages criadas com layouts
- [ ] Types definidos
- [ ] Validações Zod implementadas
- [ ] Lint sem erros (`npm run lint`)
- [ ] TypeScript sem erros (`npm run typecheck`)

### Documentação
- [ ] `docs/CHANGELOG.md` atualizado
- [ ] `IMPLEMENTATION_REPORT.md` atualizado
- [ ] `.ai/LAST_SESSION.md` atualizado
- [ ] `.ai/WORKLOG.md` atualizado
- [ ] `.ai/CURRENT_TASK.md` atualizado
- [ ] `.ai/CURRENT_SPRINT.md` atualizado
- [ ] `.ai/PROJECT_STATUS.md` atualizado
- [ ] `.ai/IMPLEMENTATION_QUEUE.md` atualizado
- [ ] `sprints/[NUMERO]/REVIEW.md` preenchido
- [ ] `sprints/[NUMERO]/RETROSPECTIVE.md` preenchido
- [ ] `sprints/[NUMERO]/REPORT.md` preenchido

### Qualidade
- [ ] Review realizado
- [ ] Correções aplicadas
- [ ] Testes passando
- [ ] Lint aprovado

## Critérios para Iniciar Sprint

1. DoR completo
2. Sprint anterior finalizada e fechada
3. Nenhum blocker crítico
4. Playbook e contexto disponíveis
5. Dependências satisfeitas

## Critérios para Encerrar Sprint

1. DoD completo
2. Review aprovado
3. Correções (se houver) aplicadas e validadas
4. Documentação atualizada
5. Retrospectiva preenchida
6. SPRINT_INDEX.md atualizado
7. Próxima sprint identificada

## Fluxo de Aprovação

```
Sprint Completa
      │
      ▼
Review → [Aprovado?] ──Sim──→ Correções (se necessário)
      │                        │
      Não                      │
      ▼                        ▼
  Refazer                  Documentação
      │                        │
      ▼                        ▼
  Nova Revisão             Retrospectiva
                              │
                              ▼
                            Close
```

## Atualização Obrigatória da Documentação

### Após cada sprint:
1. `docs/CHANGELOG.md` — Registrar mudanças
2. `backend/IMPLEMENTATION_REPORT.md` — Atualizar backend
3. `frontend/IMPLEMENTATION_REPORT.md` — Atualizar frontend

### Após cada sprint (memória):
4. `.ai/LAST_SESSION.md` — Atualizar última sessão
5. `.ai/WORKLOG.md` — Registrar trabalho
6. `.ai/CURRENT_TASK.md` — Fechar tarefa
7. `.ai/CURRENT_SPRINT.md` — Atualizar status
8. `.ai/PROJECT_STATUS.md` — Atualizar progresso
9. `.ai/IMPLEMENTATION_QUEUE.md` — Avançar fila

### Após cada sprint (camada de sprints):
10. `sprints/[NUMERO]/REVIEW.md` — Preencher review
11. `sprints/[NUMERO]/RETROSPECTIVE.md` — Preencher retrospectiva
12. `sprints/[NUMERO]/REPORT.md` — Preencher relatório
13. `sprints/SPRINT_INDEX.md` — Atualizar índice

### Se necessário:
14. `contexts/[modulo].context.md` — Atualizar se módulo mudou
15. `playbooks/implement-[modulo].md` — Atualizar se processo mudou
16. `docs-ai/AI_ROUTER.md` — Atualizar roteamento se necessário

---

*Versão 1.0 — 2026-07-15*
