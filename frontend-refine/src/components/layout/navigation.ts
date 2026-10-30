import type { LucideIcon } from "lucide-react";
import {
  Smartphone,
  Mail,
  Users,
  Contact,
  GitBranch,
  MessageSquare,
  Megaphone,
  BarChart3,
  Building2,
  ClipboardList,
  Package,
  Boxes,
  HardDrive,
  Workflow as WorkflowIcon,
  Sparkles,
  Home,
  Bot,
  Palette,
  Shield,
  UsersRound,
  MailPlus,
  CalendarDays,
  FileText,
  CircleDollarSign,
  LayoutDashboard,
  Briefcase,
  Headphones,
  Wrench,
  LineChart,
  Settings,
  ServerCog,
  HeartPulse,
} from "lucide-react";
import { ROUTES } from "@/lib/constants";
import type { CompanyModuleName } from "@/features/identity/modules/types/modules.types";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  badge?: string;
  permission?: string;
  /** Módulo da empresa exigido (V104); some do menu quando desligado. */
  module?: CompanyModuleName;
  /** Visível só para o dono da plataforma (CRM_ADMIN), nunca para as empresas clientes. */
  crmAdminOnly?: boolean;
  children?: { label: string; href: string; permission?: string; module?: CompanyModuleName }[];
}

export interface NavGroup {
  title?: string;
  /** Ícone do grupo: aparece no título e, com a barra recolhida, abre o menu do grupo. */
  icon?: LucideIcon;
  items: NavItem[];
}

export const NAVIGATION: NavGroup[] = [
  {
    title: "Principal",
    icon: LayoutDashboard,
    items: [
      { label: "Início", href: ROUTES.CRM, icon: Home },
      {
        label: "Agenda",
        href: ROUTES.AGENDA,
        icon: CalendarDays,
        permission: "appointment:read",
        module: "AGENDA",
      },
      {
        label: "Pacientes",
        href: ROUTES.PATIENTS,
        icon: HeartPulse,
        permission: "contact:read",
        module: "PATIENTS",
      },
      {
        label: "Financeiro",
        href: ROUTES.FINANCE_CONTROL,
        icon: CircleDollarSign,
        permission: "finance:read",
        module: "FINANCE",
        children: [
          { label: "Controle financeiro", href: ROUTES.FINANCE_CONTROL },
          { label: "Conciliação", href: ROUTES.FINANCE_RECONCILIATION },
          { label: "Comissões", href: ROUTES.FINANCE_COMMISSIONS },
        ],
      },
      {
        label: "Tarefas",
        href: ROUTES.TASKS,
        icon: ClipboardList,
        permission: "task:page:view",
        module: "TASKS",
      },
      {
        label: "Léo · Assistente IA",
        href: ROUTES.ASSISTANT,
        icon: Sparkles,
        permission: "ai:chat",
        module: "ASSISTANT",
      },
    ],
  },
  {
    title: "Comercial",
    icon: Briefcase,
    items: [
      {
        label: "Leads",
        href: ROUTES.LEADS,
        icon: Users,
        permission: "lead:page:view",
        module: "CRM",
      },
      {
        label: "Contatos",
        href: ROUTES.CONTACTS,
        icon: Contact,
        permission: "contact:page:view",
        module: "CRM",
      },
      {
        label: "Pipeline",
        href: ROUTES.PIPELINE,
        icon: GitBranch,
        permission: "pipeline:page:view",
        module: "CRM",
      },
      {
        label: "Histórico",
        href: ROUTES.ACTIVITIES,
        icon: MailPlus,
        permission: "activity:page:view",
        module: "CRM",
      },
    ],
  },
  {
    title: "Atendimento",
    icon: Headphones,
    items: [
      {
        label: "Inbox",
        href: ROUTES.INBOX,
        icon: MessageSquare,
        permission: "omnichannel:page:view",
        module: "INBOX",
      },
      {
        label: "Canais",
        href: ROUTES.CHANNELS,
        icon: Megaphone,
        permission: "omnichannel:page:view",
        module: "INBOX",
      },
      {
        label: "Follow-ups",
        href: ROUTES.FOLLOW_UP_SEQUENCES,
        icon: ClipboardList,
        permission: "omnichannel:followup:sequence:read",
        module: "FOLLOW_UPS",
      },
      {
        label: "Campanhas",
        href: ROUTES.CAMPAIGNS,
        icon: Megaphone,
        permission: "campaign:page:view",
        module: "CAMPAIGNS",
      },
      {
        label: "Automações",
        href: ROUTES.WORKFLOWS,
        icon: WorkflowIcon,
        permission: "workflow:page:view",
        module: "WORKFLOWS",
      },
    ],
  },
  {
    title: "Operação",
    icon: Wrench,
    items: [
      {
        label: "Catálogo",
        href: ROUTES.CATALOG,
        icon: Package,
        permission: "catalog:read",
        module: "CATALOG",
      },
      {
        label: "Estoque",
        href: ROUTES.INVENTORY,
        icon: Boxes,
        permission: "inventory:read",
        module: "INVENTORY",
      },
    ],
  },
  {
    title: "Análise",
    icon: LineChart,
    items: [
      {
        label: "Relatórios",
        href: ROUTES.REPORTS,
        icon: BarChart3,
        module: "REPORTS",
        children: [
          { label: "Painel de relatórios", href: ROUTES.REPORTS },
          {
            label: "Contábil mensal (DRE)",
            href: `${ROUTES.REPORTS_EXPORT}/monthly-dre`,
            permission: "finance:read",
          },
          {
            label: "Fluxo de caixa",
            href: `${ROUTES.REPORTS_EXPORT}/cash-flow`,
            permission: "finance:read",
          },
          {
            label: "Extrato de lançamentos",
            href: `${ROUTES.REPORTS_EXPORT}/transactions`,
            permission: "finance:read",
          },
          {
            label: "Agenda / atendimentos",
            href: `${ROUTES.REPORTS_EXPORT}/agenda`,
            permission: "appointment:read",
          },
        ],
      },
      { label: "Arquivos", href: ROUTES.STORAGE, icon: HardDrive, module: "STORAGE" },
    ],
  },
  {
    title: "Configurações",
    icon: Settings,
    items: [
      {
        label: "Minha Empresa",
        href: ROUTES.SETTINGS_COMPANY,
        icon: Building2,
        children: [
          { label: "Dados da empresa", href: ROUTES.SETTINGS_COMPANY },
          { label: "Preferências", href: `${ROUTES.SETTINGS_COMPANY}/preferences` },
          {
            label: "Lista de serviços",
            href: `${ROUTES.SETTINGS_COMPANY}/services`,
            module: "SERVICE_LIST",
          },
          {
            label: "Migração de dados",
            href: `${ROUTES.SETTINGS_COMPANY}/migration`,
            module: "DATA_MIGRATION",
          },
        ],
      },
      {
        label: "Equipe",
        href: ROUTES.SETTINGS_COMPANY_USERS,
        icon: UsersRound,
        permission: "user:read",
      },
      {
        label: "Perfis e Permissões",
        href: ROUTES.SETTINGS_ROLES,
        icon: Shield,
        permission: "security:page:view",
        module: "ROLES",
      },
      {
        label: "Agenda",
        href: ROUTES.SETTINGS_AGENDA,
        icon: CalendarDays,
        permission: "scheduling:configure",
        module: "AGENDA_SETTINGS",
        children: [
          { label: "Ajustes gerais", href: ROUTES.SETTINGS_AGENDA },
          { label: "Disponibilidade", href: `${ROUTES.SETTINGS_AGENDA}/availability` },
          { label: "Cadeiras", href: `${ROUTES.SETTINGS_AGENDA}/chairs` },
          { label: "Marcadores", href: `${ROUTES.SETTINGS_AGENDA}/tags` },
          { label: "Feriados", href: `${ROUTES.SETTINGS_AGENDA}/holidays` },
          { label: "Notificações", href: `${ROUTES.SETTINGS_AGENDA}/notifications` },
          { label: "Aniversários", href: `${ROUTES.SETTINGS_AGENDA}/birthday` },
        ],
      },
      {
        label: "WhatsApp",
        href: ROUTES.SETTINGS_WHATSAPP,
        icon: Smartphone,
        permission: "omnichannel:update",
        module: "INBOX",
      },
      {
        label: "E-mail",
        href: ROUTES.SETTINGS_EMAIL,
        icon: Mail,
        permission: "company:update",
        module: "EMAIL_SETTINGS",
      },
      {
        label: "Agente de IA",
        href: ROUTES.SETTINGS_AGENT_CONFIG,
        icon: Bot,
        permission: "ai:agent-config",
        module: "AI_AGENT",
      },
      {
        label: "Documentos",
        href: ROUTES.SETTINGS_DOCUMENTS,
        icon: FileText,
        module: "DOCUMENTS",
        children: [
          { label: "Modelos de documentos", href: ROUTES.SETTINGS_DOCUMENTS },
          { label: "Modelos de anamnese", href: `${ROUTES.SETTINGS_DOCUMENTS}/anamnesis` },
          { label: "Receituários", href: `${ROUTES.SETTINGS_DOCUMENTS}/prescriptions` },
        ],
      },
    ],
  },
  {
    title: "Sistema",
    icon: ServerCog,
    items: [
      { label: "Empresas", href: ROUTES.TENANTS, icon: Building2, crmAdminOnly: true },
      {
        label: "Auditoria",
        href: ROUTES.AUDIT,
        icon: ClipboardList,
        permission: "audit:page:view",
        module: "AUDIT",
      },
      // Ferramenta de desenvolvimento: fora do menu em produção.
      ...(process.env.NODE_ENV === "production"
        ? []
        : [{ label: "Design System", href: ROUTES.DESIGN_SYSTEM, icon: Palette }]),
    ],
  },
];
