# Módulo Atual

## Identificação
- **Nome:** Campanhas (Sprint 17)
- **Tipo:** Backend + Frontend
- **Status:** ⏳ Próximo módulo a implementar

## Objetivo
Sistema de campanhas de comunicação (email/whatsapp): cadastro de campanhas, definição de público
(contatos), agendamento e execução de envios, com RLS por tenant e permissões `campaign:*`.

## Estado Atual (módulos recém-concluídos)
- **Notificações:** ✅ backend (tabela `notifications` + RLS V047, permissões V048, controller REST, push
  WebSocket/STOMP, auditoria) + frontend (sino real com badge, página `/notifications`, sidebar, polling).
- **IA/Sugestão (Sprint 20):** ✅ backend (providers OpenAI real/fake + service + controller) + frontend
  (botão ✨ no Inbox).
- **Campanhas:** backend vazio (`application/campaign`, `domain/campaign`), frontend rota `/campaigns`
  sem `page.tsx`, sem tabela no banco.

## Documentação Relacionada
- `docs/01-backend/Campaigns.md`
- `docs/02-frontend/Campaigns.md`
- `docs/05-business-rules/Campaign.md`

## Dependências
- Contatos (lista de destinatários), omnichannel (envio WhatsApp), e-mail (`EmailSender`).

## Próxima Etapa
Implementar Campanhas (backend → migrações → frontend). Depois: **Automações (18)** → **Analytics (19)**.

---

*Atualizado em: 2026-08-17*