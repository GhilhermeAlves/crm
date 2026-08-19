package com.becommerce.crm.application.ai.context;

/**
 * Contexto da aplicação (AI-02): em qual módulo, tela e rota o usuário está.
 * Derivado das dicas enviadas pelo frontend ({@code screen}/{@code route});
 * usado para orientar o assistente sobre o que o usuário vê. Não carrega dados
 * sensíveis — apenas posição de navegação.
 */
public record AiApplicationContext(
        String module,
        String screen,
        String route) {

    public static AiApplicationContext of(String screen, String route) {
        String normalizedScreen = screen == null ? "" : screen;
        String normalizedRoute = route == null ? "" : route;
        return new AiApplicationContext(
                moduleOf(normalizedScreen, normalizedRoute),
                normalizedScreen,
                normalizedRoute);
    }

    /** Inferência simples de módulo a partir da rota/tela do frontend. */
    private static String moduleOf(String screen, String route) {
        String haystack = (route + " " + screen).toLowerCase();
        if (haystack.contains("dashboard")) return "DASHBOARD";
        if (haystack.contains("contact") || haystack.contains("customer")) return "CUSTOMER";
        if (haystack.contains("opportunit") || haystack.contains("pipeline")) return "PIPELINE";
        if (haystack.contains("activity")) return "ACTIVITY";
        if (haystack.contains("task")) return "TASK";
        if (haystack.contains("report")) return "REPORT";
        if (haystack.contains("audit")) return "AUDIT";
        if (haystack.contains("settings")) return "SETTINGS";
        return "GENERAL";
    }
}