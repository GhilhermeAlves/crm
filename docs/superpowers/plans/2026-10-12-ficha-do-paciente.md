# Ficha do Paciente (Fase 1) Implementation Plan

> **For agentic workers (opencode):** execute task a task, na ordem. Steps usam checkbox (`- [ ]`). Ao fim de cada task rode a verificação indicada e faça **um commit**. Não faça push/merge.

**Goal:** Transformar `/contacts/[id]` numa ficha do paciente com cabeçalho e abas (Visão geral, Consultas, Financeiro, Arquivos, Anotações) e persistir o cadastro completo (endereço, responsável, plano, prontuário, referência) com página de edição.

**Spec:** `docs/superpowers/specs/2026-10-12-ficha-do-paciente-design.md` (leia antes; a seção 2 descreve o fluxo do Capim que estamos replicando).

**Tech Stack:** Spring Boot hexagonal (`backend/`), Next.js App Router + TanStack Query + shadcn/ui (`frontend-refine/`), PostgreSQL + Flyway, JUnit 5 + Mockito, Vitest.

## Global Constraints

- Branch `feat/patient-record` a partir de `crm-improvements-deploy-phase` atualizada (se o PR do estoque ainda não entrou, partir de `feat/inventory`).
- **Migration = `V096__patient_record.sql`.** `V095` é do estoque (`docs/superpowers/plans/2026-10-11-controle-de-estoque.md`). Esta feature não pode ir para produção antes da V095 (o Flyway rejeitaria fora de ordem). Se o estoque for abandonado, renumere para `V095` antes do merge. **Nunca editar migrations antigas.**
- Padrão RLS copiado da V092: `ENABLE` + `FORCE ROW LEVEL SECURITY`, `tenant_isolation_policy` com `app.current_tenant_id()`, `GRANT SELECT, INSERT, UPDATE, DELETE ... TO crm_app`.
- Backend: siga `masterdata/contact` (domain / application/{dto,port/in,port/out,service} / infrastructure/persistence / web). Controllers com `@CurrentCompanyId(...)` + `@PreAuthorize("hasAuthority('contact:read' | 'contact:update')")`. `TenantContext.setCompanyId`/`clear` em `finally` como os services existentes. Auditoria via `TenantAuditRecorder.record(...)` como em `StorageService`.
- Permissões existentes: `contact:read|create|update|delete`, `appointment:read|...`, `finance:read|manage`. **Nenhuma permissão nova.**
- Textos de UI em pt-BR. Componentes de `frontend-refine/src/components/ui/`, tokens do tema, sem cores hex.
- Checkstyle até 200 colunas. Prettier: `printWidth 100`, aspas duplas, `trailingComma all`.
- Verificação backend: `cd backend && mvn -q test` (ITs com Testcontainers só no CI, sem Docker local).
- Verificação frontend: `cd frontend-refine && npm run lint && npx tsc --noEmit && npx prettier --check <arquivos alterados> && npx vitest run --pool=threads`. Sem `user-event`/jest-dom → usar `fireEvent`.
- Commits Conventional (`feat(patients): ...`), terminando com a linha `Co-Authored-By` usada no repo.

---

## Task 1: Migration `V096__patient_record.sql`

**Files:** Create `backend/src/main/resources/db/migration/V096__patient_record.sql`

- [ ] Cabeçalho de comentário no estilo V092 explicando: campos de cadastro do paciente em `contacts`, tabela `contact_notes`, vínculo `storage_objects.contact_id`.
- [ ] `ALTER TABLE contacts ADD COLUMN` para: `zip_code VARCHAR(10)`, `street VARCHAR(160)`, `address_number VARCHAR(20)`, `address_complement VARCHAR(80)`, `neighborhood VARCHAR(80)`, `city VARCHAR(80)`, `state CHAR(2)`, `health_plan VARCHAR(80)`, `health_plan_number VARCHAR(40)`, `record_number VARCHAR(40)`, `referral_source VARCHAR(80)`, `has_guardian BOOLEAN NOT NULL DEFAULT FALSE`, `guardian_name VARCHAR(160)`, `guardian_cpf VARCHAR(14)`, `guardian_phone VARCHAR(20)`.
- [ ] `CREATE UNIQUE INDEX uq_contacts_company_record_number ON contacts (company_id, record_number) WHERE record_number IS NOT NULL;`
- [ ] Tabela:
```sql
CREATE TABLE contact_notes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id  UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    body        TEXT NOT NULL,
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_contact_notes_body CHECK (length(trim(body)) > 0)
);
CREATE INDEX idx_contact_notes_contact ON contact_notes (company_id, contact_id, created_at DESC);
```
  + RLS FORCE + policy + `GRANT ... TO crm_app` (copiar bloco da V092).
- [ ] `ALTER TABLE storage_objects ADD COLUMN contact_id UUID REFERENCES contacts(id) ON DELETE CASCADE;` + `CREATE INDEX idx_storage_objects_company_contact ON storage_objects (company_id, contact_id);`
- [ ] Verificar: `cd backend && mvn -q test` compila; commit `feat(patients): migration de cadastro, anotações e arquivos do paciente`.

## Task 2: Backend — cadastro completo do contato

**Files:** Modify `masterdata/contact/domain/Contact.java`, `infrastructure/persistence/entity/ContactJpaEntity.java`, `infrastructure/persistence/repository/ContactRepositoryImpl.java` (mapeamento), `application/dto/request/CreateContactRequest.java`, `UpdateContactRequest.java`, `application/dto/response/ContactResponse.java`, `application/service/ContactService.java`, `infrastructure/persistence/repository/ContactJpaRepository.java`. Test: `backend/src/test/.../masterdata/contact/ContactServiceTest.java` (criar ou estender o existente).

- [ ] Adicionar os 15 campos (camelCase: `zipCode, street, addressNumber, addressComplement, neighborhood, city, state, healthPlan, healthPlanNumber, recordNumber, referralSource, hasGuardian, guardianName, guardianCpf, guardianPhone`) em domínio, entidade JPA (`@Column(name = "...")`), DTOs de request (com `@Size` igual aos tamanhos da coluna; `state` `@Pattern("^[A-Z]{2}$")`) e response.
- [ ] Normalização no service: `zipCode`, `guardianCpf`, `guardianPhone` só dígitos; strings em branco → `null`; `hasGuardian=false` zera `guardianName/Cpf/Phone`; `hasGuardian=true` exige `guardianName` e `guardianPhone` (`ContactValidationException`, mensagem pt-BR).
- [ ] Prontuário único: `ContactJpaRepository.existsByCompanyIdAndRecordNumberAndIdNot(...)`; em create/update, se duplicado, lançar exceção mapeada para **409** com "Já existe um paciente com este nº de prontuário." (ver como `ContactValidationException` é mapeada no `GlobalExceptionHandler`; criar `ContactConflictException` se precisar de 409).
- [ ] `UpdateContactRequest` segue a semântica atual do update (campos nulos = não alterar). Para permitir limpar um campo, o front envia string vazia, que o service converte em `null` — documente isso no Javadoc do DTO.
- [ ] Testes: cria com endereço/responsável persistindo; responsável obrigatório quando `hasGuardian`; prontuário duplicado → conflito; `hasGuardian=false` limpa dados.
- [ ] `mvn -q test`; commit `feat(patients): cadastro completo do paciente no contato`.

## Task 3: Backend — consultas do paciente

**Files:** Modify `sales/scheduling/web/AppointmentController.java`, port de entrada/serviço de appointment e repositório JPA. Test no teste de serviço de appointment existente.

- [ ] Novo endpoint `GET /api/v1/companies/{companyId}/contacts/{contactId}/appointments` com `@PreAuthorize("hasAuthority('appointment:read')")`, retornando `List<AppointmentResponse>` (DTO já existente) ordenado por `startAt desc`, limite 200.
- [ ] Repositório: `findByCompanyIdAndContactIdOrderByStartAtDesc(...)` (com `PageRequest.of(0, 200)`).
- [ ] Marcar presença reaproveita `POST /appointments/{id}/status/{status}` (`COMPLETED` / `NO_SHOW`). Nada novo no backend para isso.
- [ ] Teste unitário do serviço (filtra por contato, ordem desc). `mvn -q test`; commit `feat(patients): endpoint de consultas por paciente`.

## Task 4: Backend — anotações do paciente

**Files:** Create pacote `masterdata/contact/note/` com `domain/ContactNote.java`, `application/port/in/ContactNoteUseCase.java`, `application/port/out/ContactNoteRepository.java`, `application/service/ContactNoteService.java`, `application/dto/{ContactNoteRequest,ContactNoteResponse}.java`, `infrastructure/persistence/{ContactNoteJpaEntity,ContactNoteJpaRepository,ContactNoteRepositoryImpl}.java`, `web/ContactNoteController.java`. Test `ContactNoteServiceTest.java`.

- [ ] Endpoints em `/api/v1/companies/{companyId}/contacts/{contactId}/notes`: `GET` (`contact:read`, desc por `createdAt`), `POST` (`contact:update`), `PUT /{noteId}` e `DELETE /{noteId}` (`contact:update`).
- [ ] `ContactNoteRequest`: `body` `@NotBlank @Size(max = 5000)`. `ContactNoteResponse`: `id, contactId, body, createdBy, createdByName, createdAt, updatedAt` (resolver nome do autor como outras features fazem com `users`).
- [ ] Regra: PUT/DELETE só pelo autor ou por quem tem papel ADMIN; senão 403 com mensagem pt-BR. Contato inexistente → 404 (`ContactNotFoundException`).
- [ ] Auditoria CREATE/UPDATE/DELETE via `TenantAuditRecorder` (módulo do contato).
- [ ] Testes: listar ordenado, criar, editar por não autor → negado, excluir por ADMIN → ok. `mvn -q test`; commit `feat(patients): anotações do paciente`.

## Task 5: Backend — arquivos do paciente

**Files:** Modify `masterdata/storage/domain/StorageObject.java`, `infrastructure/persistence/StorageJpaEntity.java`, `StorageJpaRepository.java`, `StorageRepositoryImpl.java`, `application/port/input/StorageUseCase.java`, `application/service/StorageService.java`, `application/dto/StorageResponse.java`. Create `masterdata/storage/web/ContactFileController.java`. Test `StorageServiceTest.java`.

- [ ] Campo `contactId` (nullable) em domínio/entidade/response.
- [ ] `StorageUseCase.uploadForContact(companyId, contactId, file, userId)` e `listForContact(companyId, contactId)`; valida que o contato existe na empresa; reaproveita as validações de tamanho/cota do `upload` atual (extrair método privado comum, não duplicar).
- [ ] O `GET /storage` atual deve continuar listando só arquivos **sem** `contactId` (não poluir a tela de storage da empresa) — ajustar a query para `contactId IS NULL`.
- [ ] `ContactFileController`: `GET /api/v1/companies/{companyId}/contacts/{contactId}/files` (`contact:read`), `POST` multipart `file` (`contact:update`). Download/exclusão continuam em `/storage/{objectId}`.
- [ ] Testes: upload vincula contato; listagem do contato só traz os dele; listagem geral exclui arquivos de paciente. `mvn -q test`; commit `feat(patients): arquivos vinculados ao paciente`.

## Task 6: Frontend — tipos, services e hooks

**Files:** Modify `frontend-refine/src/features/masterdata/contacts/types/contact.types.ts`, `services/contact.service.ts`, `hooks/useContacts.ts`, `features/masterdata/storage/types/storage.types.ts`. Create `features/masterdata/contacts/{types,services,hooks}` para notes/arquivos/consultas (ex. `services/patient.service.ts`, `hooks/usePatient.ts`).

- [ ] Adicionar os 15 campos em `Contact`, `CreateContactRequest`, `UpdateContactRequest`.
- [ ] Tipos `ContactNote`, `CreateContactNoteRequest`; `contactId: string | null` em `StorageObject`.
- [ ] Services: `listContactAppointments`, `listNotes/createNote/updateNote/deleteNote`, `listContactFiles/uploadContactFile` (multipart, igual ao `storage.service`).
- [ ] Hooks TanStack Query com chaves `["contact-appointments", companyId, contactId]`, `["contact-notes", ...]`, `["contact-files", ...]`; mutations invalidam a chave correspondente. Exclusão/download de arquivo reaproveitam `useDeleteFile`/`useDownloadFile` invalidando também `contact-files`.
- [ ] `npx tsc --noEmit`; commit `feat(patients): services e hooks da ficha do paciente`.

## Task 7: Frontend — layout da ficha com cabeçalho e abas

**Files:** Create `frontend-refine/src/app/(dashboard)/contacts/[id]/layout.tsx`, `features/masterdata/contacts/components/PatientHeader.tsx`, `PatientTabs.tsx`. Modify `app/(dashboard)/contacts/[id]/page.tsx`. Test `features/masterdata/contacts/components/__tests__/PatientTabs.test.tsx`.

- [ ] `layout.tsx` (client): carrega o contato com `useContact` (ou o hook existente de detalhe), renderiza breadcrumb "Contatos › Nome", `PatientHeader` e `PatientTabs`, depois `{children}`. Estados de carregando/erro em pt-BR como o `page.tsx` atual.
- [ ] `PatientHeader`: avatar com iniciais, nome completo, celular formatado, botão WhatsApp (link `https://wa.me/55<mobile>` — só se houver celular), badge "Prontuário nº X" se houver, botão "Editar" → `/contacts/{id}/edit` (visível com `contact:update`).
- [ ] `PatientTabs`: links (não estado local) para `""` Visão geral, `/consultas`, `/financeiro` (só com `finance:read`), `/arquivos`, `/anotacoes`. Aba ativa via `usePathname()`. Usar o componente de tabs/links do design system existente; rolagem horizontal no mobile.
- [ ] `page.tsx` vira a aba **Visão geral**: remover o link "← Contatos" e o título "Customer 360" (agora no layout); manter cards, ações rápidas e diálogos.
- [ ] Teste: renderiza abas, esconde Financeiro sem permissão, marca ativa conforme pathname (mock de `next/navigation`).
- [ ] Verificação frontend; commit `feat(patients): layout da ficha com cabeçalho e abas`.

## Task 8: Frontend — cadastro completo (novo + editar)

**Files:** Create `features/masterdata/contacts/components/PatientForm.tsx`, `features/masterdata/contacts/schemas/patient.schema.ts`, `app/(dashboard)/contacts/[id]/edit/page.tsx`. Modify `app/(dashboard)/contacts/new/page.tsx`. Test `__tests__/patient.schema.test.ts`.

- [ ] Extrair o formulário de `contacts/new/page.tsx` (seções Dados, Contato, Endereço com busca de CEP, Responsável) para `PatientForm` (props `defaultValues`, `onSubmit`, `isLoading`, `submitLabel`, `onCancel`). Mover o schema zod para `patient.schema.ts`.
- [ ] Acrescentar na seção Dados: Plano, Nº do plano, Nº do prontuário, Referência (como conheceu) — todos opcionais.
- [ ] **Corrigir o bug:** o `handleSubmit` hoje descarta endereço e responsável. Mapear todos os campos para o request (`zipCode`, `street`, ..., `hasGuardian`, `guardian*`), removendo máscaras de CEP/CPF/telefone.
- [ ] Revisar o schema: CEP hoje é obrigatório (`min(1)`); tornar **opcional** como no Capim, validando formato só quando preenchido.
- [ ] `new/page.tsx` passa a usar `PatientForm`; após criar, navegar para `/contacts/{novoId}` (ficha) em vez da lista.
- [ ] `edit/page.tsx`: carrega o contato, preenche `PatientForm`, envia `UpdateContactRequest` (campo limpo → `""`), toast de sucesso, volta para a ficha. Erro 409 de prontuário mostra a mensagem do backend no campo.
- [ ] Teste do schema: CEP opcional; responsável obrigatório quando "Sim"; UF válida.
- [ ] Verificação frontend; commit `feat(patients): cadastro completo com edição`.

## Task 9: Frontend — aba Consultas

**Files:** Create `app/(dashboard)/contacts/[id]/consultas/page.tsx`, `features/masterdata/contacts/components/PatientAppointments.tsx`. Test `__tests__/PatientAppointments.test.tsx`.

- [ ] Botão "Novo agendamento" abre `CreateAppointmentDialog` (de `features/sales/scheduling`) com o contato atual como única opção/pré-selecionado; carregar `appointmentTypes` e `members` com os hooks de scheduling existentes. Se o diálogo não aceitar contato pré-selecionado, adicionar prop opcional `defaultContactId` sem quebrar a agenda.
- [ ] Seções "Próximas consultas" (`startAt >= agora` e status ≠ CANCELED, ordem asc) e "Histórico de consultas" (resto, desc), paginação local de 10.
- [ ] Cada item: data/hora por extenso pt-BR, `AppointmentStatusBadge`, profissional (host), tipo, observações recolhíveis ("Ver observações").
- [ ] Para itens passados com status `SCHEDULED`/`CONFIRMED`: pergunta "O paciente compareceu à consulta?" com botões "Compareceu" (→ `COMPLETED`) e "Não compareceu" (→ `NO_SHOW`) usando a mutation de status existente; invalidar `contact-appointments`.
- [ ] Estados vazios: "Não há nenhum agendamento."
- [ ] Teste: separa próximas/histórico; botões só aparecem em passadas pendentes; clique chama mutation com o status certo.
- [ ] Verificação frontend; commit `feat(patients): aba de consultas do paciente`.

## Task 10: Frontend — aba Financeiro

**Files:** Create `app/(dashboard)/contacts/[id]/financeiro/page.tsx`, `features/masterdata/contacts/components/PatientFinance.tsx`, `features/finance/utils/patient-summary.ts`. Test `features/finance/utils/__tests__/patient-summary.test.ts`.

- [ ] Página protegida por `finance:read` (sem permissão → mensagem "Você não tem acesso ao financeiro.").
- [ ] `useTransactions(companyId, { contactId, type: "INCOME", pageSize: 200 })` (conferir nomes reais em `ListTransactionsFilters`).
- [ ] `patient-summary.ts` (função pura): `total` = soma de não cancelados; `paid` = PAID; `pending` = PENDING com vencimento ≥ hoje; `overdue` = PENDING com vencimento < hoje. Valores em centavos, formatar com `features/finance/utils/money.ts`.
- [ ] 4 cards (Total dos pagamentos, Pago pelo paciente, Pagamento pendente, Pagamento atrasado) reaproveitando o estilo de `FinanceKpiCards`; barra de progresso pago/total.
- [ ] Lista com `TransactionsTable` (ou versão compacta) filtrada pelo paciente, agrupando parcelas pelo `installmentGroupId`.
- [ ] Botão "Registrar pagamento" (só com `finance:manage`) abre `TransactionFormDialog` com `type=INCOME` e `contactId` preenchidos/travados.
- [ ] Teste unitário do `patient-summary` (atrasado vs pendente na virada do dia, cancelados fora).
- [ ] Verificação frontend; commit `feat(patients): aba financeira do paciente`.

## Task 11: Frontend — aba Arquivos

**Files:** Create `app/(dashboard)/contacts/[id]/arquivos/page.tsx`, `features/masterdata/contacts/components/PatientFiles.tsx`.

- [ ] Botão "Enviar arquivo" (input file oculto, múltiplos) com `contact:update`; mostra progresso/loader; erros de tamanho/cota do backend em toast.
- [ ] Tabela/grade: nome, tipo, tamanho legível, enviado em, ações Baixar / Excluir (excluir com `AlertDialog` de confirmação do design system — não usar `window.confirm`).
- [ ] Estado vazio: "Nenhum arquivo enviado para este paciente."
- [ ] Verificação frontend; commit `feat(patients): aba de arquivos do paciente`.

## Task 12: Frontend — aba Anotações

**Files:** Create `app/(dashboard)/contacts/[id]/anotacoes/page.tsx`, `features/masterdata/contacts/components/PatientNotes.tsx`. Test `__tests__/PatientNotes.test.tsx`.

- [ ] Textarea + "Adicionar anotação" (com `contact:update`), limite 5000 caracteres com contador.
- [ ] Lista desc: autor, data/hora relativa + absoluta no title, corpo com quebras de linha preservadas. Editar inline e excluir (com confirmação) só para o autor ou ADMIN (o backend é a fonte da regra; o front só esconde os botões).
- [ ] Se `contact.notes` (campo legado) tiver conteúdo, mostrar no topo um card "Observação do cadastro" somente leitura.
- [ ] Estado vazio: "O paciente não possui anotações adicionadas."
- [ ] Teste: renderiza lista, envia nova anotação, esconde editar de outro autor.
- [ ] Verificação frontend; commit `feat(patients): aba de anotações do paciente`.

## Task 13: Lista de contatos estilo "Lista de Pacientes"

**Files:** Modify `features/masterdata/contacts/components/ContactTable.tsx`, `app/(dashboard)/contacts/page.tsx`, e o endpoint/serviço de busca (`ContactController` `/search` + `ContactJpaRepository`).

- [ ] Colunas: Nome (link para a ficha), CPF formatado, Celular, Prontuário, "Paciente desde" (`createdAt` dd/MM/yyyy), Ações.
- [ ] Busca aceita nome, CPF (só dígitos), celular e prontuário — estender a query do `/search` no backend (+ teste unitário).
- [ ] Botão "Editar" da linha passa a navegar para `/contacts/{id}/edit` (substitui o diálogo, se houver).
- [ ] Verificação backend + frontend; commit `feat(patients): lista de pacientes com busca por CPF e prontuário`.

## Task 14: Verificação final

- [ ] `cd backend && mvn -q test` verde.
- [ ] Verificação frontend completa verde.
- [ ] Teste manual com `npm run dev` (frontend-refine) + backend local: criar paciente com endereço/responsável → abrir ficha → editar → cada aba (consultas, financeiro, arquivos, anotações) com refresh na URL da aba.
- [ ] Atualizar a spec marcando Fase 1 como implementada e listando o que ficou para Fases 2–3.

---

## Próximas fases (não implementar agora)

- **Fase 2:** Anamnese preenchida por paciente (respostas sobre snapshot do modelo da V091, selo no cabeçalho), Evoluções (rascunho → finalizada imutável), Documentos (receita/atestado com PDF), foto do paciente.
- **Fase 3:** Ficha clínica (odontograma permanente/decídua, procedimentos por dente/face com estados A realizar/Realizado/Pré-existente, filtro por tipo) e Orçamentos (itens, desconto, status Aprovado/Perdido, visualização configurável, geração de parcelas em `financial_transactions`, vínculo procedimento ↔ orçamento).

Cada fase terá spec e plano próprios.
