-- V158__storage_objects_temporary.sql
-- Arquivos temporários (exportações de relatório) não consomem a cota
-- permanente de armazenamento (max_storage_mb) nem aparecem na listagem de
-- arquivos da empresa. RLS/GRANT de storage_objects não mudam (V037).

ALTER TABLE storage_objects ADD COLUMN IF NOT EXISTS temporary BOOLEAN NOT NULL DEFAULT FALSE;

-- Exportações já geradas passam a ser temporárias.
UPDATE storage_objects s
   SET temporary = TRUE
  FROM report_exports r
 WHERE r.storage_object_id = s.id
   AND r.company_id = s.company_id;
