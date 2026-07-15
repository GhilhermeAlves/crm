# Templates — Templates de Mensagens

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de templates de mensagens, incluindo criação, variáveis e aprovação.

## Descrição

Templates são mensagens pré-definidas com variáveis que podem ser personalizadas. São usados para comunicação em massa, respostas rápidas e automação. Templates de WhatsApp precisam de aprovação da Meta.

## Responsabilidades

- Criar e gerenciar templates de mensagem
- Suportar variáveis dinâmicas
- Gerenciar aprovação de templates WhatsApp
- Organizar templates por categorias
- Versionar templates

## Tipos de Template

| Tipo | Descrição | Exemplo |
|---|---|---|
| TEXT | Apenas texto | Mensagem de boas-vindas |
| MEDIA + TEXT | Mídia com texto | Promoção com imagem |
| BUTTONS | Texto com botões | Menu de opções |
| LIST | Texto com lista | Catálogo de produtos |
| CAROUSEL | Múltiplos cards | Showcase de produtos |

## Variáveis

```
{{variable_name}}

Exemplos:
{{contact.first_name}}  → "João"
{{contact.company}}     → "Empresa X"
{{agent.name}}          → "Maria"
{{current_date}}        → "15/07/2026"
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/templates` | Listar templates | `template:read` |
| POST | `/api/v1/templates` | Criar template | `template:write` |
| PUT | `/api/v1/templates/{id}` | Atualizar template | `template:write` |
| DELETE | `/api/v1/templates/{id}` | Deletar template | `template:delete` |
| POST | `/api/v1/templates/{id}/preview` | Preview com variáveis | `template:read` |
| POST | `/api/v1/templates/{id}/submit` | Submeter para aprovação | `template:write` |

## Dependências

- [04-integrations/WhatsApp.md](../04-integrations/WhatsApp.md) — Aprovação Meta
- [Companies.md](./Companies.md) — Templates por empresa

## Regras

- Template WhatsApp precisa de aprovação da Meta antes do uso
- Variáveis são validadas no preview
- Template não pode ser deletado se está em uso em automação
- Máximo de 1024 caracteres por template WhatsApp
- Botões: máximo 3 botões de resposta rápida
- Listas: máximo 10 opções
- Templates são versionados (mantém versão anterior)

## Futuras Melhorias

- IA para sugestão de templates
- A/B testing de templates
- Métricas de uso por template
- Templates compartilhados entre empresas
- Templates dinâmicos com dados de API
- Multilíngue

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
