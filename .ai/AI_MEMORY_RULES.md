# Regras de Memória para Agentes IA

## Regra 1: Ordem de Leitura Obrigatória

Todo agente IA DEVE seguir esta ordem ao iniciar uma sessão:

```
1. .ai/LAST_SESSION.md
2. .ai/CURRENT_SPRINT.md
3. .ai/CURRENT_MODULE.md
4. .ai/CURRENT_TASK.md
5. docs-ai/AI_ROUTER.md
6. [Contexto do módulo]
7. [Playbook correspondente]
8. [Documentação oficial indicada]
```

## Regra 2: Nunca Implementar sem Contexto

Antes de escrever código, o agente DEVE:
1. Ler o contexto do módulo
2. Ler o playbook correspondente
3. Ler a documentação oficial indicada
4. Verificar dependências

## Regra 3: Sempre Atualizar Memória

Após cada implementação, o agente DEVE atualizar:
- `.ai/CURRENT_TASK.md` — Status da tarefa
- `.ai/WORKLOG.md` — Diário de trabalho
- `.ai/LAST_SESSION.md` — Última sessão
- `docs/CHANGELOG.md` — Changelog do projeto
- `backend/IMPLEMENTATION_REPORT.md` ou `frontend/IMPLEMENTATION_REPORT.md`

## Regra 4: Nunca Modificar Documentação Oficial

O agente NUNCA deve modificar:
- `docs/**` — Apenas adicionar conteúdo, nunca remover ou reestruturar
- `docs-ai/**` — Apenas atualizar routing se necessário
- `contexts/**` — Apenas atualizar se módulo mudou
- `playbooks/**` — Apenas atualizar se processo mudou
- `prompts/**` — Apenas adicionar novos prompts

## Regra 5: Nunca Pular Etapas

O agente DEVE seguir a ordem do playbook:
1. Pré-requisitos
2. Documentos que devem ser lidos
3. Ordem de implementação
4. Checklist Backend
5. Checklist Frontend
6. Checklist Banco
7. Checklist Testes
8. Checklist Documentação

## Regra 6: Sempre Verificar Dependências

Antes de implementar, o agente DEVE:
1. Verificar se dependências estão implementadas
2. Verificar se há bloqueios
3. Verificar se há riscos
4. Consultar IMPLEMENTATION_QUEUE.md

## Regra 7: Sempre Rodar Lint

Antes de finalizar, o agente DEVE:
1. `mvn compile` (backend)
2. `npm run lint` (frontend)
3. `npm run typecheck` (frontend)
4. Corrigir erros encontrados

## Regra 8: Nunca Criar Sem Testar

O agente DEVE criar testes para:
1. Cada entidade de domínio
2. Cada caso de uso
3. Cada controller
4. Cada componente React

## Regra 9: Sempre Documentar Decisões

Toda decisão de arquitetura DEVE ser registrada em:
- `.ai/CURRENT_DECISIONS.md` — Decisões técnicas
- `docs/ARCHITECTURE_DECISIONS.md` — Decisões oficiais

## Regra 10: Nunca Deixar Blockers

Se um blocker for encontrado:
1. Registrar em `.ai/BLOCKERS.md`
2. Tentar resolver
3. Se não resolver, registrar plano de ação
4. Notificar na próxima sessão

---

## Fluxo Completo de uma Sessão

```
INÍCIO
  │
  ├─ Ler LAST_SESSION.md
  ├─ Ler CURRENT_SPRINT.md
  ├─ Ler CURRENT_MODULE.md
  ├─ Ler CURRENT_TASK.md
  │
  ├─ Consultar AI_ROUTER.md
  ├─ Ler Contexto do módulo
  ├─ Ler Playbook correspondente
  ├─ Ler Documentação oficial
  │
  ├─ Implementar código
  ├─ Criar testes
  ├─ Rodar lint
  │
  ├─ Atualizar CURRENT_TASK.md
  ├─ Atualizar WORKLOG.md
  ├─ Atualizar LAST_SESSION.md
  ├─ Atualizar CHANGELOG.md
  ├─ Atualizar IMPLEMENTATION_REPORT.md
  │
  FIM
```

---

*Regras v1.0 — 2026-07-15*
