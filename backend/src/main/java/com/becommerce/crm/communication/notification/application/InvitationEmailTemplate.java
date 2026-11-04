package com.becommerce.crm.communication.notification.application;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Templates do e-mail de convite. Três variações:
 * <ul>
 *   <li><b>Personalizado</b> ({@link #customHtml}): modelo salvo pela empresa em
 *   Configurações &gt; E-mail (logo, cor, título, mensagem e rodapé).</li>
 *   <li><b>PG Automação</b> ({@link #html}): identidade da PG, exclusiva da conta
 *   de envio dela ({@code app.mail.pg-branded-senders}).</li>
 *   <li><b>Simples</b> ({@link #plainHtml}): empresas sem modelo próprio.</li>
 * </ul>
 * O passo a passo e o link do convite são sempre gerados aqui, para ninguém
 * quebrar o fluxo de criação da conta ao editar o modelo.
 *
 * <p>HTML de e-mail: só tabelas e estilos inline (Gmail/Outlook ignoram
 * {@code <style>}). Todo texto vindo da empresa é escapado.
 */
public final class InvitationEmailTemplate {

    /** Cor padrão do modelo personalizado. */
    public static final String DEFAULT_COLOR = "#0b5cff";
    public static final String DEFAULT_TITLE = "Seja muito bem-vindo(a)!";

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private static final Map<String, String> ROLE_LABEL = Map.of(
            "ADMIN", "Administrador",
            "MANAGER", "Gerente",
            "AGENT", "Agente",
            "VIEWER", "Visualizador");

    /** Campos do modelo personalizado (todos opcionais; vazio usa o padrão). */
    public record Custom(String logoSrc, String color, String title, String message, String footer) {
    }

    /** Identidade visual aplicada ao layout comum. */
    private record Brand(String docTitle, String preheader, String logoSrc, String logoAlt, boolean darkHeader,
                         String accent, String title, String intro,
                         String footerTitle, String footerText) {
    }

    private InvitationEmailTemplate() {
    }

    // ---------------------------------------------------------------- simples

    /**
     * Convite simples (sem marca). {@code companyName} vazio = convite para
     * abrir uma nova empresa.
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

    // ------------------------------------------------------------ PG Automação

    /**
     * Assunto do convite da PG. {@code companyName} vazio indica o convite para
     * abrir uma nova empresa; preenchido, o convite de membro.
     */
    public static String subject(String companyName) {
        return isBlank(companyName)
                ? "Bem-vindo(a) ao CRM PG Automação: abra a conta da sua empresa"
                : "Bem-vindo(a) ao CRM PG Automação: seu acesso a " + companyName;
    }

    /** Convite com a identidade da PG; imagens servidas pelo frontend em /brand. */
    public static String html(String frontendBaseUrl, String companyName, String role, String url) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
        Brand pg = new Brand(
                "Convite CRM PG Automação",
                "Seu acesso ao CRM da PG Automação está pronto. Veja como começar em 4 passos.",
                base + "/brand/pg-automacao-email.png", "PG Automação", true,
                DEFAULT_COLOR, DEFAULT_TITLE + " 🎉",
                defaultIntro(companyName, role, "<strong>CRM da PG Automação</strong>"),
                "PG AUTOMAÇÃO", "Automação e CRM para o seu negócio crescer.");
        return render(pg, companyName, url);
    }

    // ----------------------------------------------------------- personalizado

    public static String customSubject(String senderName, String companyName) {
        String from = isBlank(senderName) ? "CRM" : senderName.trim();
        return isBlank(companyName)
                ? "Bem-vindo(a) a " + from + ": abra a conta da sua empresa"
                : "Bem-vindo(a) a " + from + ": seu acesso a " + companyName;
    }

    /**
     * Convite com o modelo da empresa. {@code custom.logoSrc} é a origem da
     * imagem já pronta: {@code cid:...} no envio real ou data URL na prévia.
     * Na mensagem, {@code {empresa}} e {@code {perfil}} são substituídos.
     */
    public static String customHtml(Custom custom, String senderName, String companyName, String role,
                                    String url) {
        String brandName = isBlank(senderName) ? "" : senderName.trim();
        String intro = isBlank(custom.message())
                ? defaultIntro(companyName, role, isBlank(brandName) ? "<strong>CRM</strong>"
                        : "CRM de <strong>" + escape(brandName) + "</strong>")
                : messageHtml(custom.message(), companyName, role);
        String footerTitle = brandName.toUpperCase(Locale.ROOT);
        Brand brand = new Brand(
                "Convite " + (isBlank(brandName) ? "CRM" : brandName),
                "Seu acesso está pronto. Veja como começar em 4 passos.",
                isBlank(custom.logoSrc()) ? null : custom.logoSrc(), brandName, false,
                normalizeColor(custom.color()),
                isBlank(custom.title()) ? DEFAULT_TITLE : custom.title().trim(),
                intro,
                footerTitle, isBlank(custom.footer()) ? "" : custom.footer().trim());
        return render(brand, companyName, url);
    }

    /** Cor no formato #RRGGBB; qualquer outro valor vira a cor padrão. */
    public static String normalizeColor(String color) {
        return color != null && HEX_COLOR.matcher(color.trim()).matches()
                ? color.trim().toLowerCase(Locale.ROOT) : DEFAULT_COLOR;
    }

    public static boolean isValidColor(String color) {
        return color != null && HEX_COLOR.matcher(color.trim()).matches();
    }

    // ------------------------------------------------------------------ layout

    private static String defaultIntro(String companyName, String role, String product) {
        return isBlank(companyName)
                ? "Que alegria ter você com a gente! Seu convite para abrir a conta da sua empresa no "
                  + product + " está pronto. Em poucos minutos você cadastra a empresa, cria seu acesso "
                  + "e já começa a organizar atendimentos, clientes e vendas em um só lugar."
                : "Que alegria ter você com a gente! Você foi convidado(a) para fazer parte da equipe de "
                  + "<strong>" + escape(companyName) + "</strong> no " + product + roleText(role)
                  + ". A partir de agora, atendimentos, clientes e vendas ficam organizados em um só "
                  + "lugar, com você no comando.";
    }

    /** Mensagem da empresa: texto escapado, quebras de linha e placeholders. */
    private static String messageHtml(String message, String companyName, String role) {
        String company = isBlank(companyName) ? "sua empresa" : companyName;
        String roleLabel = isBlank(role) ? "" : ROLE_LABEL.getOrDefault(role, role);
        return escape(message.trim())
                .replace("{empresa}", "<strong>" + escape(company) + "</strong>")
                .replace("{perfil}", "<strong>" + escape(roleLabel) + "</strong>")
                .replace("\r\n", "\n")
                .replace("\n", "<br>");
    }

    private static String render(Brand b, String companyName, String url) {
        String link = escape(url);
        String accent = b.accent();
        String soft = tint(accent, 0.92);
        String step2 = isBlank(companyName)
                ? "Confira os dados da sua empresa já preenchidos e complete o que faltar."
                : "Confira o convite: o nome da empresa e o seu perfil de acesso.";

        String headerBg = b.darkHeader() ? "#080b12" : "#ffffff";
        String header = b.logoSrc() == null
                ? (isBlank(b.logoAlt()) ? "" : """
                    <tr>
                      <td align="center" style="background-color:%s;padding:28px 24px;font-size:22px;font-weight:700;color:%s;">%s</td>
                    </tr>""".formatted(headerBg, b.darkHeader() ? "#ffffff" : "#111827", escape(b.logoAlt())))
                : b.darkHeader()
                ? """
                    <tr>
                      <td align="center" style="background-color:#080b12;padding:28px 24px 22px 24px;">
                        <img src="%s" width="200" alt="%s" style="display:block;width:200px;max-width:60%%;height:auto;border:0;">
                      </td>
                    </tr>""".formatted(escape(b.logoSrc()), escape(b.logoAlt()))
                : """
                    <tr>
                      <td align="center" style="background-color:#ffffff;padding:28px 24px 20px 24px;">
                        <img src="%s" height="72" alt="%s" style="display:block;height:72px;width:auto;max-width:80%%;border:0;">
                      </td>
                    </tr>""".formatted(escape(b.logoSrc()), escape(b.logoAlt()));

        String footer = isBlank(b.footerTitle()) && isBlank(b.footerText()) ? "" : b.darkHeader()
                ? """
                    <tr>
                      <td align="center" style="background-color:#080b12;padding:18px 24px;color:#9ca3af;font-size:12px;line-height:1.6;">
                        <span style="color:#ffffff;font-weight:600;letter-spacing:2px;">%s</span><br>%s
                      </td>
                    </tr>""".formatted(escape(b.footerTitle()), escape(b.footerText()))
                : """
                    <tr>
                      <td align="center" style="background-color:%s;padding:18px 24px;color:#4b5563;font-size:12px;line-height:1.6;">
                        <span style="color:%s;font-weight:600;letter-spacing:2px;">%s</span><br>%s
                      </td>
                    </tr>""".formatted(soft, accent, escape(b.footerTitle()), escape(b.footerText()));

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%1$s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f1f4f9;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;">%2$s</div>
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
                %3$s
                    <tr><td style="height:4px;line-height:4px;font-size:0;background-color:%4$s;">&nbsp;</td></tr>
                    <tr>
                      <td style="background-color:#ffffff;padding:40px 44px 8px 44px;color:#1f2937;">
                        <h1 style="margin:0 0 16px 0;font-size:26px;line-height:1.3;color:#080b12;font-weight:700;">%5$s</h1>
                        <p style="margin:0 0 16px 0;font-size:16px;line-height:1.65;color:#374151;">Olá!</p>
                        <p style="margin:0 0 28px 0;font-size:16px;line-height:1.65;color:#374151;">%6$s</p>
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 32px auto;">
                          <tr><td align="center" style="border-radius:10px;background-color:%4$s;">
                            <a href="%7$s" target="_blank"
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
                        %8$s
                        <p style="margin:24px 0 0 0;
                          padding:14px 16px;
                          background-color:%9$s;
                          border-left:4px solid %4$s;
                          border-radius:6px;
                          font-size:14px;
                          line-height:1.6;
                          color:#374151;">
                          <strong>Dica:</strong> já tem conta no CRM com este e-mail? Basta clicar em <strong>Entrar</strong> na página do convite e aceitá-lo.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:24px 44px 32px 44px;color:#6b7280;font-size:13px;line-height:1.6;">
                        <p style="margin:0 0 8px 0;">O botão não funcionou? Copie e cole este endereço no navegador:</p>
                        <p style="margin:0 0 20px 0;word-break:break-all;"><a href="%7$s" style="color:%4$s;text-decoration:underline;">%7$s</a></p>
                        <p style="margin:0;">O convite é pessoal e tem prazo de validade. Se você não esperava este e-mail, pode ignorá-lo com segurança.</p>
                      </td>
                    </tr>
                %10$s
                  </table>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(
                escape(b.docTitle()), escape(b.preheader()), header, accent, escape(b.title()), b.intro(), link,
                step(1, accent, "Clique em <strong>Criar minha conta</strong>", "O botão acima abre a página do seu convite.")
                        + step(2, accent, "Confira os dados", step2)
                        + step(3, accent, "Crie seu acesso",
                                "Informe seu nome e escolha uma senha segura, com pelo menos 8 caracteres.")
                        + step(4, accent, "Entre no CRM",
                                "Faça login com este e-mail e a senha criada. Pronto, é só começar!"),
                soft, footer);
    }

    private static String step(int number, String accent, String title, String text) {
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 14px 0;">
                  <tr>
                    <td width="40" valign="top" style="width:40px;padding-top:2px;">
                      <div style="width:30px;
                        height:30px;
                        line-height:30px;
                        border-radius:15px;
                        background-color:%s;
                        color:#ffffff;
                        font-size:14px;
                        font-weight:700;
                        text-align:center;">%d</div>
                    </td>
                    <td valign="top" style="font-size:15px;line-height:1.55;color:#374151;">
                      <strong style="color:#080b12;">%s</strong><br>%s
                    </td>
                  </tr>
                </table>
                """.formatted(accent, number, title, text);
    }

    /** Mistura a cor com branco ({@code amount} = fração de branco). */
    private static String tint(String hex, double amount) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return String.format("#%02x%02x%02x",
                (int) Math.round(r + (255 - r) * amount),
                (int) Math.round(g + (255 - g) * amount),
                (int) Math.round(b + (255 - b) * amount));
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
