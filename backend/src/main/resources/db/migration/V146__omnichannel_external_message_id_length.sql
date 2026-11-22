-- O mid do Instagram Direct passa de 120 caracteres (o wamid do WhatsApp cabia).
-- Aumentar VARCHAR no Postgres só altera metadados; o índice único continua válido.
ALTER TABLE omnichannel_messages ALTER COLUMN external_message_id TYPE VARCHAR(512);
