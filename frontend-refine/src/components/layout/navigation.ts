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
  HardDrive,
  Workflow as WorkflowIcon,
  Bell,
  Sparkles,
  Home,
  Bot,
  Palette,
  Shield,
  MailPlus,
  KeyRound,
} from "lucide-react";
import { ROUTES } from "@/lib/constants";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  badge?: string;
  permission?: string;
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
        label: "Tarefas",
        href: ROUTES.TASKS,
        icon: ClipboardList,
        permission: "task:page:view",
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
        label: "Notificações",
        href: ROUTES.NOTIFICATIONS,
        icon: Bell,
        permission: "notification:page:view",
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
      { label: "Arquivos", href: ROUTES.STORAGE, icon: HardDrive },
    ],
  },
  {
    title: "Análise",
    items: [{ label: "Relatórios", href: ROUTES.REPORTS, icon: BarChart3 }],
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
    title: "Segurança",
    items: [
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
        label: "Agente de IA",
        href: ROUTES.SETTINGS_AGENT_CONFIG,
        icon: Bot,
        permission: "ai:agent-config",
      },
    ],
  },
];
