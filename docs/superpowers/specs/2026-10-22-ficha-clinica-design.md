# Ficha do Paciente — Fase 3a: Ficha clínica (odontograma + procedimentos)

**Status:** Implementada (branch `feature/pacientes-fase3a`, base `crm-improvements-deploy-phase`). Aprovado em brainstorming em 2026-10-22.

A Fase 3 foi dividida em duas entregas com spec/plano/PR próprios:

- **3a (este spec):** odontograma, procedimentos do paciente por dente/face, estados, filtros, lançamento financeiro opcional ao realizar.
- **3b (spec próprio depois):** Orçamentos — itens a partir de procedimentos "A realizar", desconto, status Aprovado/Perdido, parcelas em `financial_transactions`, visualização configurável e vínculo procedimento ↔ orçamento (`budget_id`).

## 1. Modelo de dados

Migration `V106__patient_procedures.sql`, com RLS (`ENABLE` + `FORCE` + `tenant_isolation_policy`) e GRANT no mesmo padrão da V105.

### `patient_procedures`

| Coluna | Tipo | Regra |
|---|---|---|
| `id` | UUID PK | `gen_random_uuid()` |
| `company_id` | UUID NOT NULL | FK `companies`, RLS |
| `contact_id` | UUID NOT NULL | FK `contacts` ON DELETE CASCADE |
| `catalog_item_id` | UUID NOT NULL | FK `catalog_items`; na criação deve ser `SERVICE` e `active` |
| `name_snapshot` | VARCHAR(160) NOT NULL | nome do serviço no momento da criação |
| `state` | VARCHAR(20) NOT NULL | `PLANNED` \| `DONE` \| `PRE_EXISTING` |
| `dentition` | VARCHAR(10) NOT NULL | `PERMANENT` \| `DECIDUOUS` |
| `price_cents` | BIGINT NOT NULL | `>= 0`; padrão = preço do catálogo (0 se nulo) |
| `professional_id` | UUID NULL | usuário responsável |
| `notes` | TEXT NULL | |
| `performed_at` | DATE NULL | obrigatório quando `state = DONE` (CHECK) |
| `financial_transaction_id` | UUID NULL | FK `financial_transactions` ON DELETE SET NULL |
| `created_by`, `created_at`, `updated_at` | | auditoria |

Índices: `(company_id, contact_id)`.

### `patient_procedure_teeth`

| Coluna | Tipo | Regra |
|---|---|---|
| `procedure_id` | UUID NOT NULL | FK `patient_procedures` ON DELETE CASCADE |
| `company_id` | UUID NOT NULL | RLS |
| `tooth` | SMALLINT NOT NULL | FDI; validado contra a dentição no service |
| `faces` | VARCHAR(1)[] NOT NULL | subconjunto de `M`, `D`, `O`, `V`, `L`; pode ser vazio |

PK `(procedure_id, tooth)`. Procedimento sem dentes = boca toda.

Dentes válidos: permanente 11–18, 21–28, 31–38, 41–48; decídua 51–55, 61–65, 71–75, 81–85. A face `O` representa oclusal/incisal e `L` lingual/palatina.

## 2. Regras de negócio (service)

- **Criar:** valida serviço (`SERVICE`, ativo, mesma empresa), dentes x dentição, faces sem repetição. `DONE` exige `performedAt`. Se `DONE` e `generateTransaction = true`, cria `INCOME` `PENDING` ligada ao contato, categoria `PROCEDURE`, forma de pagamento `PaymentMethod.OTHER`, valor `price_cents` (> 0, senão 400), descrição `Procedimento: <nome> – dentes <lista>` (ou "boca toda"), vencimento informado (padrão hoje), e grava `financial_transaction_id`.
- **Concluir** (`POST /{id}/complete`): só de `PLANNED` (senão 409). Recebe `performedAt`, `professionalId`, `generateTransaction`, `dueDate`. Mesma geração do item anterior.
- **Editar** (`PATCH /{id}`): `PLANNED` e `PRE_EXISTING` editam todos os campos (inclusive trocar entre esses dois estados; ir para `DONE` só via concluir). `DONE` edita apenas `notes`; qualquer outro campo → 409.
- **Excluir:** se houver lançamento `PAID` → 409 com mensagem "Procedimento com pagamento registrado não pode ser excluído". Se `PENDING` → lançamento vira `CANCELLED` na mesma transação. `CANCELLED` ou sem lançamento → exclui direto.
- **Permissões:** leitura `contact:read`; escrita `contact:update`; gerar lançamento exige também `finance:create` (sem ela → 403).
- Autorização de tenant segue o padrão atual (service + RLS como última barreira).

## 3. API

Base `/api/v1/companies/{companyId}/patients/{contactId}/procedures`:

| Método | Rota | Descrição |
|---|---|---|
| GET | `/` | lista; filtros opcionais `state`, `category` (de `catalog_items`), `tooth`; ordenado por `created_at desc` |
| POST | `/` | cria |
| PATCH | `/{procedureId}` | edita |
| POST | `/{procedureId}/complete` | conclui |
| DELETE | `/{procedureId}` | exclui (204) |

Resposta inclui `category`, `professionalName`, `teeth[{tooth, faces}]` e `transaction{id, status}` quando houver. O resumo do odontograma é calculado no frontend a partir da lista.

Erros: validação → 400; conflito de estado ou exclusão com lançamento pago → 409; procedimento não encontrado → 404; gerar lançamento sem `finance:create` → 403.

## 4. Frontend (`frontend-refine`)

- Rota `src/app/(dashboard)/patients/[id]/ficha-clinica/page.tsx`; aba **"Ficha clínica"** em `PatientTabs`, logo após "Consultas", visível com `contact:read`.
- `src/features/clinical/procedures/`:
  - `services/procedure.service.ts`, `hooks/useProcedures.ts` (`useProcedures(contactId, filters)` + mutations via mutation helper).
  - `Odontogram.tsx` — SVG; toggle Permanente/Decídua; 4 quadrantes. Cor do dente pela prioridade **A realizar > Realizado > Pré-existente**, com legenda; faces como 5 setores pintados quando há procedimento nelas; procedimentos sem dente não colorem. Clique no dente aplica/limpa o filtro `tooth`. Reutilizável em modo seleção (usado no drawer).
  - `ProcedureDrawer.tsx` — criar/editar: estado, serviço (autopreenche preço), valor, data (quando Realizado), dentes (mini-odontograma em modo seleção) + chips de faces por dente, profissional, observações, checkbox "Gerar lançamento financeiro" + vencimento (só em Realizado e com `finance:create`). Em `DONE`, apenas observações editáveis.
  - `CompleteProcedureDialog.tsx` — data, profissional, gerar lançamento + vencimento.
  - `ProcedureList.tsx` ("Ficha geral") — filtros estado/tipo/dente; colunas procedimento, dentes/faces, estado, valor, data, profissional, status do lançamento; ações editar, marcar como realizado (só `PLANNED`), excluir (confirmação avisa o cancelamento do lançamento pendente; bloqueada com aviso quando pago).
- Layout: odontograma em cima, lista embaixo. Sem coluna de anotações (aba Anotações já existe).

## 5. Testes e verificação

- Backend: unitários do service (validação de dentes/faces, transições, 409 em edição de `DONE`, exclusão com `PAID`/`PENDING`, geração do lançamento, 403 sem `finance:create`); `PatientProcedureIsolationIT` (RLS); teste unitário do controller (`PatientProcedureControllerTest`).
- Frontend (Vitest): odontograma (prioridade de cor, filtro por clique, toggle de dentição), drawer (checkbox condicional, bloqueio em Realizado), lista (ações por estado, aviso de exclusão).
- `mvn test`; `npm run lint`, `npx tsc --noEmit`, `npx vitest run --pool=threads`; Prettier `--check --end-of-line auto` em todo arquivo alterado do frontend. Sem banco local: conferir nome da migration, RLS e GRANT; o E2E do CI é o primeiro Postgres real.

## 6. Fora de escopo

Orçamentos e `budget_id` (3b); anotações por dente; impressão/PDF do odontograma; histórico de alterações de procedimento; procedimentos em dentes supranumerários.
