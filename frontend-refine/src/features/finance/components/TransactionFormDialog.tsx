"use client";

import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { format, parseISO } from "date-fns";
import { CalendarIcon, Check, ChevronsUpDown } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { ContactCombobox } from "@/features/sales/scheduling/components/ContactCombobox";
import { useCatalog } from "@/features/masterdata/catalog/hooks/useCatalog";
import type { CatalogItem } from "@/features/masterdata/catalog/types/catalog.types";
import { useContacts } from "@/features/masterdata/contacts/hooks/useContacts";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { cn } from "@/lib/utils";

import { useCreateTransaction, useUpdateTransaction } from "../hooks/useFinance";
import { transactionFormSchema, type TransactionFormValues } from "../schemas/transaction.schema";
import type {
  CreateFinancialTransactionRequest,
  FinancialTransaction,
  TransactionCategory,
  TransactionType,
} from "../types/finance.types";
import {
  CATEGORY_GROUPS,
  PAYMENT_METHOD_LABELS,
  TRANSACTION_CATEGORY_LABELS,
  TRANSACTION_TYPE_LABELS,
} from "../types/finance.types";
import { describeInstallments } from "../utils/installments";
import { centsToBRL, parseBRLToCents } from "../utils/money";

type TransactionFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string | null;
  /** Quando informado, o diálogo entra em modo edição. */
  transaction?: FinancialTransaction | null;
  /** Tipo pré-selecionado na criação; quando informado, o tipo fica travado. */
  defaultType?: TransactionType;
  /** Paciente pré-selecionado e travado na criação (ex.: aberto pela ficha). */
  lockedContact?: { id: string; name: string };
};

const maskBRL = (raw: string): string => {
  const digits = raw.replace(/\D/g, "").slice(0, 12);
  if (!digits) return "";
  return (Number(digits) / 100).toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
};

const centsToInput = (cents: number): string =>
  (cents / 100).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const today = (): string => format(new Date(), "yyyy-MM-dd");

const emptyValues = (type: TransactionType = "INCOME"): TransactionFormValues => ({
  type,
  description: "",
  category: "",
  amount: "",
  paymentMethod: "",
  dueDate: today(),
  installments: 1,
  recurring: false,
  contactId: undefined,
  professionalUserId: undefined,
  catalogItemId: undefined,
  fee: "0",
  paid: false,
  paidAt: undefined,
  notes: "",
});

const fromTransaction = (transaction: FinancialTransaction): TransactionFormValues => ({
  type: transaction.type,
  description: transaction.description,
  category: transaction.category,
  amount: centsToInput(transaction.amountCents),
  paymentMethod: transaction.paymentMethod,
  dueDate: transaction.dueDate,
  installments: 1,
  recurring: false,
  contactId: transaction.contactId ?? undefined,
  professionalUserId: transaction.professionalUserId ?? undefined,
  catalogItemId: transaction.catalogItemId ?? undefined,
  fee: transaction.feeCents ? centsToInput(transaction.feeCents) : "0",
  paid: transaction.status === "PAID",
  paidAt: transaction.paidAt ?? undefined,
  notes: transaction.notes ?? "",
});

/**
 * Monta o corpo da requisição.
 *
 * Decisão "Já foi pago" na criação: o CreateFinancialTransactionRequest aceita
 * `paidAt` e o backend (FinancialTransaction.createSeries) já marca o lançamento
 * como PAID quando esse campo vem preenchido. Por isso enviamos `paidAt` direto
 * na criação, sem uma segunda chamada de /pay.
 */
const buildBody = (values: TransactionFormValues): CreateFinancialTransactionRequest => {
  const amountCents = parseBRLToCents(values.amount) ?? 0;
  const feeCents = values.fee && values.fee.trim() !== "" ? (parseBRLToCents(values.fee) ?? 0) : 0;
  return {
    type: values.type as TransactionType,
    description: values.description.trim(),
    category: values.category as TransactionCategory,
    amountCents,
    feeCents,
    paymentMethod: values.paymentMethod as CreateFinancialTransactionRequest["paymentMethod"],
    dueDate: values.dueDate,
    contactId: values.contactId ?? null,
    professionalUserId: values.professionalUserId ?? null,
    catalogItemId: values.catalogItemId ?? null,
    notes: values.notes?.trim() ? values.notes.trim() : null,
    paidAt: values.paid ? (values.paidAt ?? null) : null,
    installments: values.installments ?? 1,
    recurring: (values.installments ?? 1) > 1 && Boolean(values.recurring),
  };
};

function OptionalLabel({ children }: { children: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-2">
      <Label>{children}</Label>
      <span className="text-xs text-muted-foreground">Opcional</span>
    </div>
  );
}

type DatePickerFieldProps = {
  value?: string;
  onChange: (value: string | undefined) => void;
  placeholder: string;
  disableFuture?: boolean;
};

function DatePickerField({ value, onChange, placeholder, disableFuture }: DatePickerFieldProps) {
  const selected = value ? parseISO(value) : undefined;
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          className={cn(
            "w-full justify-start text-left font-normal",
            !selected && "text-muted-foreground",
          )}
        >
          <CalendarIcon className="mr-2 h-4 w-4" />
          {selected ? format(selected, "dd/MM/yyyy") : placeholder}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-auto p-0">
        <Calendar
          mode="single"
          selected={selected}
          disabled={disableFuture ? { after: new Date() } : undefined}
          onSelect={(day) => onChange(day ? format(day, "yyyy-MM-dd") : undefined)}
        />
      </PopoverContent>
    </Popover>
  );
}

type CatalogComboboxProps = {
  items: CatalogItem[];
  value?: string;
  onChange: (value: string | undefined) => void;
};

function CatalogCombobox({ items, value, onChange }: CatalogComboboxProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    if (!search) return items;
    const term = search.toLowerCase();
    return items.filter((item) => item.name.toLowerCase().includes(term));
  }, [items, search]);

  const selected = items.find((item) => item.id === value);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className="w-full justify-between font-normal"
        >
          <span className={cn("truncate", !selected && "text-muted-foreground")}>
            {selected ? selected.name : "Selecionar procedimento"}
          </span>
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[--radix-popover-trigger-width] p-0" align="start">
        <Command shouldFilter={false}>
          <CommandInput
            placeholder="Buscar procedimento…"
            value={search}
            onValueChange={setSearch}
          />
          <CommandList>
            <CommandEmpty>Nenhum item encontrado.</CommandEmpty>
            <CommandGroup>
              {filtered.map((item) => (
                <CommandItem
                  key={item.id}
                  value={item.id}
                  onSelect={() => {
                    onChange(item.id === value ? undefined : item.id);
                    setOpen(false);
                    setSearch("");
                  }}
                >
                  <Check
                    className={cn("mr-2 h-4 w-4", value === item.id ? "opacity-100" : "opacity-0")}
                  />
                  <span className="truncate">{item.name}</span>
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}

export function TransactionFormDialog({
  open,
  onOpenChange,
  companyId,
  transaction,
  defaultType,
  lockedContact,
}: TransactionFormDialogProps) {
  const isEditing = !!transaction;

  const { data: contacts = [] } = useContacts(companyId);
  const { data: members = [] } = useMembers(companyId);
  const { data: catalogPage } = useCatalog(companyId, { active: true, pageSize: 100 });
  const catalogItems = catalogPage?.content ?? [];

  const createTransaction = useCreateTransaction(companyId);
  const updateTransaction = useUpdateTransaction(companyId);

  const form = useForm<TransactionFormValues>({
    resolver: zodResolver(transactionFormSchema),
    mode: "onChange",
    defaultValues: emptyValues(),
  });

  useEffect(() => {
    if (!open) return;
    form.reset(transaction ? fromTransaction(transaction) : emptyValues(defaultType));
  }, [open, transaction, defaultType, form]);

  useEffect(() => {
    if (open && !transaction && lockedContact) {
      form.setValue("contactId", lockedContact.id);
    }
  }, [open, transaction, lockedContact, form]);

  const values = form.watch();
  const isValid = transactionFormSchema.safeParse(values).success;
  const isSaving = createTransaction.isPending || updateTransaction.isPending;

  const amountCents = parseBRLToCents(values.amount) ?? 0;
  const count = values.installments ?? 1;
  const condition: "ONCE" | "INSTALLMENTS" | "RECURRING" =
    count <= 1 ? "ONCE" : values.recurring ? "RECURRING" : "INSTALLMENTS";
  const installmentSummary =
    condition === "RECURRING"
      ? `${count} lançamentos mensais de ${centsToBRL(amountCents)}`
      : describeInstallments(amountCents, count);

  const handleTypeChange = (next: TransactionType) => {
    form.setValue("type", next);
    form.setValue("category", "");
    if (next === "EXPENSE") form.setValue("catalogItemId", undefined);
  };

  const handleSubmit = form.handleSubmit((formValues) => {
    const body = buildBody(formValues);
    const onSuccess = () => {
      toast.success("Lançamento salvo");
      onOpenChange(false);
    };
    if (transaction) {
      updateTransaction.mutate({ id: transaction.id, body }, { onSuccess });
    } else {
      createTransaction.mutate(body, { onSuccess });
    }
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85dvh] overflow-y-auto sm:max-w-[560px]">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar lançamento" : "Novo lançamento"}</DialogTitle>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="type"
                render={({ field }) => (
                  <FormItem className="sm:col-span-2">
                    <FormLabel>Tipo</FormLabel>
                    <div className="flex gap-2">
                      {(["INCOME", "EXPENSE"] as const).map((option) => (
                        <button
                          key={option}
                          type="button"
                          disabled={!!defaultType}
                          onClick={() => handleTypeChange(option)}
                          className={cn(
                            "flex-1 rounded-md border px-3 py-2 text-sm font-medium transition-colors",
                            field.value === option
                              ? "border-primary bg-primary/10 text-primary"
                              : "border-input text-muted-foreground hover:bg-muted",
                            !!defaultType && "cursor-not-allowed opacity-70",
                          )}
                        >
                          {TRANSACTION_TYPE_LABELS[option]}
                        </button>
                      ))}
                    </div>
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Descrição</FormLabel>
                    <FormControl>
                      <Input placeholder="Ex.: Limpeza dental" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="category"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Categoria</FormLabel>
                    <Select value={field.value || undefined} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Selecione" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {CATEGORY_GROUPS.filter((group) => group.type === values.type).map(
                          (group) => (
                            <SelectGroup key={group.label}>
                              <SelectLabel>{group.label}</SelectLabel>
                              {group.categories.map((category) => (
                                <SelectItem key={category} value={category}>
                                  {TRANSACTION_CATEGORY_LABELS[category]}
                                </SelectItem>
                              ))}
                            </SelectGroup>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="amount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Valor</FormLabel>
                    <div className="relative">
                      <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                        R$
                      </span>
                      <FormControl>
                        <Input
                          inputMode="numeric"
                          className="pl-9"
                          placeholder="0,00"
                          value={field.value}
                          onChange={(event) => field.onChange(maskBRL(event.target.value))}
                        />
                      </FormControl>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="paymentMethod"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Forma de pagamento</FormLabel>
                    <Select value={field.value || undefined} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Selecione" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {Object.entries(PAYMENT_METHOD_LABELS).map(([value, label]) => (
                          <SelectItem key={value} value={value}>
                            {label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="dueDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Vencimento</FormLabel>
                    <DatePickerField
                      value={field.value}
                      onChange={field.onChange}
                      placeholder="Selecione a data"
                    />
                    <FormMessage />
                  </FormItem>
                )}
              />

              {!isEditing && (
                <FormItem>
                  <FormLabel>Condição de pagamento</FormLabel>
                  <Select
                    value={condition}
                    onValueChange={(value) => {
                      const next = value as typeof condition;
                      form.setValue("recurring", next === "RECURRING");
                      if (next === "ONCE") form.setValue("installments", 1);
                      else if (count <= 1)
                        form.setValue("installments", next === "RECURRING" ? 12 : 2);
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ONCE">À vista</SelectItem>
                      <SelectItem value="INSTALLMENTS">Parcelado</SelectItem>
                      <SelectItem value="RECURRING">Recorrente (todo mês)</SelectItem>
                    </SelectContent>
                  </Select>
                </FormItem>
              )}

              {!isEditing && condition !== "ONCE" && (
                <FormField
                  control={form.control}
                  name="installments"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>
                        {condition === "RECURRING" ? "Repetir por" : "Parcelas"}
                      </FormLabel>
                      <Select
                        value={String(field.value ?? 2)}
                        onValueChange={(value) => field.onChange(Number(value))}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent className="max-h-60">
                          {Array.from({ length: 23 }, (_, index) => {
                            const n = index + 2;
                            return (
                              <SelectItem key={n} value={String(n)}>
                                {condition === "RECURRING" ? `${n} meses` : `${n}x`}
                              </SelectItem>
                            );
                          })}
                        </SelectContent>
                      </Select>
                      {installmentSummary && (
                        <p className="text-xs text-muted-foreground">{installmentSummary}</p>
                      )}
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}

              <FormItem className="sm:col-span-1">
                <OptionalLabel>Paciente</OptionalLabel>
                {lockedContact ? (
                  <div className="flex h-9 items-center rounded-md border border-input bg-muted/50 px-3 text-sm">
                    {lockedContact.name}
                  </div>
                ) : (
                  <ContactCombobox
                    contacts={contacts}
                    value={values.contactId}
                    onChange={(id) => form.setValue("contactId", id)}
                    placeholder="Selecione um paciente"
                  />
                )}
              </FormItem>

              <div className="space-y-2">
                <OptionalLabel>Profissional</OptionalLabel>
                <Select
                  value={values.professionalUserId ?? "none"}
                  onValueChange={(value) =>
                    form.setValue("professionalUserId", value === "none" ? undefined : value)
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Selecione" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">Nenhum</SelectItem>
                    {members.map((member) => (
                      <SelectItem key={member.userId} value={member.userId}>
                        {member.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {values.type === "INCOME" && (
                <FormItem>
                  <OptionalLabel>Procedimento</OptionalLabel>
                  <CatalogCombobox
                    items={catalogItems}
                    value={values.catalogItemId}
                    onChange={(id) => form.setValue("catalogItemId", id)}
                  />
                </FormItem>
              )}

              <FormField
                control={form.control}
                name="fee"
                render={({ field }) => (
                  <FormItem>
                    <OptionalLabel>Taxa</OptionalLabel>
                    <div className="relative">
                      <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                        R$
                      </span>
                      <FormControl>
                        <Input
                          inputMode="numeric"
                          className="pl-9"
                          placeholder="0,00"
                          value={field.value ?? ""}
                          onChange={(event) => field.onChange(maskBRL(event.target.value))}
                        />
                      </FormControl>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {isEditing && transaction?.installmentTotal && transaction.installmentTotal > 1 && (
                <p className="text-xs text-muted-foreground sm:col-span-2">
                  Este é o lançamento {transaction.installmentNumber} de{" "}
                  {transaction.installmentTotal} da série; a edição vale só para ele.
                </p>
              )}

              <FormField
                control={form.control}
                name="paid"
                render={({ field }) => (
                  <FormItem className="sm:col-span-2">
                    <label className="flex items-center gap-2 text-sm font-medium">
                      <FormControl>
                        <Checkbox
                          checked={!!field.value}
                          onCheckedChange={(checked) => field.onChange(checked === true)}
                        />
                      </FormControl>
                      Já foi pago
                    </label>
                  </FormItem>
                )}
              />

              {values.paid && (
                <FormField
                  control={form.control}
                  name="paidAt"
                  render={({ field }) => (
                    <FormItem className="sm:col-span-2">
                      <FormLabel>Data do pagamento</FormLabel>
                      <DatePickerField
                        value={field.value}
                        onChange={field.onChange}
                        placeholder="Selecione a data"
                        disableFuture
                      />
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}

              <FormField
                control={form.control}
                name="notes"
                render={({ field }) => (
                  <FormItem className="sm:col-span-2">
                    <OptionalLabel>Observações</OptionalLabel>
                    <FormControl>
                      <Textarea rows={3} placeholder="Anotações sobre o lançamento" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <DialogFooter className="gap-2 sm:justify-stretch">
              <Button
                type="button"
                variant="outline"
                className="flex-1"
                onClick={() => onOpenChange(false)}
              >
                Cancelar
              </Button>
              <Button type="submit" className="flex-1" disabled={!isValid || isSaving}>
                {isSaving ? "Salvando…" : "Salvar"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
