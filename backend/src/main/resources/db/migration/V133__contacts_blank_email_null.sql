-- V133__contacts_blank_email_null.sql
-- Contatos sem e-mail eram gravados com '' (string vazia) na criação. Como o
-- índice único uk_contacts_email_company (email, company_id) trata '' como um
-- valor, o segundo contato sem e-mail da empresa falhava com 500. O código passa
-- a gravar NULL; aqui os registros antigos são normalizados.

UPDATE contacts SET email = NULL WHERE email IS NOT NULL AND btrim(email) = '';
