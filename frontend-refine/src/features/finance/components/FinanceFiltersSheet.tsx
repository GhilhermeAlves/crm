"use client";

import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { ContactCombobox } from "@/features/sales/scheduling/components/ContactCombobox";
import { useContacts } from "@/features/masterdata/contacts/hooks/useContacts";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { Label } from "@/components/ui/label";

import type { FinanceTab } from "./FinanceTypeTabs";
import type {
  PaymentMethod,
  TransactionCategory,
  TransactionStatusFilter,
} from "../types/finance.types";
import {
  CATEGORIES_BY_TYPE,
  PAYMENT_METHOD_LABELS,
  TRANSACTION_CATEGORY_LABELS,
  TRANSACTION_STATUS_LABELS,
} from "../types/finance.types";

export type FinanceFilterValues = {
  status?: TransactionStatusFilter;
  paymentMethod?: PaymentMethod;
  category?: TransactionCategory;
  professionalUserId?: string;
  contactId?: string;
};

type FinanceFiltersSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tab: FinanceTab;
  companyId: string | null;
  initial: FinanceFilterValues;
  onApply: (values: FinanceFilterValues) => void;
  onClear: () => void;
};

const ALL = "all";

type Draft = {
  status: TransactionStatusFilter | typeof ALL;
  paymentMethod: PaymentMethod | typeof ALL;
  category: TransactionCategory | typeof ALL;
  professionalUserId: string | typeof ALL;
  contactId: string | undefined;
};

const toDraft = (values: FinanceFilterValues): Draft => ({
  status: values.status ?? ALL,
  paymentMethod: values.paymentMethod ?? ALL,
  category: values.category ?? ALL,
  professionalUserId: values.professionalUserId ?? ALL,
  contactId: values.contactId,
});

const toValues = (draft: Draft): FinanceFilterValues => ({
  status: draft.status === ALL ? undefined : draft.status,
  paymentMethod: draft.paymentMethod === ALL ? undefined : draft.paymentMethod,
  category: draft.category === ALL ? undefined : draft.category,
  professionalUserId: draft.professionalUserId === ALL ? undefined : draft.professionalUserId,
  contactId: draft.contactId,
});

const STATUS_OPTIONS: TransactionStatusFilter[] = ["PENDING", "PAID", "OVERDUE", "CANCELLED"];

export function FinanceFiltersSheet({
  open,
  onOpenChange,
  tab,
  companyId,
  initial,
  onApply,
  onClear,
}: FinanceFiltersSheetProps) {
  const [draft, setDraft] = useState<Draft>(() => toDraft(initial));

  const { data: contacts = [] } = useContacts(companyId);
  const { data: members = [] } = useMembers(companyId);

  useEffect(() => {
    if (open) setDraft(toDraft(initial));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const incomeCategories = CATEGORIES_BY_TYPE.INCOME;
  const expenseCategories = CATEGORIES_BY_TYPE.EXPENSE;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex w-full flex-col gap-6 sm:max-w-sm">
        <SheetHeader>
          <SheetTitle>Filtrar</SheetTitle>
        </SheetHeader>

        <div className="flex-1 space-y-5 overflow-y-auto">
          <div className="space-y-2">
            <Label>Status</Label>
            <Select
              value={draft.status}
              onValueChange={(value) =>
                setDraft((d) => ({ ...d, status: value as Draft["status"] }))
              }
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Todos</SelectItem>
                {STATUS_OPTIONS.map((status) => (
                  <SelectItem key={status} value={status}>
                    {TRANSACTION_STATUS_LABELS[status]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Forma de pagamento</Label>
            <Select
              value={draft.paymentMethod}
              onValueChange={(value) =>
                setDraft((d) => ({ ...d, paymentMethod: value as Draft["paymentMethod"] }))
              }
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Todas</SelectItem>
                {Object.entries(PAYMENT_METHOD_LABELS).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Categoria</Label>
            <Select
              value={draft.category}
              onValueChange={(value) =>
                setDraft((d) => ({ ...d, category: value as Draft["category"] }))
              }
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Todas</SelectItem>
                {tab === "income" &&
                  incomeCategories.map((category) => (
                    <SelectItem key={category} value={category}>
                      {TRANSACTION_CATEGORY_LABELS[category]}
                    </SelectItem>
                  ))}
                {tab === "expense" &&
                  expenseCategories.map((category) => (
                    <SelectItem key={category} value={category}>
                      {TRANSACTION_CATEGORY_LABELS[category]}
                    </SelectItem>
                  ))}
                {tab === "all" && (
                  <>
                    <SelectSeparator />
                    <SelectGroup>
                      <SelectLabel>Receitas</SelectLabel>
                      {incomeCategories.map((category) => (
                        <SelectItem key={category} value={category}>
                          {TRANSACTION_CATEGORY_LABELS[category]}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                    <SelectSeparator />
                    <SelectGroup>
                      <SelectLabel>Despesas</SelectLabel>
                      {expenseCategories.map((category) => (
                        <SelectItem key={category} value={category}>
                          {TRANSACTION_CATEGORY_LABELS[category]}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  </>
                )}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Profissional</Label>
            <Select
              value={draft.professionalUserId}
              onValueChange={(value) =>
                setDraft((d) => ({
                  ...d,
                  professionalUserId: value as Draft["professionalUserId"],
                }))
              }
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Todos</SelectItem>
                {members.map((member) => (
                  <SelectItem key={member.userId} value={member.userId}>
                    {member.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Paciente</Label>
            <ContactCombobox
              contacts={contacts}
              value={draft.contactId}
              onChange={(id) => setDraft((d) => ({ ...d, contactId: id }))}
              placeholder="Todos os pacientes"
            />
          </div>
        </div>

        <SheetFooter className="flex-col-reverse gap-2 sm:flex-col-reverse sm:space-x-0">
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              onClear();
              onOpenChange(false);
            }}
          >
            Limpar filtros
          </Button>
          <Button
            type="button"
            onClick={() => {
              onApply(toValues(draft));
              onOpenChange(false);
            }}
          >
            Aplicar
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
