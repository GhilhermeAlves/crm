# Estrutura do Projeto

## Visão Geral
```
crm/
├── backend/          ← Código Java (Spring Boot)
├── frontend/         ← Código React (Next.js)
├── docs/             ← Documentação Oficial (source of truth)
├── docs-ai/          ← Knowledge Layer (navegação)
├── contexts/         ← Contextos por módulo
├── playbooks/        ← Playbooks de implementação
├── prompts/          ← Biblioteca de prompts
├── .ai/              ← Memória do projeto (runtime)
├── docker/           ← Configurações Docker
├── infra/            ← Infraestrutura
├── scripts/          ← Scripts de automação
└── .github/          ← CI/CD
```

## Descrição por Pasta

### `backend/`
- **Responsabilidade:** Código-fonte Java do backend
- **Tecnologias:** Java 21, Spring Boot 3.4.1, Maven, Clean Architecture
- **Estrutura:** domain/, application/, infrastructure/, presentation/, shared/
- **Regra:** NUNCA duplicar lógica de negócio — sempre implementar aqui

### `frontend/`
- **Responsabilidade:** Código-fonte React do frontend
- **Tecnologias:** Next.js 14, React 18, TypeScript 5, Tailwind CSS 3, Shadcn UI
- **Estrutura:** app/, components/, hooks/, lib/, providers/, types/
- **Regra:** NUNCA duplicar componentes — sempre implementar aqui

### `docs/`
- **Responsabilidade:** Documentação oficial do projeto
- **Conteúdo:** Architecture, Modules, Database, Business Rules, Integrations, DevOps
- **Regra:** Source of truth — NUNCA modificar estrutura, apenas atualizar conteúdo

### `docs-ai/`
- **Responsabilidade:** Camada de navegação para agentes IA
- **Conteúdo:** AI_ROUTER, indexes, policies, decision trees
- **Regra:** NUNCA conter lógica de negócio — apenas navegação

### `contexts/`
- **Responsabilidade:** Contextos técnicos por módulo
- **Conteúdo:** 21 contextos (.context.md) com <3 min de leitura
- **Regra:** NUNCA duplicar documentação oficial — apenas resumir

### `playbooks/`
- **Responsabilidade:** Playbooks de implementação
- **Conteúdo:** 12 playbooks com checklists detalhados
- **Regra:** NUNCA pular etapas — sempre seguir a ordem

### `prompts/`
- **Responsabilidade:** Biblioteca de prompts reutilizáveis
- **Conteúdo:** 11 prompts para diferentes tarefas
- **Regra:** NUNCA usar prompts genéricos — sempre usar os específicos do projeto

### `.ai/`
- **Responsabilidade:** Memória persistente do projeto
- **Conteúdo:** Estado operacional, decisões, blocker, worklog
- **Regra:** NUNCA iniciar sessão sem ler LAST_SESSION.md primeiro

### `docker/`
- **Responsabilidade:** Configurações de containerização
- **Conteúdo:** docker-compose.yml, Dockerfiles
- **Regra:** Manter sincronizado com dependências

### `infra/`
- **Responsabilidade:** Configuração de infraestrutura
- **Conteúdo:** Terraform, Kubernetes (futuro)
- **Regra:** Preparado para decomposição em microservices

### `scripts/`
- **Responsabilidade:** Scripts de automação
- **Conteúdo:** setup.sh, seed.sh, deploy.sh
- **Regra:** Documentar cada script

### `.github/`
- **Responsabilidade:** CI/CD
- **Conteúdo:** ci.yml, cd.yml
- **Regra:** Manter pipelines atualizados

---

## Fluxo de Trabalho

```
Solicitação → .ai/LAST_SESSION → AI_ROUTER → Context → Playbook → Official Docs → Code → .ai/ Update
```

---

*Atualizado em: 2026-07-15*
