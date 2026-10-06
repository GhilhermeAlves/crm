-- Nome do convidado (opcional), informado por quem convida em
-- Minha Empresa -> Gerenciar usuarios. Exibido na lista de convites pendentes
-- e usado para preencher o cadastro por convite.
ALTER TABLE invitations ADD COLUMN IF NOT EXISTS invitee_name varchar(255);
