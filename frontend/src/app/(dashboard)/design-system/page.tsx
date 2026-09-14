"use client";

import { useState } from "react";
import { toast } from "sonner";
import {
  Palette,
  Copy,
  Check,
  Search,
  SlidersHorizontal,
  Plus,
  RefreshCw,
  Sparkles,
  ShieldCheck,
  Layers,
  Type,
  Boxes,
  LayoutDashboard,
  TrendingUp,
  TrendingDown,
  Users,
  Code2,
  SunMoon,
  Eye,
  Pencil,
  Trash2,
  Table as TableIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Checkbox } from "@/components/ui/checkbox";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { CardStat } from "@/components/common/CardStat";
import { EmptyState } from "@/components/common/EmptyState";
import { ThemeToggle } from "@/components/common/ThemeToggle";
import {
  StatusBadge,
  LeadStatusBadgePreset,
  DealStageBadgePreset,
  UserStatusBadgePreset,
  ChannelStatusBadgePreset,
} from "@/components/ui/status-badge";
import { DataTable } from "@/components/ui/data-table";

// Tokens de cores do projeto para documentação viva
const COLOR_TOKENS = [
  {
    category: "Marca & Ações Principais",
    description: "Cores de identidade do CRM, botões de ação e estados de interação primários.",
    items: [
      {
        name: "CRM Primary",
        token: "--crm-primary",
        tailwind: "bg-crm-primary",
        hex: "hsl(221.2 83.2% 53.3%)",
        textColor: "text-white",
        bgClass: "bg-crm-primary",
        usage: "Botões principais, abas ativas, links de destaque e seleções.",
      },
      {
        name: "CRM Primary Hover",
        token: "--crm-primary-hover",
        tailwind: "bg-crm-primary-hover",
        hex: "hsl(221.2 83.2% 46%)",
        textColor: "text-white",
        bgClass: "bg-crm-primary-hover",
        usage: "Estado de hover em botões e elementos clicáveis primários.",
      },
      {
        name: "CRM Primary Active",
        token: "--crm-primary-active",
        tailwind: "bg-crm-primary-active",
        hex: "hsl(221.2 83.2% 39%)",
        textColor: "text-white",
        bgClass: "bg-crm-primary-active",
        usage: "Estado pressionado/ativo.",
      },
      {
        name: "CRM Secondary",
        token: "--crm-secondary",
        tailwind: "bg-crm-secondary",
        hex: "hsl(220 14.3% 95.9%)",
        textColor: "text-foreground",
        bgClass: "bg-crm-secondary",
        usage: "Fundos de botões secundários e áreas de apoio neutras.",
      },
    ],
  },
  {
    category: "Status Semânticos de Negócio",
    description:
      "Cores universais para pipelines, leads, status de usuários, canais e notificações.",
    items: [
      {
        name: "Sucesso (Success / Ganho)",
        token: "--crm-success",
        tailwind: "text-emerald-700 bg-emerald-100 dark:bg-emerald-950/50 dark:text-emerald-400",
        hex: "hsl(142 71% 45%)",
        textColor: "text-white",
        bgClass: "bg-emerald-500",
        usage: "Deals ganhos, canais conectados, usuários ativos, metas batidas.",
      },
      {
        name: "Atenção (Warning / Pendente)",
        token: "--warning",
        tailwind: "text-amber-800 bg-amber-100 dark:bg-amber-950/50 dark:text-amber-400",
        hex: "hsl(38 92% 50%)",
        textColor: "text-slate-900",
        bgClass: "bg-amber-500",
        usage: "Avisos de SLA, oportunidades estagnadas, confirmações pendentes.",
      },
      {
        name: "Perigo (Danger / Perdido)",
        token: "--crm-danger",
        tailwind: "text-rose-700 bg-rose-100 dark:bg-rose-950/50 dark:text-rose-400",
        hex: "hsl(0 84.2% 60.2%)",
        textColor: "text-white",
        bgClass: "bg-rose-500",
        usage: "Deals perdidos, falhas de envio, exclusões destrutivas, erros críticos.",
      },
      {
        name: "Informação (Info / Em Andamento)",
        token: "--info",
        tailwind: "text-blue-700 bg-blue-100 dark:bg-blue-950/50 dark:text-blue-400",
        hex: "hsl(217 91% 60%)",
        textColor: "text-white",
        bgClass: "bg-blue-500",
        usage: "Leads novos, contatos em qualificação, sincronizações ativas.",
      },
    ],
  },
  {
    category: "Superfícies, Bordas & Contraste",
    description: "Fundos adaptativos com alternância perfeita entre Light Mode e Dark Mode.",
    items: [
      {
        name: "Fundo Base (Background)",
        token: "--background",
        tailwind: "bg-background",
        hex: "var(--background)",
        textColor: "text-foreground",
        bgClass: "bg-background border",
        usage: "Fundo principal das páginas e da aplicação.",
      },
      {
        name: "Superfície / Card",
        token: "--card",
        tailwind: "bg-card",
        hex: "var(--card)",
        textColor: "text-card-foreground",
        bgClass: "bg-card border",
        usage: "Containers elevados, cards, painéis e diálogos.",
      },
      {
        name: "Muted / Sutil",
        token: "--muted",
        tailwind: "bg-muted",
        hex: "var(--muted)",
        textColor: "text-muted-foreground",
        bgClass: "bg-muted",
        usage: "Áreas de preenchimento suave, skeletons e linhas zebradas.",
      },
      {
        name: "Borda Padrão (Border)",
        token: "--border",
        tailwind: "border-border",
        hex: "var(--border)",
        textColor: "text-foreground",
        bgClass: "bg-background border-4 border-border",
        usage: "Divisores, contornos de cards, inputs e tabelas.",
      },
    ],
  },
];

type DemoContact = {
  id: string;
  name: string;
  email: string;
  phone: string;
  company: string;
  stage: string;
  status: string;
  value: string;
};

const DEMO_CONTACTS: DemoContact[] = [
  {
    id: "1",
    name: "Mariana Silva",
    email: "mariana.silva@acme.com",
    phone: "(11) 98765-4321",
    company: "Acme Indústria",
    stage: "Negociação",
    status: "QUALIFIED",
    value: "R$ 45.000",
  },
  {
    id: "2",
    name: "Carlos Eduardo",
    email: "carlos@techcorp.io",
    phone: "(21) 97654-3210",
    company: "TechCorp Brasil",
    stage: "Fechado/Ganho",
    status: "CONVERTED",
    value: "R$ 120.000",
  },
  {
    id: "3",
    name: "Juliana Mendes",
    email: "juliana@logexpress.com.br",
    phone: "(31) 99887-7665",
    company: "LogExpress Transportes",
    stage: "Proposta",
    status: "CONTACTED",
    value: "R$ 28.500",
  },
  {
    id: "4",
    name: "Roberto Fernandes",
    email: "roberto@construbem.com",
    phone: "(41) 98877-6655",
    company: "ConstruBem Obras",
    stage: "Descoberta",
    status: "NEW",
    value: "R$ 75.000",
  },
  {
    id: "5",
    name: "Camila Duarte",
    email: "camila@fintechmax.com",
    phone: "(19) 97766-5544",
    company: "FintechMax Pagamentos",
    stage: "Perdido",
    status: "LOST",
    value: "R$ 18.000",
  },
];

export default function DesignSystemPage() {
  const [copiedToken, setCopiedToken] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [interactiveLoading, setInteractiveLoading] = useState(false);
  const [interactiveDisabled, setInteractiveDisabled] = useState(false);
  const [buttonSize, setButtonSize] = useState<"sm" | "default" | "lg">("default");
  const [badgeAppearance, setBadgeAppearance] = useState<"subtle" | "solid" | "outline">("subtle");
  const [badgeWithDot, setBadgeWithDot] = useState(true);
  const [badgePulse, setBadgePulse] = useState(false);

  // Estados do DataTable de Demonstração
  const [tableLoading, setTableLoading] = useState(false);
  const [tableEmpty, setTableEmpty] = useState(false);
  const [tablePage, setTablePage] = useState(1);
  const [tableSort, setTableSort] = useState<{ column: string; direction: "asc" | "desc" }>({
    column: "name",
    direction: "asc",
  });

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopiedToken(text);
    toast.success(`${label} copiado!`, {
      description: text,
      duration: 2000,
    });
    setTimeout(() => setCopiedToken(null), 2000);
  };

  const filteredCategories = COLOR_TOKENS.map((cat) => ({
    ...cat,
    items: cat.items.filter(
      (item) =>
        item.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.token.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.tailwind.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.usage.toLowerCase().includes(searchTerm.toLowerCase()),
    ),
  })).filter((cat) => cat.items.length > 0);

  return (
    <div className="space-y-8 pb-16">
      {/* ========================================================================= */}
      {/* 1. HERO HEADER COM BENTO-GRID & GLASSMORPHISM                             */}
      {/* ========================================================================= */}
      <div className="relative overflow-hidden rounded-2xl border bg-gradient-to-br from-card via-card to-muted/40 p-6 shadow-sm md:p-8">
        <div className="pointer-events-none absolute right-0 top-0 -mr-16 -mt-16 h-64 w-64 rounded-full bg-crm-primary/10 blur-3xl" />

        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full border border-crm-primary/30 bg-crm-primary/10 px-3 py-1 text-xs font-semibold text-crm-primary">
              <Sparkles className="h-3.5 w-3.5" />
              <span>Design System v1.0 • Live Foundations</span>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-foreground md:text-4xl">
              Sistema de Design do CRM
            </h1>
            <p className="max-w-2xl text-sm text-muted-foreground md:text-base">
              Catálogo visual vivo de tokens semânticos, componentes atômicos, tipografia e padrões
              de tela. Use como bancada de trabalho para testar novos componentes antes de plugá-los
              nos módulos.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="shadow-xs backdrop-blur-xs flex items-center gap-2 rounded-xl border bg-background/80 p-1.5">
              <SunMoon className="ml-2 h-4 w-4 text-muted-foreground" />
              <span className="text-xs font-medium text-muted-foreground">Alternar Tema:</span>
              <ThemeToggle />
            </div>
          </div>
        </div>

        {/* Bento Stats / Pills Row */}
        <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-4 md:gap-4">
          <div className="backdrop-blur-xs rounded-xl border bg-background/60 p-3.5">
            <div className="flex items-center gap-2 text-crm-primary">
              <Palette className="h-4 w-4" />
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Tokens
              </span>
            </div>
            <p className="mt-1.5 text-2xl font-bold">24 Cores</p>
            <p className="text-[11px] text-muted-foreground">CSS Vars & Tailwind</p>
          </div>

          <div className="backdrop-blur-xs rounded-xl border bg-background/60 p-3.5">
            <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
              <Boxes className="h-4 w-4" />
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Componentes
              </span>
            </div>
            <p className="mt-1.5 text-2xl font-bold">20+ Base</p>
            <p className="text-[11px] text-muted-foreground">Radix UI + CVA</p>
          </div>

          <div className="backdrop-blur-xs rounded-xl border bg-background/60 p-3.5">
            <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400">
              <ShieldCheck className="h-4 w-4" />
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Acessibilidade
              </span>
            </div>
            <p className="mt-1.5 text-2xl font-bold">WCAG AA</p>
            <p className="text-[11px] text-muted-foreground">Contraste & ARIA nativo</p>
          </div>

          <div className="backdrop-blur-xs rounded-xl border bg-background/60 p-3.5">
            <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
              <Layers className="h-4 w-4" />
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Temas
              </span>
            </div>
            <p className="mt-1.5 text-2xl font-bold">Light / Dark</p>
            <p className="text-[11px] text-muted-foreground">Persistência automática</p>
          </div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 2. TABS PRINCIPAIS COM PESQUISA E FILTROS                                 */}
      {/* ========================================================================= */}
      <Tabs defaultValue="foundations" className="space-y-6">
        <div className="flex flex-col gap-4 border-b pb-4 sm:flex-row sm:items-center sm:justify-between">
          <TabsList className="h-11 bg-muted/60 p-1">
            <TabsTrigger value="foundations" className="gap-2 text-xs md:text-sm">
              <Palette className="h-4 w-4" />
              Cores & Tokens
            </TabsTrigger>
            <TabsTrigger value="typography" className="gap-2 text-xs md:text-sm">
              <Type className="h-4 w-4" />
              Tipografia & Espaçamento
            </TabsTrigger>
            <TabsTrigger value="components" className="gap-2 text-xs md:text-sm">
              <Boxes className="h-4 w-4" />
              Componentes Base
            </TabsTrigger>
            <TabsTrigger value="crm-patterns" className="gap-2 text-xs md:text-sm">
              <LayoutDashboard className="h-4 w-4" />
              Padrões CRM
            </TabsTrigger>
          </TabsList>

          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Buscar tokens ou estilos..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9 text-xs sm:text-sm"
            />
          </div>
        </div>

        {/* ======================================================================= */}
        {/* ABA 1: CORES E TOKENS FUNDAMENTAIS                                      */}
        {/* ======================================================================= */}
        <TabsContent value="foundations" className="space-y-8">
          <div className="space-y-1">
            <h2 className="text-xl font-bold tracking-tight">Paleta Semântica e Design Tokens</h2>
            <p className="text-sm text-muted-foreground">
              Clique em qualquer cartão para copiar o token CSS ou a classe utilitária do Tailwind
              para a sua área de transferência.
            </p>
          </div>

          {filteredCategories.map((cat, idx) => (
            <div key={idx} className="space-y-4">
              <div>
                <h3 className="text-base font-semibold">{cat.category}</h3>
                <p className="text-xs text-muted-foreground">{cat.description}</p>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {cat.items.map((token) => (
                  <Card
                    key={token.token}
                    className="group relative cursor-pointer overflow-hidden transition-all hover:border-crm-primary hover:shadow-md"
                    onClick={() => handleCopy(token.tailwind, "Classe Tailwind")}
                  >
                    {/* Swatch color strip */}
                    <div
                      className={`h-24 w-full ${token.bgClass} flex items-end justify-between p-3`}
                    >
                      <span
                        className={`backdrop-blur-xs rounded bg-black/40 px-2 py-0.5 font-mono text-xs font-medium text-white`}
                      >
                        {token.hex}
                      </span>
                      <Button
                        size="icon"
                        variant="secondary"
                        className="h-7 w-7 opacity-80 transition-opacity group-hover:opacity-100"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleCopy(`var(${token.token})`, "Variável CSS");
                        }}
                        title="Copiar Variável CSS"
                      >
                        {copiedToken === `var(${token.token})` ? (
                          <Check className="h-3.5 w-3.5 text-emerald-500" />
                        ) : (
                          <Code2 className="h-3.5 w-3.5" />
                        )}
                      </Button>
                    </div>

                    <CardContent className="space-y-2 p-4">
                      <div className="flex items-center justify-between">
                        <h4 className="text-sm font-semibold">{token.name}</h4>
                        <span className="font-mono text-[11px] text-muted-foreground">
                          {token.token}
                        </span>
                      </div>
                      <p className="line-clamp-2 text-xs text-muted-foreground">{token.usage}</p>
                      <div className="flex items-center justify-between border-t pt-2 text-[11px] text-muted-foreground group-hover:text-crm-primary">
                        <span>Copiar classe Tailwind</span>
                        <Copy className="h-3 w-3" />
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          ))}
        </TabsContent>

        {/* ======================================================================= */}
        {/* ABA 2: TIPOGRAFIA E ESCALA                                              */}
        {/* ======================================================================= */}
        <TabsContent value="typography" className="space-y-8">
          <div className="space-y-1">
            <h2 className="text-xl font-bold tracking-tight">
              Hierarquia Tipográfica & Espaçamento
            </h2>
            <p className="text-sm text-muted-foreground">
              A tipografia utiliza a família sans-serif Inter para interfaces densas e legíveis, com
              JetBrains Mono para códigos e identificadores.
            </p>
          </div>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Escala de Títulos e Textos</CardTitle>
              <CardDescription>Padrões de tamanho, peso e espaçamento vertical.</CardDescription>
            </CardHeader>
            <CardContent className="divide-y">
              <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
                <div className="font-mono text-xs text-muted-foreground">
                  H1 · text-3xl md:text-4xl · Bold
                </div>
                <div className="text-3xl font-extrabold tracking-tight md:col-span-3 md:text-4xl">
                  Dashboard de Oportunidades
                </div>
              </div>

              <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
                <div className="font-mono text-xs text-muted-foreground">
                  H2 · text-2xl · Semibold
                </div>
                <div className="text-2xl font-bold tracking-tight md:col-span-3">
                  Pipeline de Vendas & Contatos
                </div>
              </div>

              <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
                <div className="font-mono text-xs text-muted-foreground">
                  H3 · text-lg md:text-xl · Semibold
                </div>
                <div className="text-lg font-semibold md:col-span-3 md:text-xl">
                  Detalhes do Cliente e Próxima Ação Recomendada
                </div>
              </div>

              <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
                <div className="font-mono text-xs text-muted-foreground">
                  Body · text-sm · Regular
                </div>
                <div className="text-sm text-foreground md:col-span-3">
                  Gerencie todo o fluxo omnichannel com histórico unificado de WhatsApp, e-mails,
                  notas comerciais e automações inteligentes alimentadas pela IA Leo.
                </div>
              </div>

              <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
                <div className="font-mono text-xs text-muted-foreground">
                  Small / Muted · text-xs · Muted
                </div>
                <div className="text-xs text-muted-foreground md:col-span-3">
                  Criado em 12 de setembro de 2026 às 14:32 por Guilherme Alves · ID: deal_9012a4b
                </div>
              </div>

              <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
                <div className="font-mono text-xs text-muted-foreground">
                  Code / Mono · font-mono text-xs
                </div>
                <div className="inline-block rounded bg-muted/60 px-2 py-1 font-mono text-xs md:col-span-3">
                  POST /api/v1/omnichannel/messages/send
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Espaçamentos */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Tokens de Espaçamento e Grid</CardTitle>
              <CardDescription>
                Escala modular de 4px para paddings, margens e gaps.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
                {[
                  { label: "space-1", px: "4px", w: "w-1 h-8" },
                  { label: "space-2", px: "8px", w: "w-2 h-8" },
                  { label: "space-3", px: "12px", w: "w-3 h-8" },
                  { label: "space-4", px: "16px", w: "w-4 h-8" },
                  { label: "space-6", px: "24px", w: "w-6 h-8" },
                  { label: "space-8", px: "32px", w: "w-8 h-8" },
                  { label: "space-12", px: "48px", w: "w-12 h-8" },
                ].map((s) => (
                  <div
                    key={s.label}
                    className="flex flex-col items-center rounded-lg border bg-muted/20 p-3"
                  >
                    <span className="font-mono text-xs font-medium">{s.label}</span>
                    <span className="mb-3 text-[11px] text-muted-foreground">{s.px}</span>
                    <div className={`rounded bg-crm-primary ${s.w}`} />
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* ======================================================================= */}
        {/* ABA 3: COMPONENTES BASE (INTERACTIVE SANDBOX)                           */}
        {/* ======================================================================= */}
        <TabsContent value="components" className="space-y-8">
          {/* Barra de Controles da Bancada de Testes */}
          <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border bg-muted/30 p-4">
            <div className="flex items-center gap-2">
              <SlidersHorizontal className="h-4 w-4 text-crm-primary" />
              <span className="text-sm font-semibold">Bancada de Testes Interativa:</span>
            </div>

            <div className="flex flex-wrap items-center gap-6">
              <div className="flex items-center gap-2">
                <Switch
                  id="loading-toggle"
                  checked={interactiveLoading}
                  onCheckedChange={setInteractiveLoading}
                />
                <label htmlFor="loading-toggle" className="cursor-pointer text-xs font-medium">
                  Simular Carregamento
                </label>
              </div>

              <div className="flex items-center gap-2">
                <Switch
                  id="disabled-toggle"
                  checked={interactiveDisabled}
                  onCheckedChange={setInteractiveDisabled}
                />
                <label htmlFor="disabled-toggle" className="cursor-pointer text-xs font-medium">
                  Simular Desabilitado
                </label>
              </div>

              <div className="flex items-center gap-1.5">
                <span className="text-xs text-muted-foreground">Tamanho:</span>
                {(["sm", "default", "lg"] as const).map((sz) => (
                  <Button
                    key={sz}
                    size="sm"
                    variant={buttonSize === sz ? "default" : "outline"}
                    className="h-7 px-2 text-xs uppercase"
                    onClick={() => setButtonSize(sz)}
                  >
                    {sz}
                  </Button>
                ))}
              </div>
            </div>
          </div>

          {/* Botões */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <div>
                <CardTitle className="text-base">Botões (Button Variants)</CardTitle>
                <CardDescription>
                  Ações primárias, secundárias, destrutivas e sutis.
                </CardDescription>
              </div>
              <Button
                variant="ghost"
                size="sm"
                className="gap-1 text-xs text-muted-foreground"
                onClick={() =>
                  handleCopy(
                    `<Button variant="crm" size="${buttonSize}">Ação Primária</Button>`,
                    "Código JSX",
                  )
                }
              >
                <Copy className="h-3 w-3" /> Copiar Exemplo
              </Button>
            </CardHeader>
            <CardContent className="space-y-6 pt-4">
              <div className="flex flex-wrap items-center gap-3">
                <Button variant="crm" size={buttonSize} disabled={interactiveDisabled}>
                  {interactiveLoading ? (
                    <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                  ) : (
                    <Plus className="mr-2 h-4 w-4" />
                  )}
                  CRM Primary
                </Button>

                <Button variant="default" size={buttonSize} disabled={interactiveDisabled}>
                  Default
                </Button>

                <Button variant="secondary" size={buttonSize} disabled={interactiveDisabled}>
                  Secundário
                </Button>

                <Button variant="outline" size={buttonSize} disabled={interactiveDisabled}>
                  Outline
                </Button>

                <Button variant="ghost" size={buttonSize} disabled={interactiveDisabled}>
                  Ghost
                </Button>

                <Button variant="destructive" size={buttonSize} disabled={interactiveDisabled}>
                  Destrutivo
                </Button>

                <Button
                  variant="crm"
                  size="icon"
                  className={
                    buttonSize === "sm"
                      ? "h-8 w-8"
                      : buttonSize === "lg"
                        ? "h-11 w-11"
                        : "h-10 w-10"
                  }
                  disabled={interactiveDisabled}
                >
                  <Plus className="h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Badges de Status Semânticos (Design System Core) */}
          <Card>
            <CardHeader className="flex flex-col gap-4 pb-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <CardTitle className="flex items-center gap-2 text-base">
                  <span>StatusBadge Polimórfico</span>
                  <Badge
                    variant="outline"
                    className="border-crm-primary/30 text-xs text-crm-primary"
                  >
                    Unificado v1.0
                  </Badge>
                </CardTitle>
                <CardDescription>
                  Substitui os 12 componentes isolados de badges do sistema com suporte a temas,
                  indicador de ponto, animação de pulso e presets automáticos por entidade.
                </CardDescription>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <Button
                  variant="ghost"
                  size="sm"
                  className="gap-1 text-xs text-muted-foreground"
                  onClick={() =>
                    handleCopy(
                      `<StatusBadge intent="success" withDot appearance="${badgeAppearance}">Fechado / Ganho</StatusBadge>`,
                      "Código JSX",
                    )
                  }
                >
                  <Copy className="h-3 w-3" /> Copiar JSX
                </Button>
              </div>
            </CardHeader>

            <CardContent className="space-y-6 pt-4">
              {/* Controles da Badge */}
              <div className="flex flex-wrap items-center justify-between gap-4 rounded-lg border bg-muted/20 p-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-muted-foreground">
                    Estilo Visual:
                  </span>
                  {(["subtle", "solid", "outline"] as const).map((app) => (
                    <Button
                      key={app}
                      size="sm"
                      variant={badgeAppearance === app ? "default" : "outline"}
                      className="h-7 px-2.5 text-xs capitalize"
                      onClick={() => setBadgeAppearance(app)}
                    >
                      {app}
                    </Button>
                  ))}
                </div>

                <div className="flex flex-wrap items-center gap-4">
                  <div className="flex items-center space-x-2">
                    <Switch
                      id="badge-dot"
                      checked={badgeWithDot}
                      onCheckedChange={setBadgeWithDot}
                    />
                    <label htmlFor="badge-dot" className="cursor-pointer text-xs font-medium">
                      Ponto Indicador (withDot)
                    </label>
                  </div>

                  <div className="flex items-center space-x-2">
                    <Switch id="badge-pulse" checked={badgePulse} onCheckedChange={setBadgePulse} />
                    <label htmlFor="badge-pulse" className="cursor-pointer text-xs font-medium">
                      Pulsar (pulseDot)
                    </label>
                  </div>
                </div>
              </div>

              {/* 1. Intenções Fundamentais */}
              <div className="space-y-2">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  1. Intenções Semânticas Universais
                </h4>
                <div className="flex flex-wrap items-center gap-2.5">
                  <StatusBadge
                    intent="success"
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                    pulseDot={badgePulse}
                  >
                    Success (Sucesso)
                  </StatusBadge>

                  <StatusBadge
                    intent="warning"
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                    pulseDot={badgePulse}
                  >
                    Warning (Atenção)
                  </StatusBadge>

                  <StatusBadge
                    intent="danger"
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                    pulseDot={badgePulse}
                  >
                    Danger (Perigo)
                  </StatusBadge>

                  <StatusBadge
                    intent="info"
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                    pulseDot={badgePulse}
                  >
                    Info (Informação)
                  </StatusBadge>

                  <StatusBadge
                    intent="indigo"
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                    pulseDot={badgePulse}
                  >
                    Indigo (Descoberta)
                  </StatusBadge>

                  <StatusBadge
                    intent="purple"
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                    pulseDot={badgePulse}
                  >
                    Purple (Negociação)
                  </StatusBadge>

                  <StatusBadge
                    intent="neutral"
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                    pulseDot={badgePulse}
                  >
                    Neutral (Padrão)
                  </StatusBadge>
                </div>
              </div>

              {/* 2. Presets de Entidades de Negócio */}
              <div className="space-y-4 border-t pt-4">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  2. Presets Integrados de Negócio (Zero Boilerplate)
                </h4>

                {/* Pipeline Stages */}
                <div className="space-y-1.5">
                  <span className="text-xs font-medium text-foreground">Pipeline & Deals:</span>
                  <div className="flex flex-wrap items-center gap-2">
                    {[
                      "Novo",
                      "Descoberta",
                      "Proposta",
                      "Negociação",
                      "Fechado/Ganho",
                      "Perdido",
                    ].map((st) => (
                      <DealStageBadgePreset
                        key={st}
                        stage={st}
                        appearance={badgeAppearance}
                        withDot={badgeWithDot}
                        pulseDot={badgePulse && st === "Fechado/Ganho"}
                      />
                    ))}
                  </div>
                </div>

                {/* Lead Statuses */}
                <div className="space-y-1.5">
                  <span className="text-xs font-medium text-foreground">Módulo de Leads:</span>
                  <div className="flex flex-wrap items-center gap-2">
                    {["NEW", "CONTACTED", "QUALIFIED", "UNQUALIFIED", "CONVERTED", "LOST"].map(
                      (st) => (
                        <LeadStatusBadgePreset
                          key={st}
                          status={st}
                          appearance={badgeAppearance}
                          withDot={badgeWithDot}
                        />
                      ),
                    )}
                  </div>
                </div>

                {/* User Statuses */}
                <div className="space-y-1.5">
                  <span className="text-xs font-medium text-foreground">Usuários do Sistema:</span>
                  <div className="flex flex-wrap items-center gap-2">
                    {["active", "pending", "locked", "inactive"].map((st) => (
                      <UserStatusBadgePreset
                        key={st}
                        status={st}
                        appearance={badgeAppearance}
                        withDot={badgeWithDot}
                        pulseDot={badgePulse && st === "active"}
                      />
                    ))}
                  </div>
                </div>

                {/* Omnichannel Channels */}
                <div className="space-y-1.5">
                  <span className="text-xs font-medium text-foreground">
                    Canais Omnichannel / WhatsApp:
                  </span>
                  <div className="flex flex-wrap items-center gap-2">
                    {["ACTIVE", "CONNECTING", "INACTIVE", "ERROR"].map((st) => (
                      <ChannelStatusBadgePreset
                        key={st}
                        status={st}
                        appearance={badgeAppearance}
                        withDot={badgeWithDot}
                        pulseDot={st === "ACTIVE" || st === "CONNECTING"}
                      />
                    ))}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Controles de Formulário */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Controles de Formulário e Entradas</CardTitle>
              <CardDescription>
                Inputs, switches e caixas de texto com validações integradas.
              </CardDescription>
            </CardHeader>
            <CardContent className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div className="space-y-2">
                <label className="text-xs font-semibold">Campo de Texto com Ícone</label>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input placeholder="Pesquisar por nome ou e-mail..." className="pl-9" />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold">Campo com Estado de Erro</label>
                <Input
                  defaultValue="contato-invalido"
                  className="border-rose-500 focus-visible:ring-rose-500"
                />
                <p className="text-[11px] text-rose-600 dark:text-rose-400">
                  Insira um endereço de e-mail corporativo válido.
                </p>
              </div>

              <div className="space-y-2 md:col-span-2">
                <label className="text-xs font-semibold">Área de Texto (Notas & Observações)</label>
                <Textarea
                  placeholder="Adicione notas da reunião de alinhamento com o cliente..."
                  rows={3}
                />
              </div>

              <div className="flex items-center space-x-2">
                <Checkbox id="demo-check" defaultChecked />
                <label htmlFor="demo-check" className="cursor-pointer text-xs font-medium">
                  Disparar sequência de follow-up via WhatsApp automaticamente
                </label>
              </div>

              <div className="flex items-center space-x-2">
                <Switch id="demo-switch" defaultChecked />
                <label htmlFor="demo-switch" className="cursor-pointer text-xs font-medium">
                  Ativar co-piloto IA para qualificação do Lead
                </label>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* ======================================================================= */}
        {/* ABA 4: PADRÕES DE CRM (MOLECULES & ORGANISMS)                            */}
        {/* ======================================================================= */}
        <TabsContent value="crm-patterns" className="space-y-8">
          <div className="space-y-1">
            <h2 className="text-xl font-bold tracking-tight">Padrões de Tela do CRM</h2>
            <p className="text-sm text-muted-foreground">
              Composições completas de interface utilizadas em Contatos, Leads, Pipeline e
              Dashboards.
            </p>
          </div>

          {/* Cards de Métricas (KPIs) */}
          <div className="space-y-3">
            <h3 className="text-sm font-semibold">1. Cards de Indicadores (CardStat)</h3>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <CardStat
                title="Receita Mensal Prevista"
                value="R$ 148.500"
                description="em comparação ao mês anterior"
                icon={<TrendingUp className="h-5 w-5 text-crm-primary" />}
                trend={{ value: 14.8, isPositive: true }}
              />

              <CardStat
                title="Novas Oportunidades"
                value="42"
                description="3 aguardando contato inicial"
                icon={<Users className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />}
                trend={{ value: 8.2, isPositive: true }}
              />

              <CardStat
                title="Taxa de Churn / Perdas"
                value="2.1%"
                description="-0.4% abaixo da meta de tolerância"
                icon={<TrendingDown className="h-5 w-5 text-rose-600 dark:text-rose-400" />}
                trend={{ value: 3.4, isPositive: false }}
              />
            </div>
          </div>

          {/* Estado Vazio (EmptyState) */}
          <div className="space-y-3">
            <h3 className="text-sm font-semibold">2. Estado Vazio Oficial (EmptyState)</h3>
            <Card>
              <CardContent className="p-0">
                <EmptyState
                  icon={<Users className="h-8 w-8 text-crm-primary" />}
                  title="Nenhum contato encontrado no filtro atual"
                  description="Tente ajustar os critérios de pesquisa ou adicione um novo contato para alimentar seu pipeline."
                  action={
                    <Button variant="crm" size="sm">
                      <Plus className="mr-2 h-4 w-4" /> Adicionar Primeiro Contato
                    </Button>
                  }
                />
              </CardContent>
            </Card>
          </div>

          {/* Skeleton Loaders */}
          <div className="space-y-3">
            <h3 className="text-sm font-semibold">3. Loading Skeletons</h3>
            <Card className="p-6">
              <div className="space-y-3">
                <div className="flex items-center space-x-4">
                  <Skeleton className="h-12 w-12 rounded-full" />
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-[250px]" />
                    <Skeleton className="h-4 w-[200px]" />
                  </div>
                </div>
                <Skeleton className="mt-4 h-10 w-full rounded-md" />
                <Skeleton className="h-10 w-full rounded-md" />
              </div>
            </Card>
          </div>

          {/* 4. DataTable Mestre Unificado */}
          <div className="space-y-4">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-semibold">
                    4. Tabela de Dados Unificada (DataTable)
                  </h3>
                  <Badge
                    variant="outline"
                    className="border-crm-primary/30 text-xs text-crm-primary"
                  >
                    Genérico v1.0
                  </Badge>
                </div>
                <p className="text-xs text-muted-foreground">
                  Componente mestre com suporte a ordenação, paginação de servidor/cliente, ações
                  contextuais e estados vazios/loading automáticos.
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <Button
                  variant="ghost"
                  size="sm"
                  className="gap-1 text-xs text-muted-foreground"
                  onClick={() =>
                    handleCopy(
                      `<DataTable
  data={contacts}
  columns={columns}
  isLoading={isLoading}
  actions={[
    { label: "Visualizar", icon: Eye, onClick: handleView },
    { label: "Editar", icon: Pencil, onClick: handleEdit },
    { label: "Excluir", icon: Trash2, destructive: true, onClick: handleDelete },
  ]}
  pagination={{
    currentPage: 1,
    totalPages: 5,
    totalItems: 48,
    pageSize: 10,
    onPageChange: (page) => setPage(page),
  }}
/>`,
                      "Código JSX",
                    )
                  }
                >
                  <Copy className="h-3 w-3" /> Copiar Exemplo
                </Button>
              </div>
            </div>

            {/* Controles da Tabela */}
            <div className="flex flex-wrap items-center justify-between gap-4 rounded-lg border bg-muted/20 p-3">
              <div className="flex items-center gap-2">
                <TableIcon className="h-4 w-4 text-crm-primary" />
                <span className="text-xs font-semibold text-muted-foreground">
                  Simular Estados da Tabela:
                </span>
              </div>

              <div className="flex flex-wrap items-center gap-6">
                <div className="flex items-center space-x-2">
                  <Switch
                    id="table-loading-toggle"
                    checked={tableLoading}
                    onCheckedChange={setTableLoading}
                  />
                  <label
                    htmlFor="table-loading-toggle"
                    className="cursor-pointer text-xs font-medium"
                  >
                    Simular Carregamento
                  </label>
                </div>

                <div className="flex items-center space-x-2">
                  <Switch
                    id="table-empty-toggle"
                    checked={tableEmpty}
                    onCheckedChange={setTableEmpty}
                  />
                  <label
                    htmlFor="table-empty-toggle"
                    className="cursor-pointer text-xs font-medium"
                  >
                    Simular Tabela Vazia
                  </label>
                </div>
              </div>
            </div>

            {/* Renderização do DataTable */}
            <DataTable
              data={tableEmpty ? [] : DEMO_CONTACTS}
              isLoading={tableLoading}
              columns={[
                {
                  header: "Contato",
                  accessor: "name",
                  sortable: true,
                  cell: (row) => (
                    <div className="flex items-center gap-3">
                      <Avatar className="h-8 w-8">
                        <AvatarFallback className="bg-crm-primary/10 text-xs font-semibold text-crm-primary">
                          {row.name
                            .split(" ")
                            .map((n) => n[0])
                            .join("")
                            .slice(0, 2)}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-medium text-foreground">{row.name}</div>
                        <div className="text-xs text-muted-foreground">{row.email}</div>
                      </div>
                    </div>
                  ),
                },
                {
                  header: "Empresa",
                  accessor: "company",
                  sortable: true,
                  className: "text-muted-foreground",
                },
                {
                  header: "Telefone",
                  accessor: "phone",
                  className: "text-xs text-muted-foreground font-mono",
                },
                {
                  header: "Etapa / Pipeline",
                  accessor: "stage",
                  sortable: true,
                  cell: (row) => <DealStageBadgePreset stage={row.stage} withDot />,
                },
                {
                  header: "Status Lead",
                  accessor: "status",
                  cell: (row) => <LeadStatusBadgePreset status={row.status} withDot />,
                },
                {
                  header: "Valor Estimado",
                  accessor: "value",
                  sortable: true,
                  className: "font-semibold text-foreground text-right",
                  headerClassName: "text-right",
                },
              ]}
              sort={{
                column: tableSort.column,
                direction: tableSort.direction,
                onSort: (col) =>
                  setTableSort((prev) => ({
                    column: col,
                    direction: prev.column === col && prev.direction === "asc" ? "desc" : "asc",
                  })),
              }}
              actions={[
                {
                  label: "Visualizar Contato",
                  icon: Eye,
                  onClick: (row) => toast.info(`Visualizando ${row.name}`),
                },
                {
                  label: "Editar Informações",
                  icon: Pencil,
                  onClick: (row) => toast.info(`Editando ${row.name}`),
                },
                {
                  label: "Excluir Registro",
                  icon: Trash2,
                  destructive: true,
                  onClick: (row) => toast.error(`Exclusão solicitada para ${row.name}`),
                },
              ]}
              pagination={{
                currentPage: tablePage,
                totalPages: 5,
                totalItems: 24,
                pageSize: 5,
                onPageChange: (p) => setTablePage(p),
              }}
              emptyState={{
                icon: <Users className="h-8 w-8 text-crm-primary" />,
                title: "Nenhum contato encontrado",
                description:
                  "Não há registros correspondentes aos filtros aplicados nesta consulta.",
                action: (
                  <Button variant="crm" size="sm" onClick={() => setTableEmpty(false)}>
                    Restaurar Dados de Exemplo
                  </Button>
                ),
              }}
            />
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
