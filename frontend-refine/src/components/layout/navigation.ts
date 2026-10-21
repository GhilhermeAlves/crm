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
} from "lucide-react";
import { ROUTES } from "@/lib/constants";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  badge?: string;
  permission?: string;
  children?: { label: string; href: string; permission?: string }[];
}

export interface NavGroup {
  title?: string;
  items: NavItem[];
}

export const NAVIGATION: NavGroup[] = [
  {
    title: "Principal",
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
        // Fase 1: agrupa as telas de usuário que existem hoje; a página única com abas vem depois.
        label: "Equipe",
        href: ROUTES.MEMBERS,
        icon: UsersRound,
        children: [
          { label: "Membros", href: ROUTES.MEMBERS, permission: "membership:view" },
          { label: "Convites", href: ROUTES.INVITATIONS, permission: "membership:view" },
          { label: "Usuários", href: ROUTES.SETTINGS_USERS, permission: "security:page:view" },
          { label: "Gerenciar usuários", href: `${ROUTES.SETTINGS_COMPANY}/users` },
          { label: "Criar novo usuário", href: `${ROUTES.SETTINGS_COMPANY}/users/new` },
        ],
      },
      {
        label: "Perfis e Permissões",
        href: ROUTES.SETTINGS_ROLES,
        icon: Shield,
        children: [
          { label: "Perfis", href: ROUTES.SETTINGS_ROLES, permission: "security:page:view" },
          { label: "Permissões", href: ROUTES.PERMISSIONS, permission: "role:read" },
        ],
      },
      {
        label: "Agenda",
        href: ROUTES.SETTINGS_AGENDA,
        icon: CalendarDays,
        permission: "scheduling:configure",
        children: [
          { label: "Ajustes gerais", href: ROUTES.SETTINGS_AGENDA },
          { label: "Disponibilidade", href: `${ROUTES.SETTINGS_AGENDA}/availability` },
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
    items: [
      { label: "Empresas", href: ROUTES.TENANTS, icon: Building2, permission: "company:view" },
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
