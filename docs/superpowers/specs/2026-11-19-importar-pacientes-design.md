# Importação de pacientes de outro sistema — design

Data: 2026-11-19 · Branch: `feature/importar-pacientes`

## Objetivo

Permitir que a clínica importe, **pela interface do CRM**, os pacientes de uma
exportação de outro sistema (zip de CSVs, ex.: `base_clinica_37294_05112026.zip`).
Primeira entrega: só `patients.csv`. Demais arquivos são listados como "ainda não suportado".

## Escopo

- Dentro: pré-visualização, importação de pacientes, rastreio por importação e desfazer.
- Fora: anotações, procedimentos, orçamentos, financeiro, anamneses, arquivos de documentos
  (o zip só traz metadados em `documents.csv`).

## Tela

Configurações → "Importar base de outro sistema" (admin da empresa; CRM_ADMIN sempre passa).

1. Upload do `.zip`.
2. Prévia: totais (OK / duplicado / erro) e tabela por linha com motivo.
3. "Importar N pacientes" → grava só os válidos e mostra o resultado.
4. Histórico de importações da empresa com botão "Desfazer".

## Backend — módulo `masterdata/importing`

- `POST /imports/patients/preview` (multipart zip) → análise, não grava.
- `POST /imports/patients` (multipart zip) → reanalisa e cria via `ContactService`
  (mesmas validações/regras do cadastro manual), marcando `import_id`.
- `GET /imports` → histórico da empresa.
- `DELETE /imports/{id}` → desfaz: remove pacientes da importação **sem** agenda nem
  procedimentos; retorna removidos e mantidos.

Limites: zip até 20 MB; ignora entradas fora de `*.csv`; proteção contra zip-slip/zip-bomb
(lê só `patients.csv` em memória com limite de tamanho).

### Mapeamento `patients.csv` → `CreateContactRequest`

| Origem | Destino |
|---|---|
| `name` | `firstName` (1ª palavra) + `lastName` (resto) |
| `email` | `email` (inválido → erro da linha) |
| `phone` | `mobile` (só dígitos) |
| `local_phone` | `phone` |
| `birthdate` | `birthDate` |
| `cpf` | `cpf` (só dígitos) |
| `identity_document` / `identity_document_emissor` | `rg` / `rgIssuer` |
| `gender`, `marital_status` | `gender`, `maritalStatus` (normalizados para os valores do CRM; desconhecido → vazio) |
| `occupation` | `professionalStatus` |
| `zipcode`, `address1`, `address_number`, `address2`, `district`, `city`, `state` | endereço (UF em maiúsculas) |
| `health_plan`, `health_plan_number` | convênio |
| `medical_record_id` | `recordNumber` |
| `source` | `referralSource` |
| `contact_name`, `contact_cpf`, `contact_phone` | responsável (`hasGuardian` = nome preenchido) |

Linhas com `discarded_at` preenchido são ignoradas (contam como "ignorado").
Valores acima do tamanho máximo do campo são truncados com aviso (não erro).

### Duplicados

Duplicado = CPF ou prontuário já existente na empresa, ou repetido dentro do próprio arquivo.
Duplicado é pulado, nunca sobrescrito — reimportar é seguro.

## Dados

Migration nova:
- Tabela `data_import` (`id`, `company_id`, `user_id`, `source_file`, `created_at`,
  `total`, `imported`, `skipped`, `failed`, `undone_at`), com RLS por empresa e GRANT iguais
  às demais tabelas multiempresa.
- Coluna `import_id` (nullable, FK) em contato + índice.

## Testes

- Unitários: leitor do zip/CSV, mapeamento, detecção de duplicados (CSV fictício; dados reais
  nunca entram no repositório).
- Service: importação e desfazer.
- Frontend: diálogo de prévia/resultado.
- Backend: o usuário roda `mvn test`; frontend: testes + Prettier.

## Validação

Após deploy: importar numa empresa de teste em produção, conferir, desfazer; depois repetir na
empresa da Dra. Raquel.
