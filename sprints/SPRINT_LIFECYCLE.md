# Ciclo de Vida da Sprint

## Fluxo Oficial

```
┌─────────────────────────────────────────────────────────────┐
│                       1. PLANNING                           │
│  Definir objetivo, escopo, módulos, playbooks, contextos    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     2. DEVELOPMENT                          │
│  Seguir SPRINT_EXECUTION_PROTOCOL.md rigorosamente          │
│  FASE 1 → FASE 2 → FASE 3 → CHECKPOINT → FASE 4 → CHECK   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       3. REVIEW                             │
│  Revisar código, arquitetura, testes, documentação          │
│  Preencher REVIEW_CHECKLIST.md                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     4. CORRECTIONS                          │
│  Aplicar correções apontadas no review                      │
│  Re-executar lint, testes                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   5. DOCUMENTATION                          │
│  Atualizar: CHANGELOG, IMPLEMENTATION_REPORT, .ai/          │
│  Atualizar: docs-ai/, contexts/, playbooks/ (se necessário) │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    6. RETROSPECTIVE                         │
│  Preencher RETROSPECTIVE.md                                 │
│  Registrar: o que funcionou, o que melhorar, lições         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        7. CLOSE                             │
│  Marcar sprint como ✅ Concluída                            │
│  Atualizar SPRINT_INDEX.md                                  │
│  Atualizar IMPLEMENTATION_QUEUE.md                          │
│  Identificar próxima sprint                                 │
└─────────────────────────────────────────────────────────────┘
```

## Detalhamento das Etapas

### 1. Planning
- Definir objetivo da sprint
- Definir escopo (o que será feito)
- Identificar módulos envolvidos
- Selecionar playbook(s) correspondente(s)
- Selecionar contexto(s) correspondente(s)
- Verificar dependências
- Estimar esforço

### 2. Development
- Seguir rigorosamente o protocolo em `.ai/SPRINT_EXECUTION_PROTOCOL.md`
- FASE 1: Memória do Projeto (.ai/)
- FASE 2: Knowledge Layer (docs-ai/, contexts/, playbooks/)
- FASE 3: Documentação Oficial (docs/)
- CHECKPOINT 1: Leitura Completa
- FASE 4: Implementação (Domain → Application → Infrastructure → Presentation)
- FASE 5: Testes (Unit → Integration)
- CHECKPOINT 2: Implementação Completa

### 3. Review
- Revisar arquitetura (Clean Architecture compliance)
- Revisar código (naming, patterns, SOLID)
- Revisar backend (controllers, services, repositories)
- Revisar frontend (components, hooks, types)
- Revisar banco (migrations, índices, FKs)
- Revisar testes (cobertura, qualidade)
- Preencher REVIEW_CHECKLIST.md

### 4. Corrections
- Aplicar correções apontadas no review
- Re-executar lint
- Re-executar testes
- Validar que correções não quebraram nada

### 5. Documentation
- Atualizar `docs/CHANGELOG.md`
- Atualizar `backend/IMPLEMENTATION_REPORT.md` ou `frontend/IMPLEMENTATION_REPORT.md`
- Atualizar `.ai/LAST_SESSION.md`
- Atualizar `.ai/WORKLOG.md`
- Atualizar `.ai/CURRENT_TASK.md`
- Atualizar `.ai/CURRENT_SPRINT.md`
- Atualizar `.ai/PROJECT_STATUS.md`
- Atualizar `.ai/IMPLEMENTATION_QUEUE.md`
- Atualizar contexts/ (se módulo mudou)
- Atualizar playbooks/ (se processo mudou)

### 6. Retrospective
- Preencher RETROSPECTIVE.md
- O que funcionou bem
- O que pode ser melhorado
- Problemas encontrados
- Lições aprendidas
- Próximos passos

### 7. Close
- Marcar sprint como ✅ Concluída
- Atualizar `sprints/SPRINT_INDEX.md`
- Atualizar `.ai/IMPLEMENTATION_QUEUE.md`
- Identificar e preparar próxima sprint

## Regras de Transição

- **Planning → Development**: Apenas quando Definition of Ready estiver completo
- **Development → Review**: Apenas quando toda implementação estiver completa
- **Review → Corrections**: Apenas se houver itens de correção
- **Review → Documentation**: Apenas se review for aprovado
- **Corrections → Review**: Re-Revisão obrigatória
- **Documentation → Retrospective**: Sempre
- **Retrospective → Close**: Sempre

---

*Versão 1.0 — 2026-07-15*
