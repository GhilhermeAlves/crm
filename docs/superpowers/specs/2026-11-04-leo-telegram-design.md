# Leo pelo Telegram — design

Data: 2026-11-04 · Branch: `feat/leo-telegram`

## Objetivo

Falar com o Leo (assistente de IA do CRM) pelo Telegram, por texto ou áudio, sem abrir o CRM.
O Leo passa a atuar como secretário do usuário: tudo o que ele já faz no chat do CRM fica
disponível no Telegram.

## Decisões tomadas

- **Sem Hermes e sem segunda IA.** O bot é só um canal: o texto chega ao Leo palavra por palavra.
  Só o Leo interpreta. O Hermes deixa de fazer parte desta integração.
- **O bot roda no próprio backend do CRM** (webhook na VPS e no domínio atuais). Não há hospedagem nova.
- **Vínculo por pessoa.** Cada usuário do CRM liga o próprio Telegram. O Leo age com as permissões
  e a auditoria desse usuário. Não há usuário técnico nem chave de API.
- **Confirmação antes de gravar.** Usa o fluxo existente `AiAction` (`PROPOSED → CONFIRMED → EXECUTED`),
  com botões no Telegram.
- **Áudio** é transcrito pelo `AiMediaProvider.transcribe` que já existe (OpenAI). É o único custo novo:
  cerca de US$ 0,006 por minuto. O texto transcrito é mostrado ao usuário.
- **Uma clínica por bot.** O token é configurado por clínica (tenant).

## Fora do escopo (entregas seguintes, cada uma com a própria spec)

1. Leo marcar consulta (reaproveitando `SlotService` e as regras de `WhatsAppSchedulingTools`).
2. Leo cadastrar paciente.
3. Leo lançar procedimento e exame.

Assim que existirem como ferramentas do Leo, essas ações aparecem no Telegram sem nenhuma mudança no canal.

## Componentes

### 1. Configuração do bot (por clínica)
- Tela em Configurações → Integrações → Telegram, acessível a quem tem permissão de integrações e ao `CRM_ADMIN`.
- O administrador cola o token do @BotFather. O backend:
  - valida o token chamando `getMe`;
  - gera um `webhook_secret` aleatório;
  - chama `setWebhook` com `https://<domínio>/api/v1/telegram/webhook/{companyId}` e o `secret_token`;
  - grava o token criptografado, no mesmo padrão das credenciais do WhatsApp/Evolution.
- O administrador pode desativar a integração, o que remove o webhook e apaga o token.

### 2. Vínculo de usuário
- No perfil, "Conectar Telegram" gera um código de 6 dígitos de uso único, válido por 10 minutos e guardado como hash.
- No bot, `/vincular 123456` grava o vínculo `telegram_user_id ↔ user_id ↔ company_id`.
- `/desvincular` remove o vínculo. O administrador também pode revogar vínculos pela tela de integração.
- Mensagem de quem não tem vínculo recebe "Conta não vinculada. Gere o código no seu perfil do CRM."
  e não chega ao Leo.
- Usuário desativado ou removido da clínica perde o acesso, porque o vínculo é validado a cada mensagem.

### 3. Recebimento de mensagens
- `POST /api/v1/telegram/webhook/{companyId}` é público (`permitAll`), mas exige que o header
  `X-Telegram-Bot-Api-Secret-Token` seja igual ao segredo da clínica. Sem isso a resposta é 401, e nada é processado.
- O endpoint responde 200 na hora e processa de forma assíncrona, para o Telegram não reenviar.
- Idempotência: o `update_id` já processado é ignorado.
- **Texto:** é repassado sem alteração para `AiAssistantUseCase.chat(companyId, userId, permissions, request)`.
  As permissões são as do usuário vinculado, carregadas do CRM.
- **Áudio (voice):** `getFile`, download e `AiMediaProvider.transcribe` geram o texto, que vai ao Leo.
  A resposta começa com `🎙️ Entendi: "<texto>"`.
- **Outros tipos** (foto, documento): a resposta é "Por enquanto entendo texto e áudio."
- **Conversa:** um id de conversa do Leo é mantido por vínculo. `/nova` inicia outra conversa.
- O processamento roda com o tenant da clínica definido (RLS), como no fluxo do WhatsApp.

### 4. Confirmação de ações
- Quando a resposta do Leo traz uma `AiAction` `PROPOSED`, o bot envia o resumo com teclado inline:
  `✅ Confirmar` (`callback_data = c:<actionId>`) e `❌ Cancelar` (`x:<actionId>`).
- No `callback_query`, chama `AiActionUseCase.confirm/cancel` com o usuário vinculado ao remetente do clique.
  O caso de uso já recusa ações de outro usuário.
- Depois de clicar, a mensagem é editada com o resultado ("✅ Feito: …" ou "❌ Cancelado"), e os botões são removidos.

### 5. Envio de respostas
- O cliente `TelegramBotClient` cobre `sendMessage`, `editMessageText`, `answerCallbackQuery`, `getFile`,
  `setWebhook`, `deleteWebhook` e `getMe`.
- Mensagens acima de 4096 caracteres são divididas.
- Formatação em texto simples, para evitar erro de parse de Markdown.

## Dados (migration nova)

- `telegram_bot_config` (`company_id` PK, `bot_token_enc`, `webhook_secret_hash`, `bot_username`, `enabled`, datas).
- `telegram_link` (`id`, `company_id`, `user_id`, `telegram_user_id`, `chat_id`, `ai_conversation_id`, `created_at`),
  com unicidade em (`company_id`, `telegram_user_id`) e em (`company_id`, `user_id`).
- `telegram_link_code` (`code_hash`, `company_id`, `user_id`, `expires_at`, `used_at`).
- Idempotência: `telegram_update_seen` (`company_id`, `update_id`), com limpeza periódica.
- Todas as tabelas com RLS por `company_id` e GRANT, conforme o padrão do projeto. Antes de criar, checar
  colisão de nome e número de versão.

## Segurança

- Webhook validado pelo segredo e com limite de requisições por `telegram_user_id`
  (por exemplo, 20 mensagens por minuto).
- Token do bot nunca é devolvido pela API nem gravado em log.
- A auditoria das ações do Leo registra a origem `telegram`.
- `CRM_ADMIN` sempre pode configurar e revogar.

## Erros

- Falha do Leo ou da OpenAI: o usuário recebe "Não consegui processar agora, tente de novo." e o erro vai para o log.
- Falha na transcrição: o usuário recebe "Não entendi o áudio, pode repetir ou escrever?"
- Telegram fora do ar no envio: o envio é tentado de novo até 3 vezes, depois só log.

## Testes

- Webhook sem segredo ou com segredo errado: 401, sem processar.
- `update_id` repetido: ignorado.
- Remetente sem vínculo: recebe orientação e o Leo não é chamado.
- Código vencido, já usado ou de outra clínica: recusado.
- Texto chega ao Leo idêntico ao enviado.
- Áudio: o texto transcrito é repassado sem alteração e aparece na resposta.
- Callback de outro usuário não confirma a ação.
- Usuário desativado: bloqueado mesmo com vínculo.
- Frontend: tela de configuração e botão de vínculo, com Prettier verificado.
