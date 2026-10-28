-- V114__specialty_occupational_safety.sql
-- A especialidade do convite/membro passa a aceitar os mesmos valores da
-- especialidade da empresa (CompanySegment), incluindo Tec. Segurança do Trabalho.

ALTER TABLE invitations DROP CONSTRAINT IF EXISTS chk_invitations_specialty;
ALTER TABLE invitations ADD CONSTRAINT chk_invitations_specialty
    CHECK (specialty IN ('GENERIC', 'DENTAL', 'OCCUPATIONAL_SAFETY', 'TRICHOLOGY'));

ALTER TABLE memberships DROP CONSTRAINT IF EXISTS chk_memberships_specialty;
ALTER TABLE memberships ADD CONSTRAINT chk_memberships_specialty
    CHECK (specialty IN ('GENERIC', 'DENTAL', 'OCCUPATIONAL_SAFETY', 'TRICHOLOGY'));
