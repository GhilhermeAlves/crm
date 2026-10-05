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
  HardDrive,
  Workflow as WorkflowIcon,
  Sparkles,
  Home,
  Bot,
  Palette,
  Shield,
  MailPlus,
  KeyRound,
  CalendarDays,
  FileText,
} from "lucide-react";
import { ROUTES } from "@/lib/constants";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  badge?: string;
  permission?: string;
  children?: { label: string; href: string }[];
}

export interface NavGroup {
  title?: string;
  items: NavItem[];
}

export const NAVIGATION: NavGroup[] = [
  {
    title: "CRM",
    items: [
      {
        label: "Início",
        href: ROUTES.CRM,
        icon: Home,
      },
      {
        label: "Leads",
        href: ROUTES.LEADS,
        icon: Users,
        permission: "lead:page:view",
      },
      {
        label: "Contatos",
        href: ROUTES.CONTACTS,
        icon: Contact,
        permission: "contact:page:view",
      },
      {
        label: "Pipeline",
        href: ROUTES.PIPELINE,
        icon: GitBranch,
        permission: "pipeline:page:view",
      },
      {
        label: "Agenda",
        href: ROUTES.AGENDA,
        icon: CalendarDays,
        permission: "appointment:read",
      },
      {
        label: "Tarefas",
        href: ROUTES.TASKS,
        icon: ClipboardList,
        permission: "task:page:view",
      },
      {
        label: "Catálogo",
        href: ROUTES.CATALOG,
        icon: Package,
        permission: "catalog:read",
      },
      {
        label: "Timeline",
        href: ROUTES.ACTIVITIES,
        icon: MailPlus,
        permission: "activity:page:view",
      },
      {
        label: "Automações",
        href: ROUTES.WORKFLOWS,
        icon: WorkflowIcon,
        permission: "workflow:page:view",
      },
      {
        label: "Léo · Assistente IA",
        href: ROUTES.ASSISTANT,
        icon: Sparkles,
        permission: "ai:chat",
      },
    ],
  },
  {
    title: "Comunicação",
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
        label: "Sequências de Follow-up",
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
    ],
  },
  {
    title: "Administração",
    items: [
      {
        label: "Empresas",
        href: ROUTES.TENANTS,
        icon: Building2,
        permission: "company:view",
      },
      {
        label: "Membros",
        href: ROUTES.MEMBERS,
        icon: Users,
        permission: "membership:view",
      },
      {
        label: "Convites",
        href: ROUTES.INVITATIONS,
        icon: MailPlus,
        permission: "membership:view",
      },
      {
        label: "Permissões",
        href: ROUTES.PERMISSIONS,
        icon: KeyRound,
        permission: "role:read",
      },
    ],
  },
  {
    title: "Análise",
    items: [
      { label: "Relatórios", href: ROUTES.REPORTS, icon: BarChart3 },
      { label: "Arquivos", href: ROUTES.STORAGE, icon: HardDrive },
    ],
  },
  {
    title: "Sistema",
    items: [
      {
        label: "Auditoria",
        href: ROUTES.AUDIT,
        icon: ClipboardList,
        permission: "audit:page:view",
      },
      {
        label: "Design System",
        href: ROUTES.DESIGN_SYSTEM,
        icon: Palette,
      },
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
          { label: "Criar novo usuário", href: `${ROUTES.SETTINGS_COMPANY}/users/new` },
          { label: "Gerenciar usuários", href: `${ROUTES.SETTINGS_COMPANY}/users` },
          { label: "Migração de dados", href: `${ROUTES.SETTINGS_COMPANY}/migration` },
          { label: "Lista de serviços", href: `${ROUTES.SETTINGS_COMPANY}/services` },
        ],
      },
      {
        label: "Usuários",
        href: ROUTES.SETTINGS_USERS,
        icon: Users,
        permission: "security:page:view",
      },
      {
        label: "Perfis",
        href: ROUTES.SETTINGS_ROLES,
        icon: Shield,
        permission: "security:page:view",
      },
      {
        label: "Config. Agenda",
        href: ROUTES.SETTINGS_AGENDA,
        icon: CalendarDays,
        permission: "scheduling:configure",
        children: [
          { label: "Ajustes gerais", href: ROUTES.SETTINGS_AGENDA },
          {
            label: "Configurar disponibilidade",
            href: `${ROUTES.SETTINGS_AGENDA}/availability`,
          },
          {
            label: "Central de notificações",
            href: `${ROUTES.SETTINGS_AGENDA}/notifications`,
          },
          { label: "Mensagem de aniversário", href: `${ROUTES.SETTINGS_AGENDA}/birthday` },
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
];
