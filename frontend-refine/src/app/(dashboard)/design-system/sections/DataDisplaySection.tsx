"use client";

import { useState } from "react";
import { toast } from "sonner";
import {
  Copy,
  Eye,
  Pencil,
  Plus,
  Table as TableIcon,
  Trash2,
  TrendingDown,
  TrendingUp,
  Users,
} from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { DealStageBadgePreset, LeadStatusBadgePreset } from "@/components/ui/status-badge";
import { Switch } from "@/components/ui/switch";
import { DataTable } from "@/components/ui/data-table";
import { CardStat } from "@/components/common/CardStat";
import { EmptyState } from "@/components/common/EmptyState";

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

export function DataDisplaySection() {
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
    toast.success(`${label} copiado!`, {
      description: text,
      duration: 2000,
    });
  };

  return (
    <section className="space-y-8" aria-labelledby="design-system-data-display-title">
      <div className="space-y-1">
        <h2 id="design-system-data-display-title" className="text-xl font-bold tracking-tight">
          Padrões de Tela do CRM
        </h2>
        <p className="text-sm text-muted-foreground">
          Composições completas de interface utilizadas em Contatos, Leads, Pipeline e Dashboards.
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
              <h3 className="text-base font-semibold">4. Tabela de Dados Unificada (DataTable)</h3>
              <Badge variant="outline" className="border-crm-primary/30 text-xs text-crm-primary">
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
              <label htmlFor="table-loading-toggle" className="cursor-pointer text-xs font-medium">
                Simular Carregamento
              </label>
            </div>

            <div className="flex items-center space-x-2">
              <Switch
                id="table-empty-toggle"
                checked={tableEmpty}
                onCheckedChange={setTableEmpty}
              />
              <label htmlFor="table-empty-toggle" className="cursor-pointer text-xs font-medium">
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
            description: "Não há registros correspondentes aos filtros aplicados nesta consulta.",
            action: (
              <Button variant="crm" size="sm" onClick={() => setTableEmpty(false)}>
                Restaurar Dados de Exemplo
              </Button>
            ),
          }}
        />
      </div>
    </section>
  );
}
