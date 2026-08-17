# Tarefa Atual

## Identificação
- **Tarefa:** Módulo de Notificações
- **Sprint:** Roadmap / Notificações
- **Prioridade:** Alta
- **Status:** ⏳ Próximo passo (roadmap atualizado)

## Descrição
Após a revisão profunda do estado real dos módulos e a atualização do roadmap `.ai/`, o próximo
módulo a implementar é **Notificações**:

- **Backend:** tabela `notifications`, entidade de domínio, repositório, serviço de criação/consulta,
  controller REST (`GET` minhas notificações, `markAsRead`), mecanismo de push (WebSocket/SSE) e
  provider de e-mail real (hoje é `ConsoleEmailSender` fake).
- **Frontend:** página de notificações, hook `useNotifications`, serviço, badge/sino real no `Header.tsx`
  (hoje é decorativo/hardcoded).

## Dependências
- Nenhuma (módulo novo, greenfield).

## Depois
- **IA / Sugestão de resposta** (Sprint 20) — integração LLM para sugerir respostas no chat.

---

*Atualizado em: 2026-08-17*