-- V150__trichology_v110_fixes.sql
-- Módulo Tricologia (doc v1.2, seção 1.2): corrige a V110.
-- 1) FKs da imagem sem ON DELETE CASCADE (excluir empresa/contato não apaga exame clínico).
-- 2) Soft delete: imagem ligada a um exame não é apagada, recebe deleted_at.
-- 3) Região do couro cabeludo e magnificação opcionais na própria imagem.
-- As colunas taken_at/created_at existentes continuam TIMESTAMP: os valores antigos
-- foram gravados com LocalDateTime da JVM e converter exigiria supor o fuso.

ALTER TABLE patient_trichoscopy_images DROP CONSTRAINT IF EXISTS patient_trichoscopy_images_company_id_fkey;
ALTER TABLE patient_trichoscopy_images DROP CONSTRAINT IF EXISTS patient_trichoscopy_images_contact_id_fkey;

ALTER TABLE patient_trichoscopy_images
    ADD CONSTRAINT fk_trichoscopy_images_company FOREIGN KEY (company_id) REFERENCES companies (id);
ALTER TABLE patient_trichoscopy_images
    ADD CONSTRAINT fk_trichoscopy_images_contact FOREIGN KEY (company_id, contact_id)
        REFERENCES contacts (company_id, id);

ALTER TABLE patient_trichoscopy_images
    ADD COLUMN deleted_at    TIMESTAMPTZ,
    ADD COLUMN deleted_by    UUID REFERENCES users (id),
    ADD COLUMN region_code   VARCHAR(40),
    ADD COLUMN magnification VARCHAR(10);

ALTER TABLE patient_trichoscopy_images
    ADD CONSTRAINT uq_trichoscopy_images_company_id UNIQUE (company_id, id);

CREATE INDEX idx_patient_trichoscopy_images_active
    ON patient_trichoscopy_images (company_id, contact_id, taken_at DESC)
    WHERE deleted_at IS NULL;
