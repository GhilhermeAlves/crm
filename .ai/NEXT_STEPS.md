# Próximos Passos

## 1. 🔴 Módulo de Campanhas (Sprint 17) — próximo

### Status
⏳ Próximo módulo (após Notificações e IA concluídos) — 2026-08-17

### Backend (greenfield)
- Migração Flyway: tabela de campanhas (ex.: `campaigns` + `campaign_contacts`/`campaign_messages`) +
  RLS tenant + permissões `campaign:*` + grants `crm_app`.
- Domain: `Campaign`, `CampaignStatus`/`CampaignType` (ex.: email, whatsapp), exceções.
- Application: `CampaignService` (create, list, detail, schedule, execute), ports, DTOs.
- Infrastructure: entidade JPA, repositório, mapper.
- Presentation: `CampaignController`.
- Integração com contatos (lista de destino) e envio (reuso de `EmailSender`/`WhatsAppProvider`).

### Frontend
- Feature `campaigns` (types, service, hook `useCampaigns`).
- Página `/campaigns` (hoje rota sem `page.tsx`): lista, criação, detalhe, status.

## 2. 🔴 Automações Omnichannel (Sprint 18) — depois

### Status
⏳ Após Campanhas

- Automatizar envio de campanhas (agendamento, triggers) integrado ao módulo de Workflows.

## 3. 🟠 Analytics / Dashboard avançado (Sprint 19)

- Relatórios avançados; página `/reports` (hoje rota sem página).

## 4. 🟡 Fechamento Sprint 16 (WhatsApp deploy)
- Deploy/VPS + IT Testcontainers + E2E manual.

## 5. 🟡 Itens de maturidade (já implementados)
- **Notificações:** ✅ backend + WebSocket/STOMP + frontend. Resta e-mail real (hoje console fake) e,
  opcionalmente, conectar o frontend ao STOMP (hoje polling 15s).
- **IA/Sugestão:** ✅ backend + frontend. Resta chave OpenAI real (hoje `AI_PROVIDER=fake`).

---

*Última atualização: 2026-08-17*