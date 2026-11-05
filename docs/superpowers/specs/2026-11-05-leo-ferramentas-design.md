# Leo: marcar consulta, cadastrar paciente, lançar procedimento e exame — design

Data: 2026-11-05 · Branch: `feat/leo-ferramentas`

## Objetivo

O Leo (chat do CRM e Telegram) passa a atender pedidos como:

> "Cadastre a paciente Lúcia de Aguiar da Silva, 10 facetas em resina composta nos dentes 11 a 15 e 21 a 25,
> R$ 750 por dente, e marque a consulta para 11/11 às 13:30."

Toda escrita continua passando pelo fluxo existente `AiAction` (`PROPOSED → CONFIRMED → EXECUTED`), com botões
✅/❌ no Telegram e confirmação no chat do CRM. Nada é gravado sem confirmação.

## Etapas (uma entrega cada, nesta ordem)

### Etapa A — Correções de comportamento do Leo
1. **Texto corrompido**: `AiAssistantService` tem o literal `"N�o consegui..."` com encoding quebrado. Corrigir para UTF-8
   (`"Não ..."`) e procurar outros literais quebrados no pacote `automation/ai` (`grep -rn $'\xEF\xBF\xBD'`).
2. **Sem resposta final**: quando o laço atinge o limite de iterações com tool calls pendentes, fazer UMA chamada final
   ao modelo sem ferramentas (lista vazia de tools), pedindo um resumo do que foi feito e do que falta. Se ainda vier vazio,
   montar a resposta a partir das propostas (`"Preparei N ações para você confirmar: ..."`).
3. **Limite de iterações**: subir `MAX_TOOL_ITERATIONS` de 5 para 8 (pedidos com vários passos).
4. **Propostas duplicadas**: no mesmo turno, se o modelo propuser a mesma ferramenta com os mesmos parâmetros, não criar
   uma segunda `AiAction`; devolver a proposta já existente ao modelo. Implementar no `AiActionService.propose` ou no laço:
   chave = tool + parâmetros normalizados, escopo = conversa + ações `PROPOSED` criadas no turno atual.
5. **Instrução de sistema** (prompt do Leo): (a) se não houver ferramenta para algo, dizer claramente "isso eu ainda não
   consigo fazer" e nunca dar a entender que fez; (b) pedidos com dependência (paciente novo → procedimento/consulta):
   propor primeiro o cadastro e explicar que os próximos passos vêm depois da confirmação.
6. **Continuação após confirmar (Telegram)**: quando uma ação é confirmada pelo botão e termina `EXECUTED`, o processador
   do Telegram chama o Leo na mesma conversa com uma mensagem marcada como do sistema:
   `"[Sistema] Ação confirmada e executada: <descrição> (<entityType> <entityId>). Continue o pedido anterior do usuário,
   se ainda houver passos pendentes; se não houver, responda apenas 'Tudo certo.'"`
   A resposta e as novas propostas vão para o mesmo chat com botões, como em uma mensagem normal. Não dispara para
   CANCELLED/FAILED. A mensagem do usuário continua chegando ao Leo sem alteração; esta é uma mensagem separada.

### Etapa B — Marcar consulta (`create_appointment`)
- Leitura nova: `search_professionals` (usuários da clínica que podem ser responsáveis por agenda — reaproveitar o que o
  `WhatsAppSchedulingTools`/`SlotService` usa) e reaproveitar a busca de tipos de consulta.
- Escrita `create_appointment`: argumentos `contactId` (obrigatório, de `search_contacts`), `startAt` (`AAAA-MM-DDTHH:mm`,
  horário de Brasília), `durationMinutes` (padrão: duração do tipo; senão 30), `appointmentTypeId` (opcional),
  `hostId` (opcional; padrão: o próprio usuário se puder ser responsável, senão o único profissional da clínica; se houver
  vários e nenhum informado, erro pedindo para escolher), `title` (padrão: "Consulta - <nome>"), `notes`.
- Validação na proposta: contato existe na empresa; horário no futuro; conflito de agenda — avisar na descrição se o
  horário não estiver livre (usar a mesma verificação do agendamento normal, sem `force`).
- Execução: `AppointmentUseCase.create(companyId, CreateAppointmentRequest, userId)` com `force=false`.
- Permissão: a mesma do endpoint de criação de consulta (conferir no controller; provavelmente `appointment:create`).

### Etapa C — Cadastrar paciente (`create_patient`)
- Escrita `create_patient`: `firstName` (obrigatório), `lastName`, `mobile`, `email`, `birthDate` (`AAAA-MM-DD`), `cpf`,
  `notes`. O nome completo dito pelo usuário é dividido em primeiro nome + sobrenome.
- Validação na proposta: procurar contato com o mesmo nome completo (ou CPF/celular) na empresa; se existir, a descrição
  avisa "Já existe um paciente chamado X" e o modelo deve perguntar antes de duplicar.
- Execução: `ContactUseCase.create(companyId, CreateContactRequest, userId)`.
- Permissão: a do endpoint de criação de contato (provavelmente `contact:create`).

### Etapa D — Lançar procedimento e exame (`create_procedure`)
- Leitura nova `search_catalog`: busca itens do catálogo (`CatalogService.list`, tipo SERVICE, ativos) por nome,
  devolvendo id, nome, categoria e preço.
- Escrita `create_procedure`: `contactId`, `catalogItemId` (de `search_catalog`), `teeth` (lista de dentes FDI, opcional),
  `priceCents` POR DENTE (padrão: preço do catálogo), `state` (`PLANNED` padrão, `DONE` se o usuário disser que já foi
  feito), `performedAt`, `notes`. Uma proposta por pedido: um procedimento com vários dentes vira UMA proposta que, ao
  executar, cria um `PatientProcedure` por dente (ou um com vários dentes, conforme o modelo do odontograma — seguir o
  que a tela do odontograma faz hoje; conferir `PatientProcedureService.create` e o frontend).
- A descrição mostra o total: "10 × Faceta em resina composta (dentes 11–15, 21–25) — R$ 750,00 cada, total R$ 7.500,00".
- **Exames**: não há módulo próprio de exames no CRM. Decisão: exame é um item de catálogo (SERVICE, categoria com
  "exame") lançado pela mesma ferramenta, sem dentes. Se não existir no catálogo, o Leo informa que precisa cadastrar o
  item no catálogo primeiro (não cria item de catálogo).
- Sem financeiro automático: `generateTransaction=false` (o lançamento financeiro continua pela ferramenta
  `create_transaction` existente, se o usuário pedir).
- Permissão: a do endpoint de procedimentos (`contact:update`, conferir).

## Regras comuns
- Padrão das write tools existentes (`CancelAppointmentTool`): validar na proposta, persistir via
  `AiActionService.propose`, executar no `AiActionExecutor` lendo os parâmetros persistidos.
- `CRM_ADMIN` passa em toda guarda.
- Textos ao usuário em pt-BR; descrições de proposta legíveis.
- Testes unitários por ferramenta (proposta válida, validação recusada, execução chama o caso de uso certo).
- O usuário roda os testes de backend (`mvn test`); agentes nunca rodam Maven.
