package com.becommerce.crm.automation.ai.application.context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catálogo das telas do CRM para o assistente Léo: rota → nome exibido no menu
 * e o que o usuário faz ali. Mantido em sincronia com
 * {@code frontend-refine/src/components/layout/navigation.ts}.
 */
public final class AiScreenCatalog {

    public record Screen(String name, String purpose) {
    }

    private static final Map<String, Screen> SCREENS = new LinkedHashMap<>();

    static {
        add("/crm", "Início", "painel com os indicadores do dia");
        add("/leads", "Leads", "novos interessados");
        add("/contacts", "Contatos", "cadastro de pacientes/clientes");
        add("/pipeline", "Pipeline", "funil de oportunidades");
        add("/agenda", "Agenda", "consultas: marcar, confirmar, desmarcar e remarcar");
        add("/tasks", "Tarefas", "tarefas e follow-ups da equipe");
        add("/catalog", "Catálogo", "serviços e produtos");
        add("/activities", "Timeline", "histórico de atividades");
        add("/workflows", "Automações", "regras automáticas");
        add("/assistant", "Léo · Assistente IA", "conversas com o Léo");
        add("/inbox", "Inbox", "conversas de WhatsApp com pacientes");
        add("/channels", "Canais", "conexão do WhatsApp e outros canais");
        add("/follow-up-sequences", "Sequências de Follow-up", "mensagens automáticas em sequência");
        add("/campaigns", "Campanhas", "campanhas de mensagens");
        add("/members", "Membros", "membros da empresa");
        add("/finance/control", "Controle financeiro", "receitas, despesas e contas a pagar/receber");
        add("/finance/reconciliation", "Conciliação", "conciliação bancária");
        add("/finance/commissions", "Comissões", "comissões dos profissionais");
        add("/finance", "Gestão financeira", "módulo financeiro");
        add("/reports", "Relatórios", "relatórios da clínica (visão geral, agenda, fluxo de caixa, receitas)");
        add("/storage", "Arquivos", "arquivos enviados");
        add("/audit", "Auditoria", "registro de alterações");
        add("/settings/company", "Minha Empresa", "dados, preferências e usuários da empresa");
        add("/settings/agenda", "Config. Agenda", "disponibilidade, notificações e mensagem de aniversário");
        add("/settings/agent-config", "Agente de IA", "configuração do agente do WhatsApp");
        add("/settings/documents", "Documentos", "modelos de documentos, anamnese e receituários");
        add("/settings/roles", "Perfis", "perfis de acesso");
        add("/profile", "Meu perfil", "dados do usuário");
    }

    private AiScreenCatalog() {
    }

    private static void add(String route, String name, String purpose) {
        SCREENS.put(route, new Screen(name, purpose));
    }

    /** Tela da rota pelo prefixo mais longo; {@code null} se desconhecida. */
    public static Screen find(String route) {
        if (route == null || route.isBlank()) {
            return null;
        }
        String best = null;
        for (String candidate : SCREENS.keySet()) {
            boolean matches = route.equals(candidate) || route.startsWith(candidate + "/");
            if (matches && (best == null || candidate.length() > best.length())) {
                best = candidate;
            }
        }
        return best == null ? null : SCREENS.get(best);
    }

    /** Lista das telas para o prompt do assistente. */
    public static String render() {
        StringBuilder sb = new StringBuilder("Telas do CRM (nome — rota: para que serve):");
        SCREENS.forEach((route, screen) -> sb.append("\n- ").append(screen.name()).append(" — ")
                .append(route).append(": ").append(screen.purpose()));
        return sb.toString();
    }
}
