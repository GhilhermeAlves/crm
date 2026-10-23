# Ficha do Paciente — Design

**Data:** 2026-10-12
**Referência:** Capim (`dash.capim.com.br/#/patients/{id}/...`), levantado navegando um paciente real em modo somente leitura.
**Plano de implementação (Fase 1):** `docs/superpowers/plans/2026-10-12-ficha-do-paciente.md`

## 0. Status

**Fase 1 — IMPLEMENTADA** (branch `feat/patient-record`), cobrindo os critérios de aceite da seção 7:

- Layout da ficha com cabeçalho e abas (`/contacts/[id]` + `/consultas`, `/financeiro`, `/arquivos`, `/anotacoes`).
- Cadastro completo persistido (endereço, plano, nº do prontuário, referência, responsável) e página `/contacts/[id]/edit`; prontuário duplicado → 409 com mensagem pt-BR.
- Abas Consultas (próximas/histórico, marcar Compareceu/Não compareceu), Financeiro (4 cards + lista + "Registrar pagamento"), Arquivos (upload/listar/baixar/excluir por paciente) e Anotações (autor/data, editar/excluir por autor ou ADMIN).
- Lista de contatos estilo "Lista de Pacientes" (colunas CPF/Celular/Prontuário/Paciente desde) com busca por nome, CPF, celular e prontuário.
- Migration `V097__patient_record.sql` (a V096 é `report_exports`; o plano citava V096).
- Verificação: `mvn -q test` e suite Vitest completa verdes.

**Fase 2 — IMPLEMENTADA** (branch `feature/pacientes-fase2`, plano `docs/superpowers/plans/2026-10-22-ficha-do-paciente-fase2.md`):

- Anamnese preenchida sobre snapshot do modelo, com alertas e selo no cabeçalho.
- Evoluções clínicas (rascunho → finalizada imutável, reforçado por trigger).
- Documentos do paciente a partir de modelos, com PDF.
- Foto do paciente no cabeçalho.
- Modelos de documento persistidos em Configurações → Documentos.
- Migration `V105__patient_clinical_record.sql`.

**Fase 3 — em andamento**, dividida em 3a Ficha clínica (spec `docs/superpowers/specs/2026-10-22-ficha-clinica-design.md`) e 3b Orçamentos (spec próprio depois). 3a implementada (branch `feature/pacientes-fase3a`), 3b pendente.

**Ficou para as Fases 2-3 (spec/plano próprios):**

- **Fase 2:** Anamnese preenchida por paciente (respostas sobre snapshot do modelo + selo no cabeçalho), Evoluções (rascunho → finalizada imutável), Documentos (receita/atestado com PDF) e foto do paciente.
- **Fase 3:** Ficha clínica (odontograma permanente/decídua, procedimentos por dente/face com estados, filtro por tipo) e Orçamentos (itens, desconto, status Aprovado/Perdido, geração de parcelas em `financial_transactions`).
- Fora de escopo: análise de crédito, assinatura eletrônica, migração de dados, pastas na aba Arquivos.


## 1. Objetivo

Transformar a página de detalhe do contato (`/contacts/[id]`, hoje "Customer 360" voltado a vendas) numa **ficha do paciente** no estilo Capim: cabeçalho fixo com dados do paciente e abas que dão acesso a todos os dados clínicos, financeiros e administrativos dele. O Customer 360 atual (oportunidades, tarefas, linha do tempo) continua acessível como uma aba.

## 2. Fluxo levantado no Capim

### 2.1 Lista de pacientes (`#/patients`)
- Título "Lista de Pacientes (N)", busca por nome, CPF, celular (com DDD) ou prontuário + botão Buscar.
- Botões: "Migre seu histórico de dados", "+ Novo paciente".
- Tabela: Nome, CPF, Celular, Prontuário, Paciente desde, Ações (menu ⋮). Paginação.
- Clicar no nome abre a ficha na aba padrão **Ficha clínica**.

### 2.2 Cabeçalho da ficha (comum a todas as abas)
- Breadcrumb: Início › Pacientes › Nome.
- Avatar com inicial, nome, celular, ícone de WhatsApp (abre conversa) e ícone de ficha/cadastro.
- Selo "Nenhuma anamnese feita" (link para a aba Anamnese) — muda quando há anamnese.
- Link "Editar" → `#/patients/{id}/edit`.
- Bloco lateral "Análise de crédito" (Capim-específico; **fora de escopo**).

### 2.3 Abas (cada uma tem rota própria)
| Aba | Rota Capim | Conteúdo |
|---|---|---|
| Ficha clínica | `/procedures` | Odontograma (Permanente/Decídua, dentes 11–48 coloridos por estado), "+ Novo Procedimento" (drawer: estado A realizar/Realizado/Pré-existente, gerar lançamento financeiro, procedimento, valor, dentes/faces, profissional, observações). Lista "Ficha geral" filtrável por estado e tipo, com dente/faces, data e vínculo com orçamento. Coluna lateral **Anotações** ("+ Adicionar anotação"). |
| Anamnese | `/anamnesis` | Lista de anamneses preenchidas; "Nova anamnese" escolhe um modelo e coleta respostas; assinatura eletrônica. |
| Evoluções | `/evolutions` | Evoluções clínicas com abas "Evoluções salvas" / "Rascunhos"; rascunho editável até ser enviado para assinatura. |
| Orçamentos | `/budgets` | Tabela Título, Última edição, Editado por, Saldo total, Status (ex. "Aprovado sem assinatura"), ações: enviar, ⋮ (Marcar como perdido, Visualizar, Excluir). "Visualizar" pergunta quais blocos exibir (valor total, valor por procedimento, dentistas, desconto, observações, forma de pagamento). |
| Financeiro | `/financial` | Cards: Total dos pagamentos, Pago pelo paciente, Pagamento pendente, Pagamento atrasado. Abas Orçamentos / Procedimentos / Avulsos, "Registrar pagamento". Card por orçamento com barra de progresso pago/total, "Ver detalhes" (modal com procedimentos, dentes e valores) e lista de pagamentos. |
| Documentos | `/documents` | Histórico de documentos gerados (receitas e atestados), "Novo documento". |
| Arquivos | `/folders` | Pastas e upload de arquivos do paciente ("Adicionar pasta", "Novo documento"). |
| Consultas | `/appointmentHistory` | "Novo agendamento", Próximas consultas, Histórico (data, status, dentista, observações, pergunta "Compareceu / Não compareceu"), paginado. |

### 2.4 Editar cadastro (`/edit`)
Foto (PNG/SVG/JPEG, até 5 MB); Dados: nome, data de nasc., CPF (bloqueado após salvo), RG + emissor, sexo, estado civil, situação profissional, plano, nº do plano, nº do prontuário, referência (como conheceu); Contato: celular, telefone, e-mail; Endereço: CEP (autocompleta), logradouro, número, complemento, bairro, cidade, estado; "Paciente tem responsável" Sim/Não (+ nome, CPF, telefone). Cancelar/Salvar.

## 3. Situação atual do CRM

| Área Capim | O que já existe | Lacuna |
|---|---|---|
| Lista | `/contacts` (`ContactTable`) com editar/excluir | Busca por CPF/prontuário, colunas CPF/Prontuário/Paciente desde |
| Cabeçalho/abas | `/contacts/[id]` = Customer 360 (cards em grade, sem abas) | Layout com abas e rotas por aba |
| Cadastro | `contacts` tem nome, e-mail, telefone, celular, CPF, RG, emissor, nascimento, sexo, estado civil, situação profissional, notes. `/contacts/new` coleta endereço e responsável | **Endereço e responsável são coletados e descartados no submit** (`contacts/new/page.tsx:326`). Sem plano, nº plano, prontuário, referência, foto. **Não existe página de edição completa** |
| Anotações | `contacts.notes` (texto único) | Lista de anotações com autor/data |
| Anamnese | Modelos (`anamnesis_models` V091, `/settings/documents/anamnese`) | Anamnese **preenchida por paciente** (respostas) |
| Evoluções | — | Tudo |
| Orçamentos | — | Tudo |
| Ficha clínica | `catalog_items` (serviços) | Odontograma e procedimentos do paciente |
| Financeiro | `financial_transactions` com `contact_id`; `GET /finance/transactions?contactId=` já existe | Aba com resumo por paciente |
| Documentos | — | Receitas/atestados |
| Arquivos | `storage_objects` (upload genérico por empresa) | Vínculo arquivo ↔ paciente e pastas |
| Consultas | `appointments.contact_id` (V079) | Endpoint filtrando por contato (o `GET /appointments` exige `from/to` e não filtra por contato); marcar Compareceu/Não compareceu = status `COMPLETED`/`NO_SHOW` já existentes |

## 4. Decisões

1. **Mesma entidade `contacts`.** Paciente = contato; nada de tabela nova de paciente. Rotas continuam em `/contacts/[id]`, com abas por segmento: `/contacts/[id]` (Visão geral), `/contacts/[id]/consultas`, `/financeiro`, `/arquivos`, `/anotacoes`, e depois `/anamnese`, `/evolucoes`, `/documentos`, `/orcamentos`, `/ficha-clinica`. Layout compartilhado em `app/(dashboard)/contacts/[id]/layout.tsx`.
2. **Customer 360 vira a aba "Visão geral"** (conteúdo atual intacto), padrão ao abrir.
3. **Faseamento.**
   - **Fase 1 (plano detalhado):** layout com cabeçalho + abas; cadastro completo persistido (endereço, responsável, plano, prontuário, referência) e página `/contacts/[id]/edit`; aba Consultas; aba Financeiro (somente leitura + atalho "Registrar pagamento" reaproveitando `TransactionFormDialog` com `contactId` preenchido); aba Arquivos (upload/listar/baixar/excluir vinculado ao contato, sem pastas); Anotações (lista com autor/data).
   - **Fase 2 (spec própria depois):** Anamnese preenchida (respostas sobre um snapshot do modelo), Evoluções (rascunho → finalizada, imutável), Documentos (receita/atestado com modelo + PDF).
   - **Fase 3 (spec própria depois):** Ficha clínica (odontograma + procedimentos do paciente) e Orçamentos (itens de procedimento, status, aprovação, geração de parcelas em `financial_transactions`).
   - Abas das fases 2–3 **não aparecem** na Fase 1 (nada de abas "em breve").
4. **Fora de escopo:** análise de crédito, assinatura eletrônica, migração de dados, foto do paciente (fica para fase 2 junto com documentos).
5. **Permissões.** Leitura de cadastro/anotações/arquivos do paciente usa `contact:read`, escrita `contact:update` (já existentes — confirmar nomes no `RoleSeedService`). Consultas usa `appointment:read`. A aba Financeiro só aparece com `finance:read`.
6. **Prontuário** (`record_number`) é texto livre opcional, único por empresa quando preenchido. **CPF** continua editável no nosso CRM (não copiar o bloqueio do Capim) — regra existente de `require_contact_cpf` (V083) é mantida.

## 5. Modelo de dados (Fase 1)

Migration nova (número após o estoque, ver plano):

- `contacts` + colunas: `zip_code VARCHAR(10)`, `street VARCHAR(160)`, `address_number VARCHAR(20)`, `address_complement VARCHAR(80)`, `neighborhood VARCHAR(80)`, `city VARCHAR(80)`, `state CHAR(2)`, `health_plan VARCHAR(80)`, `health_plan_number VARCHAR(40)`, `record_number VARCHAR(40)`, `referral_source VARCHAR(80)`, `has_guardian BOOLEAN NOT NULL DEFAULT FALSE`, `guardian_name VARCHAR(160)`, `guardian_cpf VARCHAR(14)`, `guardian_phone VARCHAR(20)`. Índice único parcial `(company_id, record_number) WHERE record_number IS NOT NULL`.
- `contact_notes` (id, company_id, contact_id FK CASCADE, body TEXT NOT NULL, created_by, created_at, updated_at) + RLS FORCE + grants.
- `storage_objects` + coluna `contact_id UUID NULL REFERENCES contacts(id) ON DELETE CASCADE` + índice `(company_id, contact_id)`.

## 6. API (Fase 1)

- `GET/PUT /contacts/{id}` passam a ler/gravar os campos novos (DTOs `CreateContactRequest`, `UpdateContactRequest`, `ContactResponse`).
- `GET /contacts/{id}/appointments` → lista ordenada por `start_at desc`, com `upcoming` e `past` separados no front.
- `GET/POST /contacts/{id}/notes`, `PUT/DELETE /contacts/{id}/notes/{noteId}` (autor edita/exclui a própria; ADMIN qualquer).
- `GET /contacts/{id}/files`, `POST /contacts/{id}/files` (multipart, reaproveita `StorageService` com `contactId`), download/exclusão pelos endpoints existentes de storage.
- Financeiro: reaproveita `GET /finance/transactions?contactId=`; resumo calculado no front (pago, pendente, atrasado = PENDING com vencimento < hoje — mesma regra da V092).

## 7. Critérios de aceite (Fase 1)

- Abrir um contato mostra cabeçalho (avatar, nome, celular, WhatsApp, Editar) e abas Visão geral, Consultas, Financeiro*, Arquivos, Anotações; cada aba tem URL própria e funciona com refresh/voltar.
- Criar contato com endereço/responsável/plano/prontuário persiste tudo; a página Editar carrega e salva os mesmos campos; prontuário duplicado retorna 409 com mensagem pt-BR.
- Consultas lista próximas e histórico do paciente; "Compareceu"/"Não compareceu" muda o status para `COMPLETED`/`NO_SHOW`; "Novo agendamento" abre `CreateAppointmentDialog` com o contato pré-selecionado.
- Financeiro mostra os 4 cards e a lista de lançamentos do paciente; "Registrar pagamento" cria lançamento INCOME com `contactId`.
- Arquivos: upload, listagem, download e exclusão restritos ao paciente e ao tenant.
- Anotações: criar, listar (mais recente primeiro, autor e data), editar/excluir conforme regra.
- Testes unitários backend e Vitest frontend verdes; ITs de RLS no CI.
