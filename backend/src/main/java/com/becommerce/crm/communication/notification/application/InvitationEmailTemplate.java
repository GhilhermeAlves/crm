package com.becommerce.crm.communication.notification.application;

import java.util.Map;

/**
 * Templates de convite. O visual da PG Automação ({@link #html}) é exclusivo da
 * conta de envio dela ({@code app.mail.pg-branded-senders}); as demais empresas
 * usam o convite simples ({@link #plainHtml}) até terem template próprio.
 *
 * <p>E-mail de convite com a identidade da PG Automação: fundo branco, cabeçalho
 * escuro com o logo, marca d'água suave no corpo e passo a passo para abrir a
 * conta. Usado pelo SMTP padrão e pela conta de e-mail da empresa.
 *
 * <p>HTML de e-mail: só tabelas e estilos inline (Gmail/Outlook ignoram
 * {@code <style>}). A marca d'água usa {@code background} na célula do corpo;
 * clientes que não suportam (Outlook desktop) apenas não a exibem.
 *
 * <p>As imagens ficam em {@code frontend-refine/public/brand} e são servidas
 * pelo frontend ({@code app.mail.frontend-base-url}).
 */
public final class InvitationEmailTemplate {

    private static final Map<String, String> ROLE_LABEL = Map.of(
            "ADMIN", "Administrador",
            "MANAGER", "Gerente",
            "AGENT", "Agente",
            "VIEWER", "Visualizador");

    private InvitationEmailTemplate() {
    }

    /**
     * Convite simples (sem marca), usado pelas empresas que não têm template
     * próprio. {@code companyName} vazio = convite para abrir uma nova empresa.
     */
    public static String plainSubject(String companyName) {
        return isBlank(companyName) ? "Você foi convidado para abrir sua empresa no CRM"
                : "Você foi convidado para " + companyName;
    }

    public static String plainHtml(String companyName, String role, String url) {
        String target = isBlank(companyName) ? "abrir a conta da sua empresa no CRM"
                : "ingressar na empresa <strong>" + escape(companyName) + "</strong>"
                  + (isBlank(role) ? "" : " com o papel <strong>" + escape(role) + "</strong>");
        return """
                <p>Olá!</p>
                <p>Você foi convidado para %s.</p>
                <p><a href="%s">Aceitar convite</a></p>
                <p>Se não foi você, ignore este e-mail.</p>
                """.formatted(target, escape(url));
    }

    /**
     * Assunto do convite. {@code companyName} vazio indica o convite para abrir
     * uma nova empresa (convite de empresa); preenchido, o convite de membro.
     */
    public static String subject(String companyName) {
        return isBlank(companyName)
                ? "Bem-vindo(a) ao CRM PG Automação: abra a conta da sua empresa"
                : "Bem-vindo(a) ao CRM PG Automação: seu acesso a " + companyName;
    }

    public static String html(String frontendBaseUrl, String companyName, String role, String url) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
        String logo = base + "/brand/pg-automacao-email.png";
        String watermark = base + "/brand/pg-automacao-watermark.png";
        String link = escape(url);
        boolean newCompany = isBlank(companyName);

        String intro = newCompany
                ? "Que alegria ter você com a gente! Seu convite para abrir a conta da sua empresa no "
                  + "<strong>CRM da PG Automação</strong> está pronto. Em poucos minutos você cadastra a "
                  + "empresa, cria seu acesso e já começa a organizar atendimentos, clientes e vendas "
                  + "em um só lugar."
                : "Que alegria ter você com a gente! Você foi convidado(a) para fazer parte da equipe de "
                  + "<strong>" + escape(companyName) + "</strong> no <strong>CRM da PG Automação</strong>"
                  + roleText(role) + ". A partir de agora, atendimentos, clientes e vendas ficam "
                  + "organizados em um só lugar, com você no comando.";

        String step2 = newCompany
                ? "Confira os dados da sua empresa já preenchidos e complete o que faltar."
                : "Confira o convite: o nome da empresa e o seu perfil de acesso.";

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Convite CRM PG Automação</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f1f4f9;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;">Seu acesso ao CRM da PG Automação está pronto. Veja como começar em 4 passos.</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f1f4f9;">
                <tr><td align="center" style="padding:32px 12px;">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0"
                    style="width:100%%;
                      max-width:600px;
                      background-color:#ffffff;
                      border-radius:16px;
                      overflow:hidden;
                      box-shadow:0 4px 24px rgba(10,14,23,0.08);
                      font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
                    <tr>
                      <td align="center" style="background-color:#080b12;padding:28px 24px 22px 24px;">
                        <img src="%1$s" width="200" alt="PG Automação" style="display:block;width:200px;max-width:60%%;height:auto;border:0;">
                      </td>
                    </tr>
                    <tr><td style="height:4px;line-height:4px;font-size:0;background-color:#1a73ff;background-image:linear-gradient(90deg,#0b5cff,#19b6ff);">&nbsp;</td></tr>
                    <tr>
                      <td background="%2$s"
                        style="background-color:#ffffff;
                          background-image:url('%2$s');
                          background-repeat:no-repeat;
                          background-position:center 150px;
                          background-size:360px auto;
                          padding:40px 44px 8px 44px;
                          color:#1f2937;">
                        <h1 style="margin:0 0 16px 0;font-size:26px;line-height:1.3;color:#080b12;font-weight:700;">Seja muito bem-vindo(a)! 🎉</h1>
                        <p style="margin:0 0 16px 0;font-size:16px;line-height:1.65;color:#374151;">Olá!</p>
                        <p style="margin:0 0 28px 0;font-size:16px;line-height:1.65;color:#374151;">%3$s</p>
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 32px auto;">
                          <tr><td align="center" style="border-radius:10px;background-color:#0b5cff;">
                            <a href="%4$s" target="_blank"
                              style="display:inline-block;
                                padding:15px 36px;
                                font-size:16px;
                                font-weight:700;
                                color:#ffffff;
                                text-decoration:none;
                                border-radius:10px;">Criar minha conta</a>
                          </td></tr>
                        </table>
                        <h2 style="margin:0 0 16px 0;font-size:18px;color:#080b12;font-weight:700;">Como começar em 4 passos</h2>
                        %5$s
                        %6$s
                        %7$s
                        %8$s
                        <p style="margin:24px 0 0 0;padding:14px 16px;background-color:#f3f7ff;border-left:4px solid #0b5cff;border-radius:6px;font-size:14px;line-height:1.6;color:#374151;">
                          <strong>Dica:</strong> já tem conta no CRM com este e-mail? Basta clicar em <strong>Entrar</strong> na página do convite e aceitá-lo.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:24px 44px 32px 44px;color:#6b7280;font-size:13px;line-height:1.6;">
                        <p style="margin:0 0 8px 0;">O botão não funcionou? Copie e cole este endereço no navegador:</p>
                        <p style="margin:0 0 20px 0;word-break:break-all;"><a href="%4$s" style="color:#0b5cff;text-decoration:underline;">%4$s</a></p>
                        <p style="margin:0;">O convite é pessoal e tem prazo de validade. Se você não esperava este e-mail, pode ignorá-lo com segurança.</p>
                      </td>
                    </tr>
                    <tr>
                      <td align="center" style="background-color:#080b12;padding:18px 24px;color:#9ca3af;font-size:12px;line-height:1.6;">
                        <span style="color:#ffffff;font-weight:600;letter-spacing:2px;">PG AUTOMAÇÃO</span><br>
                        Automação e CRM para o seu negócio crescer.
                      </td>
                    </tr>
                  </table>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(
                logo, watermark, intro, link,
                step(1, "Clique em <strong>Criar minha conta</strong>", "O botão acima abre a página do seu convite."),
                step(2, "Confira os dados", step2),
                step(3, "Crie seu acesso", "Informe seu nome e escolha uma senha segura, com pelo menos 8 caracteres."),
                step(4, "Entre no CRM", "Faça login com este e-mail e a senha criada. Pronto, é só começar!"));
    }

    private static String step(int number, String title, String text) {
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 14px 0;">
                  <tr>
                    <td width="40" valign="top" style="width:40px;padding-top:2px;">
                      <div style="width:30px;height:30px;line-height:30px;border-radius:15px;background-color:#0b5cff;color:#ffffff;font-size:14px;font-weight:700;text-align:center;">%d</div>
                    </td>
                    <td valign="top" style="font-size:15px;line-height:1.55;color:#374151;">
                      <strong style="color:#080b12;">%s</strong><br>%s
                    </td>
                  </tr>
                </table>""".formatted(number, title, text);
    }

    private static String roleText(String role) {
        if (isBlank(role)) {
            return "";
        }
        return ", com o perfil <strong>" + escape(ROLE_LABEL.getOrDefault(role, role)) + "</strong>";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
