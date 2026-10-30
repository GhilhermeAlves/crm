package com.becommerce.crm.masterdata.company.domain;

/**
 * Módulos que o CRM_ADMIN liga/desliga por empresa (V104). Pacientes não
 * depende de CRM: uma clínica pode desligar o comercial e manter a ficha.
 */
public enum CompanyModule {
    /** Comercial: contatos, funil, oportunidades, leads. */
    CRM,
    /** Lista e ficha de pacientes (área da saúde). */
    PATIENTS,
    /** Agenda, tipos de consulta, cadeiras, bloqueios, retornos e feriados. */
    AGENDA,
    /** Financeiro. */
    FINANCE,
    /** Tarefas. */
    TASKS,
    /** Léo · Assistente IA. */
    ASSISTANT,
    /** Inbox e Canais. */
    INBOX,
    /** Follow-ups. */
    FOLLOW_UPS,
    /** Campanhas. */
    CAMPAIGNS,
    /** Automações. */
    WORKFLOWS,
    /** Catálogo. */
    CATALOG,
    /** Estoque. */
    INVENTORY,
    /** Relatórios. */
    REPORTS,
    /** Arquivos. */
    STORAGE,
    /** Configurações da agenda (ajustes, disponibilidade, cadeiras, feriados...). */
    AGENDA_SETTINGS,
    /** Conta de e-mail da clínica. */
    EMAIL_SETTINGS,
    /** Configuração do agente de IA do atendimento. */
    AI_AGENT,
    /** Modelos de documentos, anamnese e receituários. */
    DOCUMENTS,
    /** Lista de serviços da empresa. */
    SERVICE_LIST,
    /** Migração de dados (importação). */
    DATA_MIGRATION,
    /** Perfis e permissões. */
    ROLES,
    /** Auditoria. */
    AUDIT
}
