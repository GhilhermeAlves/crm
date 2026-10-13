"use client";

import { useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { AlertTriangle, CheckCircle2, Clock, Plus, Wallet, type LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { cn } from "@/lib/utils";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import { TransactionFormDialog } from "@/features/finance/components/TransactionFormDialog";
import { useAllTransactions } from "@/features/finance/hooks/useFinance";
import type { FinancialTransaction } from "@/features/finance/types/finance.types";
import { centsToBRL } from "@/features/finance/utils/money";
import { summarizePatientTransactions } from "@/features/finance/utils/patient-summary";
import type { Contact } from "../types/contact.types";

type CardDefinition = {
  label: string;
  valueCents: number;
  icon: LucideIcon;
  tone: "income" | "warning" | "danger" | "neutral";
};

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
          {valueCents === 0 ? (
            <p className="mt-0.5 text-sm text-muted-foreground">Sem registros</p>
          ) : (
            <p className="mt-0.5 text-lg font-semibold tabular-nums">{centsToBRL(valueCents)}</p>
          )}
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

  const [dialogOpen, setDialogOpen] = useState(false);

  const summary = useMemo(() => summarizePatientTransactions(transactions), [transactions]);

  const groups = useMemo(() => {
    const map = new Map<string, FinancialTransaction[]>();
    transactions.forEach((t) => {
      const key = t.installmentGroupId ?? `single:${t.id}`;
      const list = map.get(key) ?? [];
      list.push(t);
      map.set(key, list);
    });
    return Array.from(map.values()).map((items) =>
      [...items].sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
    );
  }, [transactions]);

  if (!can("finance:read")) {
    return (
      <p className="py-16 text-center text-sm text-muted-foreground">
        Você não tem acesso ao financeiro.
      </p>
    );
  }

  const contactName = [contact.firstName, contact.lastName].filter(Boolean).join(" ") || "Paciente";
  const paidRatio =
    summary.totalCents > 0
      ? Math.min(100, Math.round((summary.paidCents / summary.totalCents) * 100))
      : 0;

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

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">Financeiro</h1>
        {can("finance:manage") && (
          <Button size="sm" className="gap-1.5" onClick={() => setDialogOpen(true)}>
            <Plus className="h-4 w-4" />
            Registrar pagamento
          </Button>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((card) => (
          <SummaryCard key={card.label} {...card} />
        ))}
      </div>

      <div className="space-y-1.5">
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>Progresso de pagamento</span>
          <span className="tabular-nums">{paidRatio}%</span>
        </div>
        <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-emerald-500 transition-all"
            style={{ width: `${paidRatio}%` }}
          />
        </div>
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-muted-foreground">Carregando lançamentos…</p>
      ) : groups.length === 0 ? (
        <p className="py-16 text-center text-sm text-muted-foreground">
          Nenhum pagamento registrado.
        </p>
      ) : (
        <div className="space-y-3">
          {groups.map((group) => {
            const first = group[0];
            const groupTotal = group.reduce((sum, t) => sum + t.amountCents, 0);
            return (
              <Card key={first.installmentGroupId ?? first.id}>
                <CardContent className="p-4">
                  <div className="mb-3 flex items-center justify-between gap-2">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{first.description}</p>
                      {group.length > 1 && (
                        <p className="text-xs text-muted-foreground">{group.length}x parcelas</p>
                      )}
                    </div>
                    <span className="shrink-0 text-sm font-semibold tabular-nums">
                      {centsToBRL(groupTotal)}
                    </span>
                  </div>
                  <ul className="divide-y">
                    {group.map((transaction) => (
                      <li
                        key={transaction.id}
                        className="flex items-center justify-between gap-3 py-2 text-sm"
                      >
                        <span className="tabular-nums text-muted-foreground">
                          {format(parseISO(transaction.dueDate), "dd/MM/yyyy")}
                        </span>
                        <span className="flex-1 tabular-nums">
                          {centsToBRL(transaction.amountCents)}
                        </span>
                        {statusBadge(transaction)}
                      </li>
                    ))}
                  </ul>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

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
