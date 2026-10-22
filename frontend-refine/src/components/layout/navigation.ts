import type { LucideIcon } from "lucide-react";
import {
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
} from "lucide-react";
import { ROUTES } from "@/lib/constants";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  badge?: string;
  permission?: string;
  /** Visível só para o dono da plataforma (SUPER_ADMIN), nunca para as empresas clientes. */
  superAdminOnly?: boolean;
  children?: { label: string; href: string; permission?: string }[];
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
      { label: "Agenda", href: ROUTES.AGENDA, icon: CalendarDays, permission: "appointment:read" },
      { label: "Tarefas", href: ROUTES.TASKS, icon: ClipboardList, permission: "task:page:view" },
      {
        label: "Léo · Assistente IA",
        href: ROUTES.ASSISTANT,
        icon: Sparkles,
        permission: "ai:chat",
      },
    ],
  },
  {
    title: "Comercial",
    icon: Briefcase,
    items: [
      { label: "Leads", href: ROUTES.LEADS, icon: Users, permission: "lead:page:view" },
      { label: "Contatos", href: ROUTES.CONTACTS, icon: Contact, permission: "contact:page:view" },
      {
        label: "Pipeline",
        href: ROUTES.PIPELINE,
        icon: GitBranch,
        permission: "pipeline:page:view",
      },
      {
        label: "Histórico",
        href: ROUTES.ACTIVITIES,
        icon: MailPlus,
        permission: "activity:page:view",
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
      },
      {
        label: "Canais",
        href: ROUTES.CHANNELS,
        icon: Megaphone,
        permission: "omnichannel:page:view",
      },
      {
        label: "Follow-ups",
        href: ROUTES.FOLLOW_UP_SEQUENCES,
        icon: ClipboardList,
        permission: "omnichannel:followup:sequence:read",
      },
      {
        label: "Campanhas",
        href: ROUTES.CAMPAIGNS,
        icon: Megaphone,
        permission: "campaign:page:view",
      },
      {
        label: "Automações",
        href: ROUTES.WORKFLOWS,
        icon: WorkflowIcon,
        permission: "workflow:page:view",
      },
    ],
  },
  {
    title: "Operação",
    icon: Wrench,
    items: [
      { label: "Catálogo", href: ROUTES.CATALOG, icon: Package, permission: "catalog:read" },
      { label: "Estoque", href: ROUTES.INVENTORY, icon: Boxes, permission: "inventory:read" },
      {
        label: "Financeiro",
        href: ROUTES.FINANCE_CONTROL,
        icon: CircleDollarSign,
        permission: "finance:read",
        children: [
          { label: "Controle financeiro", href: ROUTES.FINANCE_CONTROL },
          { label: "Conciliação", href: ROUTES.FINANCE_RECONCILIATION },
          { label: "Comissões", href: ROUTES.FINANCE_COMMISSIONS },
        ],
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
      { label: "Arquivos", href: ROUTES.STORAGE, icon: HardDrive },
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
          { label: "Lista de serviços", href: `${ROUTES.SETTINGS_COMPANY}/services` },
          { label: "Migração de dados", href: `${ROUTES.SETTINGS_COMPANY}/migration` },
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
      },
      {
        label: "Agenda",
        href: ROUTES.SETTINGS_AGENDA,
        icon: CalendarDays,
        permission: "scheduling:configure",
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
        label: "Agente de IA",
        href: ROUTES.SETTINGS_AGENT_CONFIG,
        icon: Bot,
        permission: "ai:agent-config",
      },
      {
        label: "Documentos",
        href: ROUTES.SETTINGS_DOCUMENTS,
        icon: FileText,
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
      { label: "Empresas", href: ROUTES.TENANTS, icon: Building2, superAdminOnly: true },
      {
        label: "Auditoria",
        href: ROUTES.AUDIT,
        icon: ClipboardList,
        permission: "audit:page:view",
      },
      // Ferramenta de desenvolvimento: fora do menu em produção.
      ...(process.env.NODE_ENV === "production"
        ? []
        : [{ label: "Design System", href: ROUTES.DESIGN_SYSTEM, icon: Palette }]),
    ],
  },
];
