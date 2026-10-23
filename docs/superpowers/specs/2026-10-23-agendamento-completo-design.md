# Novo agendamento completo (paridade com o Capim) + link de cadastro do paciente

**Status:** Implementada em 2026-10-23. Aprovada pelo usuário em 2026-10-23 ("use os serviços do catálogo e inclua o link de cadastro agora"). Branch `feature/agendamento-completo` (base `crm-improvements-deploy-phase` @ 096b89f2). Sem merge/deploy até o usuário liberar o pacote todo.

## Ajustes reais (código final)

- **Link de cadastro — persistência e segurança:** segue o padrão do convite. O contexto do token é fixado via `set_config` e há policies extras de RLS para a leitura pública; o token é guardado como hash SHA-256 (o valor em claro existe só na URL devolvida). Não existe `app.find_registration_link`; a resolução pública usa o mecanismo do convite.
- **URL do link:** `MAIL_FRONTEND_BASE_URL` + `/cadastro/<token>`.
- **Limite de taxa:** 20 requisições por 10 minutos por IP, com contador no Redis.
- **Cadastro público:** campo vazio = não alterar o dado existente do contato.
- **Telefone:** o telefone informado no cadastro público vai para `contacts.mobile`.
- **Status inicial:** `COMPLETED` e `NO_SHOW` são rejeitados na criação (só `SCHEDULED`, `CONFIRMED`, `CANCELED`).
- **Conflito de recorrência:** lança `SlotUnavailableException` listando até 5 datas em conflito.
- **WhatsApp:** a janela do `wa.me` abre de forma síncrona no clique; se o navegador bloquear, o fallback é copiar o link.
- **Frontend:** `/cadastro` está liberado no middleware de autenticação.
- **Deploy:** a regra nginx `location ^~ /api/v1/public/registration/` precisa ser aplicada à mão na VPS (além do `deploy.sh`).

## Problemas encontrados (comparação com o Capim, paciente "Teste Claude")

1. Aba Consultas do paciente não passa `chairs`/`tags` ao `CreateAppointmentDialog` → Cadeira e Marcador somem.
2. `status` escolhido no formulário é descartado (o backend não recebe; tudo nasce `SCHEDULED`). Padrão do formulário é "Confirmado"; no Capim é "Pendente".
3. "Repetir agendamento" e "Repetir bloqueio" são só visuais: o backend não cria ocorrências. Falta "Personalizado".
4. Faltam: celular do paciente, "Enviar link de cadastro", título pré-preenchido com o nome do paciente, Procedimentos (serviços do catálogo), aviso "Evento fora da faixa de horário", "Adicionar novo marcador" na lista.
5. Pelo WhatsApp o paciente não consegue gravar os próprios dados no CRM (o agente só usa o nome no título do agendamento).

## Decisões

- **Status na criação:** `CreateAppointmentRequest.status` (`SCHEDULED` | `CONFIRMED` | `CANCELED`, padrão `SCHEDULED`). Aplicado a todas as ocorrências. Formulário inicia em "Pendente".
- **Recorrência** (agendamento e bloqueio): `recurrence: { frequency: DAILY|WEEKLY|MONTHLY|YEARLY, interval: 1–99, endType: NEVER|ON_DATE|AFTER_COUNT, until?: LocalDate, count?: 2–366 }`, nulo = não repete. Ocorrências calculadas na data local `America/Sao_Paulo` mantendo o horário (DST-safe). `NEVER` gera até 1 ano após o início; teto absoluto de 366 ocorrências. Todas recebem o mesmo `recurrence_group_id`. Conflito em qualquer ocorrência → nada é criado e a resposta lista as datas em conflito (mesma regra de `force` atual, aplicada a cada ocorrência). A resposta continua sendo o primeiro agendamento/bloqueio. Presets da tela: Todos os dias = DAILY/1, Semanal = WEEKLY/1, Mensal = MONTHLY/1, Anual = YEARLY/1, Personalizado = diálogo "Configurar recorrência" (repetir a cada N dia/semana/mês/ano; termina Nunca / Em data / Após N ocorrências).
- **Procedimentos no agendamento:** serviços do catálogo (`catalog_items` com `item_type = SERVICE`, ativos), múltipla escolha, gravados em `appointment_procedures`. Criar e editar aceitam `catalogItemIds`; a resposta traz `procedures: [{id, name}]`. Botão "Adicionar procedimentos" leva a Configurações → Catálogo (nova aba).
- **Celular do paciente:** campo no formulário quando há paciente; se alterado, atualiza o telefone do contato antes de criar o agendamento (validação de telefone BR).
- **Título:** pré-preenchido com o nome do paciente quando o paciente vem definido (editável).
- **Aviso de horário:** "Evento fora da faixa de horário" quando De/Até saem do expediente configurado na Agenda (aviso, não bloqueia).
- **Marcador:** na aba do paciente aparece igual à Agenda; a lista ganha "+ Novo marcador" (nome + cor) usando `useCreateTag`.
- **Aba do paciente:** sem a aba "Bloqueio de Agenda" (bloqueio é coisa da Agenda).
- **Link de cadastro do paciente:**
  - Tabela `patient_registration_links` (token aleatório de 32 bytes base64url, `expires_at` = 7 dias, `used_at`, `contact_id`, `created_by`).
  - Autenticado: `POST /api/v1/companies/{companyId}/patients/{contactId}/registration-link` (`contact:update`) → `{ url, expiresAt }`; gerar de novo invalida o anterior não usado.
  - Público (sem login, rota própria no nginx para o 8081, `permitAll`): `GET /api/v1/public/registration/{token}` → nome da clínica e dados atuais (nome, telefone) ou 404/410; `POST` mesmo caminho com `{ firstName, lastName, cpf, birthDate, email, phone, address{zipCode, street, number, complement, neighborhood, city, state} }` → atualiza o contato (mesmas validações do cadastro, incluindo CPF) e marca `used_at`. Honeypot + limite de taxa por IP como as rotas públicas existentes. Token usado/expirado → 410.
  - Página pública `/cadastro/[token]` (grupo de rota sem login, como `(invitation)`), formulário simples em pt-BR, mensagem de sucesso.
  - Botão "Enviar link de cadastro": gera o link e abre `https://wa.me/55<telefone>?text=<mensagem com o link>` em nova aba; ao lado, "Copiar link". Sem telefone → só copiar.
  - **Agente do WhatsApp:** nova ferramenta `enviar_link_cadastro` (sem argumentos) que gera o link para o contato da conversa e devolve a URL para o agente mandar ao paciente; orientação no prompt: oferecer quando faltarem dados cadastrais.
- **Permissões:** sem permissões novas (agendamento: `appointment:*`; link: `contact:update`).
- **Fora de escopo:** editar/excluir a série inteira de uma recorrência; integração do procedimento do agendamento com a ficha clínica; envio do link pela API do WhatsApp sem conversa aberta.

## Migration `V108__appointment_recurrence_procedures_registration.sql`

- `ALTER TABLE appointments ADD COLUMN recurrence_group_id UUID;` + índice.
- `ALTER TABLE schedule_blocks ADD COLUMN recurrence_group_id UUID;` + índice.
- `appointment_procedures (appointment_id FK appointments CASCADE, catalog_item_id FK catalog_items, company_id, PK (appointment_id, catalog_item_id))`.
- `patient_registration_links (id, company_id, contact_id FK contacts CASCADE, token VARCHAR(64) UNIQUE NOT NULL, expires_at, used_at, created_by, created_at)`.
- RLS ENABLE+FORCE+policy+GRANT nas tabelas novas. A rota pública lê o link sem tenant: usar o mesmo mecanismo das rotas públicas existentes (ex.: convite) para resolver a empresa pelo token antes de fixar o `TenantContext` (função `SECURITY DEFINER` ou consulta pelo papel de serviço — seguir o padrão já usado no projeto).

## Deploy (lembrete)

Além do `deploy.sh`, aplicar à mão na VPS a regra nginx `location ^~ /api/v1/public/registration/` → 8081 (backup → copiar → `nginx -t` → reload).
