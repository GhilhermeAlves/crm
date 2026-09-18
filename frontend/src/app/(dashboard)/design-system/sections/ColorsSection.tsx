"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Check, Code2, Copy, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";

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

export function ColorsSection() {
  const [copiedToken, setCopiedToken] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");

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
    <section className="space-y-8" aria-labelledby="design-system-colors-title">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="space-y-1">
          <h2 id="design-system-colors-title" className="text-xl font-bold tracking-tight">
            Paleta Semântica e Design Tokens
          </h2>
          <p className="text-sm text-muted-foreground">
            Clique em qualquer cartão para copiar o token CSS ou a classe utilitária do Tailwind
            para a sua área de transferência.
          </p>
        </div>

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
                <div className={`h-24 w-full ${token.bgClass} flex items-end justify-between p-3`}>
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
    </section>
  );
}
