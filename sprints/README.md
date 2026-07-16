# Gerenciamento de Sprints

## Objetivo

Esta pasta registra exclusivamente o ciclo de vida de cada Sprint do projeto CRM SaaS Omnichannel.

## Organização

```
sprints/
├── README.md                  ← Este arquivo
├── SPRINT_INDEX.md            ← Índice oficial de todas as sprints
├── SPRINT_TEMPLATE.md         ← Modelo padrão para criar novas sprints
├── SPRINT_LIFECYCLE.md        ← Definição do ciclo de vida
├── SPRINT_GOVERNANCE.md       ← Regras de governança
├── REVIEW_CHECKLIST.md        ← Checklist de revisão
├── RETROSPECTIVE_TEMPLATE.md  ← Template de retrospectiva
├── SPRINT_MANAGEMENT_REPORT.md ← Relatório completo da camada
│
├── 0/                         ← Planejamento
│   └── SPRINT.md, CHECKLIST.md, REVIEW.md, RETROSPECTIVE.md, REPORT.md
├── 1/                         ← Fundação
│   └── ...
├── 2/                         ← Correções
│   └── ...
├── 3.1/                       ← Knowledge Layer
│   └── ...
├── 3.2/                       ← AI Runtime Layer
│   └── ...
└── 4.1/                       ← Infraestrutura Auth
    └── SPRINT.md, CHECKLIST.md, REVIEW.md, RETROSPECTIVE.md, REPORT.md
```

## Como Criar uma Sprint

1. Copiar `SPRINT_TEMPLATE.md` para `sprints/[numero]/`
2. Renomear os arquivos conforme o template
3. Preencher SPRINT.md com os dados da sprint
4. Atualizar CHECKLIST.md com as tarefas
5. Ao finalizar: preencher REVIEW.md, RETROSPECTIVE.md, REPORT.md

## Fluxo de Trabalho

```
Planning → Development → Review → Corrections → Documentation → Retrospective → Close
```

## Boas Práticas

- Nunca pular etapas do ciclo de vida
- Sempre preencher REVIEW.md antes de encerrar
- Sempre atualizar RETROSPECTIVE.md com lições aprendidas
- Manter SPRINT_INDEX.md sempre atualizado
- Vincular sprints aos playbooks e contexts correspondentes

## Diferença entre Pastas

| Pasta | Conteúdo |
|-------|----------|
| `sprints/` | Ciclo de vida de cada sprint |
| `.ai/` | Estado operacional do projeto |
| `docs/` | Documentação oficial |
| `docs-ai/` | Navegação para IA |
| `contexts/` | Contextos técnicos |
| `playbooks/` | Playbooks de implementação |

---

*Última atualização: 2026-07-15*
