-- Autoria das mensagens enviadas: IA (agente) ou HUMANO (Inbox ou celular do dono).
-- Permite que a IA saiba, ao retomar após uma pausa, o que a equipe conversou.
-- NULL = mensagens antigas/automações sem autoria conhecida.
ALTER TABLE omnichannel_messages ADD COLUMN sent_by VARCHAR(10);
ALTER TABLE omnichannel_messages
    ADD CONSTRAINT chk_omnichannel_messages_sent_by CHECK (sent_by IN ('AI', 'HUMAN'));
