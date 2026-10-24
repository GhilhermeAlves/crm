"use client";

import { useMemo, useState } from "react";
import { useQueries, useQueryClient } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { ptBR } from "date-fns/locale";
import {
  AlertTriangle,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Clock,
  CreditCard,
  Plus,
  Wallet,
  type LucideIcon,
} from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { cn } from "@/lib/utils";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import { BudgetDetailsDialog } from "@/features/clinical/budgets/components/BudgetDetailsDialog";
import { budgetKeys, useBudgets } from "@/features/clinical/budgets/hooks/useBudgets";
import { budgetService } from "@/features/clinical/budgets/services/budget.service";
import type { BudgetDetail } from "@/features/clinical/budgets/types/budget.types";
import { useProcedures } from "@/features/clinical/procedures/hooks/useProcedures";
import { TransactionFormDialog } from "@/features/finance/components/TransactionFormDialog";
import { useAllTransactions } from "@/features/finance/hooks/useFinance";
import {
  PAYMENT_METHOD_LABELS,
  type FinancialTransaction,
} from "@/features/finance/types/finance.types";
import { centsToBRL } from "@/features/finance/utils/money";
import { summarizePatientTransactions } from "@/features/finance/utils/patient-summary";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

type CardDefinition = {
  label: string;
  valueCents: number;
  icon: LucideIcon;
  tone: "income" | "warning" | "danger" | "neutral";
};

type Section = "budgets" | "procedures" | "standalone";

const TONE_CLASSES: Record<CardDefinition["tone"], string> = {
  income: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
  warning: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
  danger: "bg-red-500/10 text-red-600 dark:text-red-400",
  neutral: "bg-muted text-muted-foreground",
};

function SummaryCard({ label, valueCents, icon: Icon, tone }: CardDefinition) {
  return (
    <Card>
      <CardContent className="flex items-center gap-3 p-4">
        <span
          className={cn(
            "flex h-10 w-10 shrink-0 items-center justify-center rounded-full",
            TONE_CLASSES[tone],
          )}
        >
          <Icon className="h-5 w-5" />
        </span>
        <div className="min-w-0">
          <p className="text-xs text-muted-foreground">{label}</p>
          <p className="mt-0.5 text-lg font-semibold tabular-nums">{centsToBRL(valueCents)}</p>
        </div>
      </CardContent>
    </Card>
  );
}

function statusBadge(transaction: FinancialTransaction) {
  if (transaction.status === "CANCELLED") {
    return (
      <StatusBadge intent="neutral" size="sm">
        Cancelado
      </StatusBadge>
    );
  }
  if (transaction.status === "PAID") {
    return (
      <StatusBadge intent="success" size="sm">
        Pago
      </StatusBadge>
    );
  }
  return transaction.overdue ? (
    <StatusBadge intent="danger" size="sm">
      Atrasado
    </StatusBadge>
  ) : (
    <StatusBadge intent="warning" size="sm">
      Pendente
    </StatusBadge>
  );
}

/** Faixa à esquerda da linha, na cor do status (como no Capim). */
function rowAccent(transaction: FinancialTransaction) {
  if (transaction.status === "PAID") return "border-l-emerald-500";
  if (transaction.status === "CANCELLED") return "border-l-slate-300";
  return transaction.overdue ? "border-l-red-500" : "border-l-amber-400";
}

const byDueDate = (a: FinancialTransaction, b: FinancialTransaction) =>
  a.dueDate.localeCompare(b.dueDate);

function PaymentsTable({
  transactions,
  extraColumn,
}: {
  transactions: FinancialTransaction[];
  extraColumn?: { header: string; value: (t: FinancialTransaction) => string };
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="text-xs text-muted-foreground">
          <tr className="border-b">
            <th className="px-3 py-2 text-left font-medium">Data</th>
            {extraColumn && (
              <th className="px-3 py-2 text-left font-medium">{extraColumn.header}</th>
            )}
            <th className="px-3 py-2 text-left font-medium">Descrição</th>
            <th className="px-3 py-2 text-left font-medium">Forma de pagto.</th>
            <th className="px-3 py-2 text-right font-medium">Valor</th>
            <th className="px-3 py-2 text-center font-medium">Status</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((t) => (
            <tr key={t.id} className={cn("border-b border-l-4 last:border-b-0", rowAccent(t))}>
              <td className="whitespace-nowrap px-3 py-2.5 tabular-nums">
                {format(parseISO(t.dueDate), "dd/MM/yyyy")}
              </td>
              {extraColumn && <td className="px-3 py-2.5">{extraColumn.value(t)}</td>}
              <td className="max-w-[18rem] truncate px-3 py-2.5" title={t.description}>
                {t.description}
              </td>
              <td className="whitespace-nowrap px-3 py-2.5">
                <span className="inline-flex items-center gap-1.5 text-muted-foreground">
                  <CreditCard className="h-3.5 w-3.5" />
                  {PAYMENT_METHOD_LABELS[t.paymentMethod]}
                </span>
              </td>
              <td className="whitespace-nowrap px-3 py-2.5 text-right tabular-nums">
                {centsToBRL(t.amountCents)}
              </td>
              <td className="px-3 py-2.5 text-center">{statusBadge(t)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function BudgetFinanceCard({
  budget,
  transactions,
  onDetails,
}: {
  budget: BudgetDetail;
  transactions: FinancialTransaction[];
  onDetails: () => void;
}) {
  const [open, setOpen] = useState(false);
  const active = transactions.filter((t) => t.status !== "CANCELLED");
  const paidCents = active
    .filter((t) => t.status === "PAID")
    .reduce((sum, t) => sum + t.amountCents, 0);
  const ratio =
    budget.totalCents > 0 ? Math.min(100, Math.round((paidCents / budget.totalCents) * 100)) : 0;
  const date = budget.approvedAt ?? budget.updatedAt;

  return (
    <Card>
      <CardContent className="space-y-4 p-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-lg font-semibold">{budget.title}</p>
            <p className="text-sm text-muted-foreground">
              {budget.approvedAt ? "Aprovado em" : "Atualizado em"}{" "}
              {format(parseISO(date), "d 'de' MMM. 'de' yyyy", { locale: ptBR })} ·{" "}
              <button
                type="button"
                onClick={onDetails}
                className="font-medium text-primary underline underline-offset-2 hover:text-primary/80"
              >
                Ver detalhes
              </button>
            </p>
          </div>
          <div className="flex gap-6 text-right">
            <div>
              <p className="text-xs text-muted-foreground">Total do tratamento</p>
              <p className="font-semibold tabular-nums">{centsToBRL(budget.totalCents)}</p>
            </div>
            <div className="border-l pl-6">
              <p className="flex items-center justify-end gap-1.5 text-xs text-muted-foreground">
                <span className="h-2 w-2 rounded-full bg-emerald-500" /> Pago pelo paciente
              </p>
              <p className="font-semibold tabular-nums">{centsToBRL(paidCents)}</p>
            </div>
          </div>
        </div>
        <div className="space-y-1">
          <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-emerald-500 transition-all"
              style={{ width: `${ratio}%` }}
            />
          </div>
          <p className="text-xs tabular-nums text-muted-foreground">
            {centsToBRL(paidCents)} de {centsToBRL(budget.totalCents)} pagos · {ratio}%
          </p>
        </div>
        <div className="border-t pt-3">
          <button
            type="button"
            aria-expanded={open}
            onClick={() => setOpen((v) => !v)}
            className="flex items-center gap-1 text-sm font-medium text-primary"
          >
            {open ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
            Pagamentos ({transactions.length})
          </button>
          {open &&
            (transactions.length === 0 ? (
              <p className="py-4 text-sm text-muted-foreground">Nenhum pagamento gerado.</p>
            ) : (
              <div className="mt-3">
                <PaymentsTable transactions={transactions} />
              </div>
            ))}
        </div>
      </CardContent>
    </Card>
  );
}

export function PatientFinance({ contact }: { contact: Contact }) {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const can = usePermission();
  const queryClient = useQueryClient();

  const { data, isLoading } = useAllTransactions(companyId, {
    contactId: contact.id,
    type: "INCOME",
  });
  const transactions = useMemo(() => data ?? [], [data]);
  const { data: budgets = [] } = useBudgets(companyId ?? "", contact.id);
  const { data: procedures = [] } = useProcedures(companyId ?? "", contact.id);

  // Orçamentos com cobrança (aprovados ou perdidos com parcelas): o detalhe traz as parcelas.
  const billedBudgets = budgets.filter((b) => b.status !== "DRAFT");
  const budgetDetails = useQueries({
    queries: billedBudgets.map((b) => ({
      queryKey: [...budgetKeys.all(companyId ?? "", contact.id), b.id],
      queryFn: () => budgetService.get(companyId as string, contact.id, b.id),
      enabled: !!companyId,
    })),
  });

  const [dialogOpen, setDialogOpen] = useState(false);
  const [section, setSection] = useState<Section>("budgets");
  const [viewingBudget, setViewingBudget] = useState<string | null>(null);

  const summary = useMemo(() => summarizePatientTransactions(transactions), [transactions]);

  const details = budgetDetails
    .map((q) => q.data)
    .filter((d): d is BudgetDetail => d != null && d.installmentsList.length > 0);

  // Cada lançamento entra em um grupo só: parcelas de orçamento, depois procedimento, o resto é avulso.
  const { budgetGroups, procedureRows, standalone, procedureNames } = (() => {
    const byId = new Map(transactions.map((t) => [t.id, t]));
    const used = new Set<string>();
    const groups = details.map((budget) => {
      const items = budget.installmentsList
        .map((i) => byId.get(i.id))
        .filter((t): t is FinancialTransaction => t != null)
        .sort(byDueDate);
      items.forEach((t) => used.add(t.id));
      return { budget, transactions: items };
    });
    const names = new Map<string, string>();
    procedures.forEach((p) => {
      if (p.transaction && !used.has(p.transaction.id)) names.set(p.transaction.id, p.name);
    });
    const procRows = transactions.filter((t) => names.has(t.id)).sort(byDueDate);
    procRows.forEach((t) => used.add(t.id));
    const rest = transactions.filter((t) => !used.has(t.id)).sort(byDueDate);
    return {
      budgetGroups: groups,
      procedureRows: procRows,
      standalone: rest,
      procedureNames: names,
    };
  })();

  if (!can("finance:read")) {
    return (
      <p className="py-16 text-center text-sm text-muted-foreground">
        Você não tem acesso ao financeiro.
      </p>
    );
  }

  const contactName = [contact.firstName, contact.lastName].filter(Boolean).join(" ") || "Paciente";

  const cards: CardDefinition[] = [
    {
      label: "Total dos pagamentos",
      valueCents: summary.totalCents,
      icon: Wallet,
      tone: "neutral",
    },
    {
      label: "Pago pelo paciente",
      valueCents: summary.paidCents,
      icon: CheckCircle2,
      tone: "income",
    },
    {
      label: "Pagamento pendente",
      valueCents: summary.pendingCents,
      icon: Clock,
      tone: "warning",
    },
    {
      label: "Pagamento atrasado",
      valueCents: summary.overdueCents,
      icon: AlertTriangle,
      tone: "danger",
    },
  ];

  const sections: { value: Section; label: string; count: number }[] = [
    { value: "budgets", label: "Orçamentos", count: budgetGroups.length },
    { value: "procedures", label: "Procedimentos", count: procedureRows.length },
    { value: "standalone", label: "Avulsos", count: standalone.length },
  ];

  const empty = (text: string) => (
    <Card>
      <CardContent className="py-12 text-center text-sm text-muted-foreground">{text}</CardContent>
    </Card>
  );

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((card) => (
          <SummaryCard key={card.label} {...card} />
        ))}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div role="tablist" className="inline-flex rounded-full border bg-background p-1">
          {sections.map(({ value, label, count }) => (
            <button
              key={value}
              type="button"
              role="tab"
              aria-selected={section === value}
              onClick={() => setSection(value)}
              className={cn(
                "rounded-full px-4 py-1.5 text-sm font-medium transition-colors",
                section === value
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground",
              )}
            >
              {label} ({count})
            </button>
          ))}
        </div>
        {can("finance:manage") && (
          <Button size="sm" className="gap-1.5" onClick={() => setDialogOpen(true)}>
            <Plus className="h-4 w-4" />
            Registrar pagamento
          </Button>
        )}
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-muted-foreground">Carregando lançamentos…</p>
      ) : section === "budgets" ? (
        budgetGroups.length === 0 ? (
          empty("Nenhum orçamento com pagamentos.")
        ) : (
          <div className="space-y-4">
            {budgetGroups.map(({ budget, transactions: items }) => (
              <BudgetFinanceCard
                key={budget.id}
                budget={budget}
                transactions={items}
                onDetails={() => setViewingBudget(budget.id)}
              />
            ))}
          </div>
        )
      ) : section === "procedures" ? (
        procedureRows.length === 0 ? (
          empty("Nenhum pagamento de procedimento.")
        ) : (
          <Card>
            <CardContent className="p-3">
              <PaymentsTable
                transactions={procedureRows}
                extraColumn={{
                  header: "Procedimento",
                  value: (t) => procedureNames.get(t.id) ?? "—",
                }}
              />
            </CardContent>
          </Card>
        )
      ) : standalone.length === 0 ? (
        empty("Nenhum pagamento avulso.")
      ) : (
        <Card>
          <CardContent className="p-3">
            <PaymentsTable transactions={standalone} />
          </CardContent>
        </Card>
      )}

      <BudgetDetailsDialog
        companyId={companyId ?? ""}
        contactId={contact.id}
        budgetId={viewingBudget}
        onOpenChange={(open) => !open && setViewingBudget(null)}
      />

      <TransactionFormDialog
        open={dialogOpen}
        onOpenChange={(open) => {
          setDialogOpen(open);
          if (!open) queryClient.invalidateQueries({ queryKey: ["finance", companyId] });
        }}
        companyId={companyId}
        defaultType="INCOME"
        lockedContact={{ id: contact.id, name: contactName }}
      />
    </div>
  );
}
