-- e2e/seed/inbox.sql
-- Task 4.3 — Conversas de inbox E2E na API do CRM (banco compartilhado crm_main).
--
-- Executa como superuser (`crm_admin` do postgres do compose) — superuser
-- ignora RLS FORCE — SEMPRE DEPOIS do backend healthcheck (Flyway completo,
-- incluindo V044/V072) e do e2e/seed/user.sql. É dev/CI apenas.
--
-- Não há endpoint autenticado para criar conversas de mensagens: elas nascem
-- apenas via webhook do provedor (no stack E2E, FakeProvider). Este seed
-- entrega estado determinístico para a spec inbox.spec.ts (Task 4.3):
--   * canal WHATSAPP/FAKE ativo da empresa default (V044 provider FAKE);
--   * N conversas OPEN com external_phone, unread_count e last_message_at
--     variados;
--   * mensagens INBOUND de texto com status SENT/DELIVERED (a spec valida que
--     a lista mostra o external_phone e o thread carrega as mensagens; o envio
--     usa FakeProvider e retorna wamid FAKE_..., portanto NÃO pre-seedamos
--     OUTBOUND).
--
-- Idempotência: marcamos tudo que é "nosso" (canal/seeds com o sufixo 'E2E-')
-- e DELETE antes de re-inserir, tornando o arquivo repetível no mesmo banco.

-- ===========================================================================
-- 1. Limpeza determinística (apenas o que o seed/canal E2E criou)
-- ===========================================================================
DELETE FROM omnichannel_messages m
USING omnichannel_channels c
WHERE m.company_id = '00000000-0000-0000-0000-000000000001'
  AND m.channel_id = c.id
  AND c.name LIKE 'E2E-%';

DELETE FROM omnichannel_conversations cv
USING omnichannel_channels c
WHERE cv.company_id = '00000000-0000-0000-0000-000000000001'
  AND cv.channel_id = c.id
  AND c.name LIKE 'E2E-%';

DELETE FROM omnichannel_channels
WHERE company_id = '00000000-0000-0000-0000-000000000001'
  AND name LIKE 'E2E-%';

-- ===========================================================================
-- 2. Canal FAKE (referenciado por conversations/messages via channel_id)
-- ===========================================================================
INSERT INTO omnichannel_channels (id, company_id, type, provider, name, status,
                                  external_id, config, secrets_ref)
VALUES ('aaaaaaaa-0000-0000-0000-0000000000e1',
        '00000000-0000-0000-0000-000000000001',
        'WHATSAPP',
        'FAKE',
        'E2E-Canal-WA',
        'ACTIVE',
        'E2E-WA-1000001',
        '{"fake":true}',  -- metadados não-sensíveis (padrão V044)
        'e2e/seed/no-secret')
ON CONFLICT DO NOTHING;

-- ===========================================================================
-- 3. Conversas OPEN (variadas para a spec, sem contato vinculado)
-- ===========================================================================
INSERT INTO omnichannel_conversations
    (id, company_id, channel_id, contact_id, external_phone, status,
     last_message_at, unread_count, created_at, updated_at)
VALUES
    ('bbbbbbbb-0000-0000-0000-0000000000c1',
     '00000000-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-0000000000e1',
     NULL,
     '5511999990001',
     'OPEN',
     CURRENT_TIMESTAMP - INTERVAL '5 minutes',
     2,
     CURRENT_TIMESTAMP - INTERVAL '2 days',
     CURRENT_TIMESTAMP - INTERVAL '5 minutes'),
    ('bbbbbbbb-0000-0000-0000-0000000000c2',
     '00000000-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-0000000000e1',
     NULL,
     '5511999990002',
     'OPEN',
     CURRENT_TIMESTAMP - INTERVAL '1 hour',
     0,
     CURRENT_TIMESTAMP - INTERVAL '1 day',
     CURRENT_TIMESTAMP - INTERVAL '1 hour')
ON CONFLICT DO NOTHING;

-- ===========================================================================
-- 4. Mensagens INBOUND (thread carregado pela spec; fake wamid p/ status)
-- ===========================================================================
-- conv1: 2 mensagens (unread_count 2) — cliente pergunta, agente ainda não viu.
INSERT INTO omnichannel_messages
    (id, company_id, conversation_id, channel_id, direction, sender_phone,
     recipient_phone, type, body, status, external_message_id, client_message_id,
     provider_error, sent_at, received_at, created_at, updated_at)
VALUES
    ('cccccccc-0000-0000-0000-0000000000d1',
     '00000000-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-0000000000c1',
     'aaaaaaaa-0000-0000-0000-0000000000e1',
     'INBOUND', '5511999990001', 'E2E-WA-1000001',
     'TEXT', 'Olá! Quero saber mais sobre o plano.', 'DELIVERED',
     'FAKE_conv1_msg1', 'dddddddd-0000-0000-0000-000000000201',
     NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '10 minutes',
     CURRENT_TIMESTAMP - INTERVAL '10 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes'),
    ('cccccccc-0000-0000-0000-0000000000d2',
     '00000000-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-0000000000c1',
     'aaaaaaaa-0000-0000-0000-0000000000e1',
     'INBOUND', '5511999990001', 'E2E-WA-1000001',
     'TEXT', 'Viu minha mensagem anterior?', 'SENT',
     'FAKE_conv1_msg2', 'dddddddd-0000-0000-0000-000000000202',
     NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '5 minutes',
     CURRENT_TIMESTAMP - INTERVAL '5 minutes', CURRENT_TIMESTAMP - INTERVAL '5 minutes'),
    -- conv2: 1 mensagem (unread_count 0) — atendida.
    ('cccccccc-0000-0000-0000-0000000000d3',
     '00000000-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-0000000000c2',
     'aaaaaaaa-0000-0000-0000-0000000000e1',
     'INBOUND', '5511999990002', 'E2E-WA-1000001',
     'TEXT', 'Obrigado pela ajuda ontem!', 'DELIVERED',
     'FAKE_conv2_msg1', 'dddddddd-0000-0000-0000-000000000203',
     NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '1 hour',
     CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour')
ON CONFLICT DO NOTHING;