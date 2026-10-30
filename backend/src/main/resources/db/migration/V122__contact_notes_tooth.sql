-- V122__contact_notes_tooth.sql
-- Anotação do paciente ligada opcionalmente a um dente (numeração FDI), usada só
-- por empresas de Odontologia. NULL = anotação geral.

ALTER TABLE contact_notes ADD COLUMN tooth INTEGER;
ALTER TABLE contact_notes ADD CONSTRAINT chk_contact_notes_tooth CHECK (
    tooth IS NULL
    OR (tooth / 10 BETWEEN 1 AND 4 AND tooth % 10 BETWEEN 1 AND 8)
    OR (tooth / 10 BETWEEN 5 AND 8 AND tooth % 10 BETWEEN 1 AND 5));
