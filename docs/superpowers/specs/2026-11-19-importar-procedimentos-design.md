# Importação de procedimentos de outro sistema — design

Data: 2026-11-19 · Branch: `feature/importar-procedimentos` · Fase 2 de `2026-11-19-importar-pacientes-design.md`

## Objetivo

Importar pela tela (Configurações → Importar dados → Procedimentos) os procedimentos do
odontograma a partir de `procedures.csv` + `procedure_types.csv` do mesmo zip.

## Decisões

- **Vínculo com paciente:** pelo código de origem (`patient_id`), agora guardado em
  `data_import_contacts.source_id` (V143). Importações de pacientes anteriores à V143 não têm o
  código: é preciso desfazer e reimportar os pacientes.
- **Catálogo:** cada tipo usado vira serviço (`catalog_items`, SERVICE). Reaproveita serviço ativo
  com o mesmo nome (sem diferenciar maiúsculas/espaços); senão cria com o `base_value`. Nomes com
  acentuação quebrada na origem (`�`) entram como estão, com aviso na prévia.
- **Mapeamento:** `pending` → PLANNED, `done` → DONE (`done_at`, fallback data do registro),
  `existed` → PRE_EXISTING; `amount` (centavos) → `price_cents`; `observations` → notas.
  Dente FDI 11–48 permanente, 51–85 decíduo; arcada (`upper_arch`, `lower_arch`, `entire_arch`)
  → boca toda + nota. Faces: mesial M, distal D, oclusal/incisal O, vestibular V, lingual/palatina L.
- **Sem financeiro:** gravação JDBC direta, sem o service da ficha clínica; não gera lançamento.
- **Duplicado:** procedimento de origem já importado (importação não desfeita) é pulado.
- **Erros:** paciente não importado, tipo inexistente, dente inválido, situação desconhecida.
- **Desfazer:** apaga os procedimentos da importação e os serviços criados por ela que não estejam
  em uso. Importação inteira numa transação.

## Dados (V143)

- `data_import_contacts.source_id` + índice.
- `data_import_procedures` (import, procedimento, empresa, source_id) e
  `data_import_catalog_items` (import, serviço, empresa), com RLS e GRANT.

## Testes

`ProcedureRowMapperTest`, `ProcedureImportServiceTest`, ajustes em `PatientRowMapperTest` /
`PatientImportServiceTest`; Vitest `ImportPanel.test.tsx`.
