# .ai/ — Memória do Projeto

## Objetivo

Esta pasta armazena exclusivamente o **estado operacional** do projeto para agentes de IA. Ela permite que qualquer agente IA retome o projeto lendo poucos arquivos.

## Como Utilizar

### Para Agentes IA (Início de Sessão)

1. **LER PRIMEIRO:** `.ai/LAST_SESSION.md`
2. **DEPOIS:** `.ai/CURRENT_SPRINT.md`
3. **DEPOIS:** `.ai/CURRENT_MODULE.md`
4. **DEPOIS:** `.ai/CURRENT_TASK.md`
5. **CONSULTAR:** `docs-ai/AI_ROUTER.md`
6. **LER:** O contexto do módulo indicado
7. **LER:** O playbook correspondente
8. **LER:** A documentação oficial indicada
9. **IMPLEMENTAR**
10. **ATUALIZAR:** LAST_SESSION, CURRENT_TASK, WORKLOG, CHANGELOG

### Para Humanos

- Consultar `.ai/PROJECT_STATUS.md` para ver o estado geral
- Consultar `.ai/PROJECT_TIMELINE.md` para ver a evolução
- Consultar `.ai/IMPLEMENTATION_QUEUE.md` para ver a fila de sprints

## Quando Atualizar

- **Após cada sprint:** CURRENT_SPRINT, PROJECT_STATUS, PROJECT_TIMELINE, WORKLOG
- **Após cada tarefa:** CURRENT_TASK, LAST_SESSION
- **Após cada decisão:** CURRENT_DECISIONS
- **Ao encontrar problema:** KNOWN_ISSUES, BLOCKERS
- **Ao adicionar dependência:** ACTIVE_DEPENDENCIES

## Quem Pode Modificar

- **Agentes IA:** Devem atualizar após cada implementação
- **Humanos:** Podem atualizar a qualquer momento

## Diferença entre Pastas

| Pasta | Conteúdo | Modificável |
|-------|----------|-------------|
| `docs/` | Documentação oficial (source of truth) | Sim (conteúdo) |
| `docs-ai/` | Navegação para agentes IA | Raramente |
| `contexts/` | Contextos técnicos por módulo | Raramente |
| `playbooks/` | Playbooks de implementação | Raramente |
| `prompts/` | Biblioteca de prompts | Raramente |
| `.ai/` | Estado operacional do projeto | Sim (sempre) |

## Regra de Ouro

> **Esta pasta NÃO substitui a documentação.**
> Ela apenas registra o estado atual do desenvolvimento.
> A documentação oficial em `docs/` é sempre o source of truth.

## Arquivos

| Arquivo | Função |
|---------|--------|
| `README.md` | Este arquivo |
| `LAST_SESSION.md` | Última sessão (PRIMEIRO a ler) |
| `CURRENT_SPRINT.md` | Sprint atual |
| `CURRENT_MODULE.md` | Módulo atual |
| `CURRENT_TASK.md` | Tarefa atual |
| `CURRENT_DECISIONS.md` | Decisões de arquitetura |
| `KNOWN_ISSUES.md` | Problemas conhecidos |
| `NEXT_STEPS.md` | Próximos passos |
| `WORKLOG.md` | Diário de trabalho |
| `PROJECT_TIMELINE.md` | Linha do tempo |
| `PROJECT_STATUS.md` | Status geral |
| `PROJECT_STRUCTURE.md` | Estrutura do projeto |
| `ACTIVE_DEPENDENCIES.md` | Dependências ativas |
| `OPEN_TASKS.md` | Tarefas abertas |
| `BLOCKERS.md` | Bloqueios e riscos |
| `IMPLEMENTATION_QUEUE.md` | Fila de implementação |
| `SESSION_TEMPLATE.md` | Modelo de sessão |
| `AI_MEMORY_RULES.md` | Regras de memória |

---

*Última atualização: 2026-07-15*
