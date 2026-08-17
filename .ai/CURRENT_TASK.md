# Tarefa Atual

## Identificação
- **Tarefa:** Módulo de Campanhas (Sprint 17)
- **Sprint:** 17 / Omnichannel
- **Prioridade:** Alta
- **Status:** ⏳ Próximo módulo

## Descrição
Notificações e IA/Sugestão de resposta foram **concluídos**. O próximo módulo a implementar é
**Campanhas** (Sprint 17):

- **Backend:** tabela de campanhas (ex.: `campaigns` + `campaign_contacts`/`campaign_messages`), domínio,
  repositório, serviço (criação, agendamento, execução), controller REST e migrações com RLS/permissões.
- **Frontend:** página `/campaigns` (hoje é rota sem `page.tsx`), hook `useCampaigns`, serviço, UI de
  criação/lista/detalhe.

## Dependências
- Backend de campanhas greenfield; integra com contatos/omnichannel.

## Depois
- **Automações Omnichannel** (Sprint 18) → **Analytics** (19) → fechamento Sprint 16 (WhatsApp).

---

*Atualizado em: 2026-08-17*