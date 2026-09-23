"use client";

import { useState } from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { useAnalyticsSummary } from "@/features/analytics/hooks/useAnalytics";
import {
  PERIOD_OPTIONS,
  delta,
  type PeriodOption,
} from "@/features/analytics/types/analytics.types";
import { PageTitle } from "@/components/common/PageTitle";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, Legend } from "recharts";
import { BarChart3, TrendingDown, TrendingUp, Minus } from "lucide-react";

type Kpi = {
  label: string;
  current: number;
  previous: number;
  format?: (v: number) => string;
};

const brl = (v: number) =>
  v.toLocaleString("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 0 });

export default function ReportsPage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;

  const [period, setPeriod] = useState<PeriodOption>("30d");
  const { data, isLoading, isError } = useAnalyticsSummary(companyId, period);

  if (!can("analytics:read")) {
    return (
      <div className="py-12 text-center text-muted-foreground">
        Você não tem permissão para visualizar relatórios.
      </div>
    );
  }

  const kpis: Kpi[] = data
    ? [
        {
          label: "Contatos criados",
          current: data.current.contactsCreated,
          previous: data.previous.contactsCreated,
        },
        {
          label: "Leads criados",
          current: data.current.leadsCreated,
          previous: data.previous.leadsCreated,
        },
        {
          label: "Leads convertidos",
          current: data.current.leadsConverted,
          previous: data.previous.leadsConverted,
        },
        {
          label: "Oportunidades criadas",
          current: data.current.opportunitiesCreated,
          previous: data.previous.opportunitiesCreated,
        },
        {
          label: "Vendas ganhas",
          current: data.current.opportunitiesWon,
          previous: data.previous.opportunitiesWon,
        },
        {
          label: "Valor ganho",
          current: Number(data.current.wonValue),
          previous: Number(data.previous.wonValue),
          format: brl,
        },
        {
          label: "Pipeline aberto",
          current: Number(data.current.pipelineOpenValue),
          previous: Number(data.previous.pipelineOpenValue),
          format: brl,
        },
        {
          label: "Atividades",
          current: data.current.activitiesCreated,
          previous: data.previous.activitiesCreated,
        },
        {
          label: "Tarefas concluídas",
          current: data.current.tasksCompleted,
          previous: data.previous.tasksCompleted,
        },
        {
          label: "Tarefas atrasadas",
          current: data.current.tasksOverdue,
          previous: data.previous.tasksOverdue,
        },
        {
          label: "Campanhas executadas",
          current: data.current.campaignsExecuted,
          previous: data.previous.campaignsExecuted,
        },
        {
          label: "Mensagens campanha",
          current: data.current.campaignMessagesSent,
          previous: data.previous.campaignMessagesSent,
        },
        {
          label: "WhatsApp enviadas",
          current: data.current.omnichannelMessagesOut,
          previous: data.previous.omnichannelMessagesOut,
        },
        {
          label: "WhatsApp recebidas",
          current: data.current.omnichannelMessagesIn,
          previous: data.previous.omnichannelMessagesIn,
        },
        {
          label: "Automações executadas",
          current: data.current.workflowRunsMatched,
          previous: data.previous.workflowRunsMatched,
        },
      ]
    : [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <PageTitle>Relatórios</PageTitle>
        <Select value={period} onValueChange={(v) => setPeriod(v as PeriodOption)}>
          <SelectTrigger className="w-[200px]" aria-label="Período do dashboard">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {PERIOD_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>
                {o.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isLoading && (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4 lg:grid-cols-5">
          {Array.from({ length: 10 }).map((_, i) => (
            <div key={i} className="h-24 animate-pulse rounded bg-muted" />
          ))}
        </div>
      )}

      {isError && (
        <Card>
          <CardContent className="py-10 text-center text-muted-foreground">
            Erro ao carregar o dashboard. Tente novamente.
          </CardContent>
        </Card>
      )}

      {data && (
        <>
          <p className="text-sm text-muted-foreground" role="status">
            Período: {data.from} a {data.to} — comparação com o período anterior de mesma duração.
          </p>

          <div className="grid grid-cols-2 gap-4 md:grid-cols-4 lg:grid-cols-5">
            {kpis.map((kpi) => (
              <KpiCard key={kpi.label} {...kpi} />
            ))}
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Evolução diária</CardTitle>
            </CardHeader>
            <CardContent>
              {data.series.length === 0 ? (
                <p className="py-8 text-center text-muted-foreground">
                  Sem dados no período selecionado.
                </p>
              ) : (
                <div
                  className="h-72 w-full"
                  role="img"
                  aria-label="Gráfico de evolução diária de leads, oportunidades e mensagens"
                >
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={data.series}>
                      <XAxis
                        dataKey="date"
                        tick={{ fontSize: 11 }}
                        tickFormatter={(d: string) => d.slice(5)}
                      />
                      <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                      <Tooltip />
                      <Legend />
                      <Line
                        type="monotone"
                        dataKey="leads"
                        name="Leads"
                        stroke="#2563eb"
                        strokeWidth={2}
                        dot={false}
                      />
                      <Line
                        type="monotone"
                        dataKey="opportunities"
                        name="Oportunidades"
                        stroke="#16a34a"
                        strokeWidth={2}
                        dot={false}
                      />
                      <Line
                        type="monotone"
                        dataKey="messagesSent"
                        name="Mensagens enviadas"
                        stroke="#f59e0b"
                        strokeWidth={2}
                        dot={false}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Automações</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-3">
              <AutomationStat
                label="Execuções"
                value={data.current.workflowRunsMatched}
                success={data.current.workflowRunsSuccess}
                failed={data.current.workflowRunsFailed}
              />
              <AutomationStat
                label="Campanhas"
                value={data.current.campaignMessagesSent}
                success={data.current.campaignMessagesSent - data.current.campaignMessagesFailed}
                failed={data.current.campaignMessagesFailed}
              />
              <AutomationStat
                label="Conversas WhatsApp"
                value={data.current.omnichannelMessagesOut + data.current.omnichannelMessagesIn}
                success={data.current.omnichannelMessagesOut}
                failed={0}
              />
            </CardContent>
          </Card>
        </>
      )}

      {!isLoading && !isError && !data && (
        <Card>
          <CardContent className="flex flex-col items-center py-12 text-muted-foreground">
            <BarChart3 className="mb-4 h-12 w-12 opacity-50" />
            <p>Sem dados disponíveis para exibir.</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function KpiCard({ label, current, previous, format }: Kpi) {
  const fmt = format ?? ((v: number) => v.toLocaleString("pt-BR"));
  const d = delta(current, previous);
  return (
    <Card>
      <CardContent className="p-4">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="mt-1 text-xl font-semibold">{fmt(current)}</p>
        <p className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
          {d.pct === null ? (
            <>
              <Minus className="h-3 w-3" /> sem base anterior
            </>
          ) : d.pct >= 0 ? (
            <>
              <TrendingUp className="h-3 w-3 text-green-600" /> +{d.pct}% vs anterior
            </>
          ) : (
            <>
              <TrendingDown className="h-3 w-3 text-red-600" /> {d.pct}% vs anterior
            </>
          )}
        </p>
      </CardContent>
    </Card>
  );
}

function AutomationStat({
  label,
  value,
  success,
  failed,
}: {
  label: string;
  value: number;
  success: number;
  failed: number;
}) {
  const rate = value > 0 ? Math.round((success / value) * 100) : null;
  return (
    <div className="rounded-md border p-4">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-lg font-semibold">{value.toLocaleString("pt-BR")}</p>
      <p className="text-xs text-muted-foreground">
        Sucesso: {success.toLocaleString("pt-BR")} · Falhas: {failed.toLocaleString("pt-BR")}
        {rate !== null && ` · ${rate}%`}
      </p>
    </div>
  );
}
