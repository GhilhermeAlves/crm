"use client";

import { useMemo, useState } from "react";
import { Plus, SearchX, ShieldOff } from "lucide-react";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { mockDeals } from "@/features/pipeline/data/deals.mock";
import type {
  Deal,
  DealFormValues,
  DealGroup,
  DealStage,
} from "@/features/pipeline/types/deal.types";
import { DealTable } from "@/features/pipeline/components/DealTable";
import { DealFilters } from "@/features/pipeline/components/DealFilters";
import { DealFormDialog } from "@/features/pipeline/components/DealFormDialog";
import { DeleteDealDialog } from "@/features/pipeline/components/DeleteDealDialog";
import { ChangeStageDialog } from "@/features/pipeline/components/ChangeStageDialog";
import { PageTitle } from "@/components/common/PageTitle";
import { SearchInput } from "@/components/common/SearchInput";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/common/EmptyState";

const groupTitles: Record<DealGroup, string> = {
  active: "Oportunidades Ativas",
  won: "Fechado/Ganho",
};

export default function PipelinePage() {
  const { can } = useAuthorization();
  const [deals, setDeals] = useState<Deal[]>(mockDeals);
  const [search, setSearch] = useState("");
  const [stageFilter, setStageFilter] = useState("all");
  const [responsibleFilter, setResponsibleFilter] = useState("all");
  const [forecastFilter, setForecastFilter] = useState("all");
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Deal | null>(null);
  const [deleting, setDeleting] = useState<Deal | null>(null);
  const [changingStage, setChangingStage] = useState<Deal | null>(null);

  const responsibles = useMemo(
    () => Array.from(new Set(deals.map((d) => d.responsible).filter((r): r is string => !!r))),
    [deals],
  );

  const hasActiveFilters =
    search.trim() !== "" ||
    stageFilter !== "all" ||
    responsibleFilter !== "all" ||
    forecastFilter !== "all";

  const clearFilters = () => {
    setSearch("");
    setStageFilter("all");
    setResponsibleFilter("all");
    setForecastFilter("all");
  };

  const filteredDeals = useMemo(() => {
    const q = search.trim().toLowerCase();
    return deals.filter((deal) => {
      if (stageFilter !== "all" && deal.stage !== stageFilter) return false;
      if (responsibleFilter !== "all" && deal.responsible !== responsibleFilter) return false;
      if (forecastFilter !== "all" && deal.forecastCategory !== forecastFilter) return false;
      if (q) {
        const haystack = `${deal.name} ${deal.contact} ${deal.responsible ?? ""}`.toLowerCase();
        if (!haystack.includes(q)) return false;
      }
      return true;
    });
  }, [deals, search, stageFilter, responsibleFilter, forecastFilter]);

  const activeDeals = filteredDeals.filter((d) => d.group === "active");
  const wonDeals = filteredDeals.filter((d) => d.group === "won");

  const handleCreate = (values: DealFormValues) => {
    const newDeal: Deal = {
      id: `deal-${Date.now()}`,
      name: values.name,
      stage: values.stage,
      value: values.value.trim() === "" ? null : Number(values.value.replace(",", ".")),
      contact: values.contact,
      expectedCloseDate: values.expectedCloseDate || null,
      probability: Number(values.probability),
      expectedValue:
        values.expectedValue.trim() === "" ? null : Number(values.expectedValue.replace(",", ".")),
      forecastCategory: values.forecastCategory || null,
      group: "active",
      responsible: values.responsible || null,
      tasks: null,
      schedule: null,
      lastInteraction: null,
      quotesInvoices: null,
    };
    setDeals((prev) => [...prev, newDeal]);
    setFormOpen(false);
  };

  const handleEdit = (values: DealFormValues) => {
    if (!editing) return;
    setDeals((prev) =>
      prev.map((d) =>
        d.id === editing.id
          ? {
              ...d,
              name: values.name,
              stage: values.stage,
              value: values.value.trim() === "" ? null : Number(values.value.replace(",", ".")),
              contact: values.contact,
              expectedCloseDate: values.expectedCloseDate || null,
              probability: Number(values.probability),
              expectedValue:
                values.expectedValue.trim() === ""
                  ? null
                  : Number(values.expectedValue.replace(",", ".")),
              forecastCategory: values.forecastCategory || null,
              responsible: values.responsible || null,
            }
          : d,
      ),
    );
    setEditing(null);
    setFormOpen(false);
  };

  const handleDelete = () => {
    if (deleting) {
      setDeals((prev) => prev.filter((d) => d.id !== deleting.id));
      setDeleting(null);
    }
  };

  const handleChangeStage = (stage: DealStage) => {
    if (!changingStage) return;
    setDeals((prev) =>
      prev.map((d) =>
        d.id === changingStage.id
          ? {
              ...d,
              stage,
              group: stage === "Fechado/Ganho" ? "won" : stage === "Perdido" ? "active" : d.group,
            }
          : d,
      ),
    );
    setChangingStage(null);
  };

  if (!can("pipeline:page:view")) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center py-12 text-muted-foreground">
          <ShieldOff className="mb-4 h-10 w-10 opacity-50" />
          <p>Você não tem permissão para acessar a página de Negociações.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="space-y-1">
          <PageTitle>Negociações</PageTitle>
          <p className="text-sm text-muted-foreground">
            Acompanhe oportunidades e o pipeline comercial.
          </p>
        </div>
        <Button
          onClick={() => {
            setEditing(null);
            setFormOpen(true);
          }}
        >
          <Plus className="mr-2 h-4 w-4" />
          Nova negociação
        </Button>
      </div>

      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div className="w-full max-w-sm">
          <SearchInput
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onClear={() => setSearch("")}
            placeholder="Pesquisar por nome, contato ou responsável..."
          />
        </div>
        <DealFilters
          stage={stageFilter}
          responsible={responsibleFilter}
          forecastCategory={forecastFilter}
          responsibles={responsibles}
          onStageChange={setStageFilter}
          onResponsibleChange={setResponsibleFilter}
          onForecastChange={setForecastFilter}
          onClear={clearFilters}
        />
      </div>

      {hasActiveFilters && filteredDeals.length === 0 ? (
        <Card>
          <CardContent>
            <EmptyState
              icon={<SearchX className="h-8 w-8" />}
              title="Nenhum resultado"
              description="Não encontramos negociações para a pesquisa ou filtros aplicados."
              action={
                <Button variant="outline" size="sm" onClick={clearFilters}>
                  Limpar filtros
                </Button>
              }
            />
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-6">
          <DealTable
            deals={activeDeals}
            groupTitle={groupTitles.active}
            onEdit={(d) => {
              setEditing(d);
              setFormOpen(true);
            }}
            onDelete={setDeleting}
            onChangeStage={setChangingStage}
          />
          <DealTable
            deals={wonDeals}
            groupTitle={groupTitles.won}
            onEdit={(d) => {
              setEditing(d);
              setFormOpen(true);
            }}
            onDelete={setDeleting}
            onChangeStage={setChangingStage}
          />
        </div>
      )}

      <DealFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        deal={editing}
        onSubmit={editing ? handleEdit : handleCreate}
      />
      <DeleteDealDialog
        deal={deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        onConfirm={handleDelete}
      />
      <ChangeStageDialog
        deal={changingStage}
        onOpenChange={(o) => !o && setChangingStage(null)}
        onConfirm={handleChangeStage}
      />
    </div>
  );
}
