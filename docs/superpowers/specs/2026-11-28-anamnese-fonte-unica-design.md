# Anamnese como fonte única (Parte 1 do fluxo de atendimento tricológico)

Data: 2026-11-28 · Branch: `feature/anamnese-fonte-unica` · Base: `crm-improvements-deploy-phase`

## Contexto

Teste ponta a ponta em produção (28/11/2026) mostrou dois questionários sobre o mesmo paciente:

- **Anamnese** (`patient_anamneses`): formulário livre montado por clínica, preenchido no modo recepção ou por link. Não sabe que "Nome completo" é o nome da pessoa e não pré-preenche nada do cadastro.
- **Quiz do exame tricológico** (`trichology_assessments.quiz_answers`): definição fixa no backend (`QuizEngine`), com chaves usadas pelas regras do Copiloto (`ExamCopilotService.RULES`: `pattern`, `onset`, `family_history`, `daily_shedding`, `recent_triggers`, `postpartum`, `scalp_symptoms`, `hair_procedures`, `acne_hirsutism`, `irregular_cycles`, `anabolic_use`) e por `AssessmentService` (`sex`, para faixas de referência do laboratório). A IA do laudo (`ReportService`) recebe `quiz` no input.

Decisões do usuário:
1. A IA e o Copiloto passam a usar a **anamnese**; o quiz sai.
2. A anamnese também é um meio de **completar o cadastro da pessoa**.

Esta é a Parte 1 de 5 (0 correções · 1 anamnese · 2 consulta→avaliação/sessão · 3 Copiloto sugere plano/orçamento/sessões · 4 impressões).

## Objetivo

Uma anamnese preenchida uma vez: vem pré-preenchida com o cadastro, completa o cadastro ao ser enviada e alimenta o exame tricológico (Copiloto e laudo).

## Fora do escopo

- Ligar exame à consulta, plano de tratamento, orçamento, impressões (Partes 2–4).
- Remover a coluna `quiz_answers` do banco (fica para exames antigos).
- Léo do menu (fica como está).

## Desenho

### 1. Campo vinculado na pergunta

Migration `V163__anamnesis_question_binding.sql`:

```sql
ALTER TABLE anamnesis_questions ADD COLUMN binding VARCHAR(40);
-- CHECK com a lista fechada abaixo; NULL = pergunta livre.
CREATE UNIQUE INDEX uq_anamnesis_questions_model_binding
    ON anamnesis_questions (model_id, binding) WHERE binding IS NOT NULL;
```

Um vínculo aparece no máximo uma vez por modelo. O snapshot JSONB de `patient_anamneses` passa a carregar `binding` de cada pergunta (snapshots antigos sem o campo continuam válidos).

Vínculos (enum `AnamnesisBinding`, backend e frontend):

| Grupo | Vínculo | Tipo de pergunta aceito | Destino |
|---|---|---|---|
| Cadastro | `PERSON_NAME` | SHORT_TEXT | `persons.name` |
| Cadastro | `PERSON_BIRTH_DATE` | DATE (V117) | `persons.birth_or_foundation_date` |
| Cadastro | `PERSON_SEX` | SINGLE_SELECT | `persons.gender` (opções mapeadas por `value`) |
| Cadastro | `PERSON_CPF` | SHORT_TEXT | `persons.tax_id` (CPF, 11 dígitos; índice único por empresa) |
| Cadastro | `PERSON_MOBILE` | SHORT_TEXT | `person_contacts` MOBILE principal |
| Cadastro | `PERSON_EMAIL` | SHORT_TEXT | `person_contacts` EMAIL principal |
| Cadastro | `PERSON_ADDRESS` | LONG_TEXT/SHORT_TEXT | `person_addresses` (texto livre no logradouro quando não há endereço) |
| Exame | `TRICHO_SEX` | SINGLE_SELECT | chave `sex` |
| Exame | `TRICHO_PATTERN` | SINGLE/MULTI_SELECT | `pattern` |
| Exame | `TRICHO_ONSET` | SINGLE_SELECT | `onset` |
| Exame | `TRICHO_DAILY_SHEDDING` | SINGLE_SELECT | `daily_shedding` |
| Exame | `TRICHO_FAMILY_HISTORY` | YES_NO(_WITH_TEXT) | `family_history` |
| Exame | `TRICHO_RECENT_TRIGGERS` | MULTI_SELECT | `recent_triggers` |
| Exame | `TRICHO_POSTPARTUM` | YES_NO | `postpartum` |
| Exame | `TRICHO_SCALP_SYMPTOMS` | MULTI_SELECT | `scalp_symptoms` |
| Exame | `TRICHO_HAIR_PROCEDURES` | MULTI_SELECT | `hair_procedures` |
| Exame | `TRICHO_ACNE_HIRSUTISM` | YES_NO | `acne_hirsutism` |
| Exame | `TRICHO_IRREGULAR_CYCLES` | YES_NO | `irregular_cycles` |
| Exame | `TRICHO_ANABOLIC_USE` | YES_NO | `anabolic_use` |
| Exame | `TRICHO_CHIEF_COMPLAINT` | LONG_TEXT | queixa principal do exame |

Para os vínculos de exame com opções, o `value` das opções da pergunta deve usar os códigos das regras (ex.: `DIFFUSE_SHEDDING`, `LT_3M`). O editor mostra os códigos esperados ao escolher o vínculo e a validação do modelo recusa opção desconhecida. Tipos YES_NO viram `"true"/"false"`.

Editor de modelos (frontend): seletor "Vincular a" agrupado (Cadastro / Exame tricológico); vínculos de Exame só aparecem com o módulo TRICHOLOGY ativo. Pergunta vinculada exibe um selo.

### 2. Pré-preenchimento

Ao iniciar uma anamnese (recepção, link ou link aberto), o backend devolve `prefill` com os valores atuais do cadastro para os vínculos `PERSON_*`. O formulário mostra os campos preenchidos e editáveis. No modo link público, só os campos vinculados recebem prefill (nada além do que a pessoa já informou).

### 3. Gravação no cadastro ao enviar

Novo `AnamnesisProfileSync` (aplicação, módulo `clinical.anamnesis`) chamado na conclusão da anamnese, na mesma transação:

- Campo do cadastro **vazio** → grava.
- Campo **igual** (após normalizar: trim, caixa, só dígitos para CPF/celular) → nada.
- Campo **diferente** → não sobrescreve; cria registro em `person_profile_divergences` (nova tabela na V163: `id, company_id, person_id, anamnesis_id, field, current_value, proposed_value, status PENDING|ACCEPTED|DISMISSED, resolved_by, resolved_at, created_at`, RLS como `persons`).
- **CPF**: valida dígitos; inválido → ignora e cria divergência com motivo; já usado por outra pessoa da empresa → não grava e cria divergência "CPF já cadastrado em outra pessoa".
- CPF válido gravado e pessoa provisória → `provisional = false`.
- Grava `person_history` com origem "Anamnese".

Ficha do paciente: faixa "Dados da anamnese divergentes do cadastro (N)" com Aceitar/Ignorar por campo (permissão `contact:update`). Aceitar aplica pelo serviço de pessoas (mesmas validações da edição manual).

### 4. Exame tricológico lê a anamnese

- `trichology_assessments.anamnesis_id UUID NULL` (V163). Ao abrir o exame, aponta a anamnese **concluída** mais recente do paciente; se não houver, fica nulo e pode ser ligada depois (quando uma anamnese é concluída e o paciente tem exame aberto sem anamnese, liga automaticamente).
- Novo `AnamnesisClinicalView` (porta) devolve: mapa das respostas `TRICHO_*` no formato das chaves do quiz + lista "pergunta → resposta" de todas as perguntas não profissionais.
- `ExamCopilotService.hypotheses`, `AssessmentService` (sexo do laboratório) e `ImageAnalysisService` passam a usar esse mapa. Exames antigos sem `anamnesis_id` continuam usando `quiz_answers`.
- `ReportService`: input `anamnesis` (lista pergunta → resposta) no lugar de `quiz`. Perguntas `PERSON_*` são **excluídas** do input da IA (sem dados identificadores).
- Pendência "Quiz do paciente não respondido" → "Sem anamnese concluída" com ação "Enviar anamnese" (abre o fluxo existente da aba Anamnese).
- `TRICHO_CHIEF_COMPLAINT` preenche a queixa do exame quando aberta sem queixa.

### 5. Remoção do quiz

- Frontend: remove a aba Quiz, `QuizForm`, a página pública `/quiz-capilar/*` e chamadas de `quiz-definition`, `quiz-link`, `PUT quiz`.
- Backend: remove os endpoints `quiz-definition`, `assessments/{id}/quiz-link`, `PUT assessments/{id}/quiz` e `GET/POST /public/trichology/quiz/{token}`; remove `QuizEngine` após migrar as regras. Mantém leitura de `quiz_answers` para exames antigos.

### 6. Modelo pronto

- Migration de dados na V163: para cada modelo cujo nome é "Ficha de Anamnese Capilar", vincula por texto exato as perguntas "Nome completo" (primeira ocorrência), "Data de nascimento", "Sexo", "Telefone / WhatsApp", "E-mail", "Cidade / endereço", "Qual é sua principal queixa ou disfunção capilar?". Perguntas não encontradas ficam sem vínculo (sem erro).
- Perguntas clínicas de tricologia (padrão, início, queda diária etc.) **não existem** no modelo atual; o usuário as adiciona pelo editor. O spec da Parte 3 pode trazer um modelo padrão completo.

### 7. Ajustes menores

- Novo procedimento (ficha do paciente e dentro do orçamento): ao escolher o procedimento, o valor é preenchido com o preço do catálogo (editável).
- Impressão do orçamento: rótulo "Dentistas" → "Profissionais" (opção e bloco impresso; o parâmetro `dentists` da URL fica igual para não quebrar links).
- Toast genérico "An unexpected error occurred" → "Não foi possível concluir a ação. Tente novamente." (`GlobalExceptionHandler` e fallback do frontend).

## Erros e bordas

- Modelo com vínculo duplicado → 400 no salvar do modelo.
- Opção com código desconhecido em vínculo de exame → 400 com a lista de códigos aceitos.
- Anamnese enviada por link público: sync roda sem usuário (`resolved_by` nulo, histórico com origem "Anamnese (paciente)").
- Falha do sync não pode perder a anamnese: divergências são gravadas na mesma transação; erro inesperado no sync é logado e a anamnese conclui (sync é best-effort por campo).
- RLS/GRANT em todas as tabelas novas (sem banco local: conferir colisão de nome, RLS FORCE e GRANT ao `crm_app`; o E2E do CI é o primeiro Postgres real).
- CRM_ADMIN passa em todas as guardas novas.

## Testes

- Unit: `AnamnesisProfileSync` (vazio grava, igual ignora, diferente diverge, CPF inválido, CPF duplicado, provisório vira definitivo).
- Unit: mapeamento `TRICHO_*` → chaves do quiz; `ExamCopilotService.hypotheses` com anamnese.
- Unit: `ReportService` não envia `PERSON_*` à IA.
- IT (Testcontainers): V163 aplicada — RLS de `person_profile_divergences`, índice único de vínculo.
- Frontend: editor com seletor de vínculo; formulário com prefill; faixa de divergências; remoção da aba Quiz.
- E2E manual em produção após deploy: refazer o fluxo do "Paciente Teste Fluxo".

## Entrega

Um PR para `crm-improvements-deploy-phase`. Backend e frontend no mesmo deploy (`backend frontend`).
