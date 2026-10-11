"use client";

import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Plus, Search, SlidersHorizontal } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { FinanceActionsCard } from "@/features/finance/components/FinanceActionsCard";
import { FinancialHealthCard } from "@/features/finance/components/FinancialHealthCard";
import { FinanceKpiCards } from "@/features/finance/components/FinanceKpiCards";
import { FinanceTypeTabs, type FinanceTab } from "@/features/finance/components/FinanceTypeTabs";
import { PeriodPicker } from "@/features/finance/components/PeriodPicker";
import { TransactionsTable } from "@/features/finance/components/TransactionsTable";
import { useFinanceSummary, useTransactions } from "@/features/finance/hooks/useFinance";
import type { TransactionFilters } from "@/features/finance/types/finance.types";
import {
  PERIOD_KEYS,
  resolvePeriod,
  type DateRangeValue,
  type PeriodKey,
} from "@/features/finance/utils/period";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";

const PAGE_SIZE = 20;

function FinanceControlSkeleton() {
  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-4 w-96" />
      </div>
      <div className="grid gap-4 lg:grid-cols-3">
        <Skeleton className="h-80 lg:col-span-2" />
        <Skeleton className="h-40" />
      </div>
      <Skeleton className="h-12 w-full" />
      <div className="grid gap-4 sm:grid-cols-3">
        <Skeleton className="h-20" />
        <Skeleton className="h-20" />
        <Skeleton className="h-20" />
      </div>
      <Skeleton className="h-96 w-full" />
    </div>
  );
}

function FinanceControlContent() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  // PONTOS DE ABERTURA — os diálogos (e o consumo destes estados) são das etapas 6 e 7.
  const [, setNewOpen] = useState(false); // etapa 6: novo lançamento
  const [, setImportOpen] = useState(false); // etapa 6: envio/importação de tabela
  const [, setFiltersOpen] = useState(false); // etapa 6: filtros avançados
  const [, setAccountingReportOpen] = useState(false); // etapa 7: relatório contábil

  const tabParam = searchParams.get("tab");
  const tab: FinanceTab = tabParam === "income" || tabParam === "expense" ? tabParam : "all";

  const periodParam = searchParams.get("period");
  const period: PeriodKey =
    periodParam && (PERIOD_KEYS as readonly string[]).includes(periodParam)
      ? (periodParam as PeriodKey)
      : "month";

  const fromParam = searchParams.get("from");
  const toParam = searchParams.get("to");
  const custom: DateRangeValue | null =
    period === "custom" && fromParam && toParam ? { from: fromParam, to: toParam } : null;

  const pageParam = Number(searchParams.get("page") ?? "1");
  const page = Number.isFinite(pageParam) && pageParam >= 1 ? Math.floor(pageParam) : 1;

  const months = searchParams.get("months") === "12" ? 12 : 6;
  const activeFilterCount = 0; // etapa 6: contagem de filtros avançados ativos

  const q = searchParams.get("q") ?? "";
  const [search, setSearch] = useState(q);
  const debouncedSearch = useDebouncedValue(search, 300);

  const setParams = useCallback(
    (updates: Record<string, string | null>, options?: { resetPage?: boolean }) => {
      const params = new URLSearchParams(searchParams.toString());
      for (const [key, value] of Object.entries(updates)) {
        if (value === null || value === "") params.delete(key);
        else params.set(key, value);
      }
      if (options?.resetPage !== false) params.delete("page");
      const queryString = params.toString();
      router.replace(queryString ? `${pathname}?${queryString}` : pathname, { scroll: false });
    },
    [pathname, router, searchParams],
  );

  // Mantém o input de busca refletindo a URL (ex.: voltar/avançar do navegador).
  useEffect(() => {
    setSearch(q);
  }, [q]);

  // Sincroniza a busca já debounced com a URL (resetando a página).
  useEffect(() => {
    if (debouncedSearch === q) return;
    setParams({ q: debouncedSearch || null }, { resetPage: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch]);

  const range = useMemo(
    () =>
      resolvePeriod(
        period,
        period === "custom" && fromParam && toParam ? { from: fromParam, to: toParam } : null,
      ),
    [period, fromParam, toParam],
  );

  const type = tab === "income" ? "INCOME" : tab === "expense" ? "EXPENSE" : undefined;

  const summaryFilters = useMemo<TransactionFilters>(
    () => ({ from: range.from, to: range.to, ...(type ? { type } : {}) }),
    [range.from, range.to, type],
  );

  const listFilters = useMemo(
    () => ({
      ...summaryFilters,
      ...(q ? { q } : {}),
      page: page - 1,
      pageSize: PAGE_SIZE,
    }),
    [summaryFilters, q, page],
  );

  const summaryQuery = useFinanceSummary(companyId, summaryFilters);
  const transactionsQuery = useTransactions(companyId, listFilters);

  const transactions = transactionsQuery.data?.content ?? [];
  const totalPages = transactionsQuery.data?.totalPages ?? 0;
  const totalElements = transactionsQuery.data?.totalElements ?? 0;

  const handleTabChange = (next: FinanceTab) => {
    setParams({ tab: next === "all" ? null : next });
  };

  const handlePeriodChange = (next: PeriodKey, nextCustom?: DateRangeValue) => {
    setParams({
      period: next,
      from: nextCustom?.from ?? null,
      to: nextCustom?.to ?? null,
    });
  };

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Controle financeiro</h1>
        <p className="text-sm text-muted-foreground">
          Acompanhe as receitas, despesas e o saldo do período.
        </p>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <FinancialHealthCard
          companyId={companyId}
          months={months}
          onMonthsChange={(next) => setParams({ months: String(next) }, { resetPage: false })}
          className="lg:col-span-2"
        />
        <FinanceActionsCard
          onAccountingReport={() => setAccountingReportOpen(true)}
          onImport={() => setImportOpen(true)}
        />
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <FinanceTypeTabs value={tab} onChange={handleTabChange} />
        <div className="flex flex-wrap items-center gap-2">
          <PeriodPicker period={period} custom={custom} onChange={handlePeriodChange} />
          <Button
            variant="outline"
            className="gap-2"
            onClick={() => setFiltersOpen(true)}
            aria-label="Filtrar lançamentos"
          >
            <SlidersHorizontal className="h-4 w-4" />
            Filtrar
            {activeFilterCount > 0 && (
              <Badge variant="secondary" className="h-5 min-w-5 justify-center px-1">
                {activeFilterCount}
              </Badge>
            )}
          </Button>
        </div>
      </div>

      <FinanceKpiCards tab={tab} summary={summaryQuery.data} isLoading={summaryQuery.isLoading} />

      <Card>
        <CardContent className="space-y-4 p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative w-full max-w-sm">
              <Input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Busque por paciente, profissional ou descrição"
                className="pr-9"
                aria-label="Buscar lançamentos"
              />
              <Search className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            </div>
            <Button className="gap-2" onClick={() => setNewOpen(true)}>
              <Plus className="h-4 w-4" />
              Novo lançamento
            </Button>
          </div>

          <TransactionsTable
            transactions={transactions}
            isLoading={transactionsQuery.isLoading}
            isError={transactionsQuery.isError}
            onRetry={() => transactionsQuery.refetch()}
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={PAGE_SIZE}
            onPageChange={(next) => setParams({ page: String(next) }, { resetPage: false })}
            onNew={() => setNewOpen(true)}
            onImport={() => setImportOpen(true)}
          />
        </CardContent>
      </Card>
    </div>
  );
}

export default function FinanceControlPage() {
  return (
    <Suspense fallback={<FinanceControlSkeleton />}>
      <FinanceControlContent />
    </Suspense>
  );
}
