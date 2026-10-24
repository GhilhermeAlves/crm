"use client";

import { useEffect, useId, useState } from "react";
import { ChevronDown } from "lucide-react";
import { format } from "date-fns";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Textarea } from "@/components/ui/textarea";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { useAllCatalog } from "@/features/masterdata/catalog/hooks/useCatalog";
import { useCreateProcedure, useUpdateProcedure } from "../hooks/useProcedures";
import type {
  Dentition,
  PatientProcedureRequest,
  PatientProcedureResponse,
  ProcedureState,
  ProcedureTooth,
  ToothFace,
} from "../types/procedure.types";
import { centsToMask, maskToCents } from "../utils/currency";
import { DENTITION_LABELS, FACES, QUADRANTS, STATE_LABELS } from "../utils/teeth";

type Props = {
  companyId: string;
  contactId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  procedure?: PatientProcedureResponse | null;
  /** Dente clicado no odontograma: abre "Novo procedimento" com ele já marcado. */
  initialTooth?: number | null;
  initialDentition?: Dentition;
  /** Seção de dentes e faces: só em clínicas de odontologia. */
  showTeeth?: boolean;
};

const NO_PROFESSIONAL = "none";
const STATES: ProcedureState[] = ["PLANNED", "DONE", "PRE_EXISTING"];
const today = () => format(new Date(), "yyyy-MM-dd");
const formatBRL = (reais: number) =>
  reais.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export function ProcedureDrawer({
  companyId,
  contactId,
  open,
  onOpenChange,
  procedure,
  initialTooth,
  initialDentition,
  showTeeth = true,
}: Props) {
  const uid = useId();
  const { can } = useAuthorization();
  const { data: catalog } = useAllCatalog(companyId, { type: "SERVICE", active: true });
  const { data: members } = useMembers(companyId);
  const create = useCreateProcedure(companyId, contactId);
  const update = useUpdateProcedure(companyId, contactId);

  const [state, setState] = useState<ProcedureState>("PLANNED");
  const [catalogItemId, setCatalogItemId] = useState("");
  const [priceCents, setPriceCents] = useState<number | null>(null);
  const [dentition, setDentition] = useState<Dentition>("PERMANENT");
  const [teeth, setTeeth] = useState<ProcedureTooth[]>([]);
  const [professionalId, setProfessionalId] = useState(NO_PROFESSIONAL);
  const [notes, setNotes] = useState("");
  const [performedAt, setPerformedAt] = useState(today());
  const [generateTransaction, setGenerateTransaction] = useState(false);
  const [dueDate, setDueDate] = useState(today());

  useEffect(() => {
    if (!open) return;
    setState(procedure?.state ?? "PLANNED");
    setCatalogItemId(procedure?.catalogItemId ?? "");
    setPriceCents(procedure ? procedure.priceCents : null);
    if (procedure) {
      setDentition(procedure.dentition);
      setTeeth(procedure.teeth);
    } else {
      setDentition(initialDentition ?? "PERMANENT");
      setTeeth(initialTooth != null ? [{ tooth: initialTooth, faces: [] }] : []);
    }
    setProfessionalId(procedure?.professionalId ?? NO_PROFESSIONAL);
    setNotes(procedure?.notes ?? "");
    setPerformedAt(procedure?.performedAt ?? today());
    setGenerateTransaction(procedure?.generateTransactionOnDone ?? false);
    setDueDate(today());
  }, [open, procedure, initialTooth, initialDentition]);

  const locked = procedure?.state === "DONE";
  const editing = Boolean(procedure);
  const canFinance = can("finance:create");
  // Procedimento de orçamento aprovado já é cobrado pelo orçamento.
  const budgetApproved = procedure?.budget?.status === "APPROVED";
  const services = catalog ?? [];
  const isPending = create.isPending || update.isPending;

  const chooseService = (id: string) => {
    setCatalogItemId(id);
    const item = services.find((s) => s.id === id);
    setPriceCents(item?.price != null ? Math.round(item.price * 100) : null);
  };

  const toggleTooth = (tooth: number) => {
    if (locked) return;
    setTeeth((current) =>
      current.some((t) => t.tooth === tooth)
        ? current.filter((t) => t.tooth !== tooth)
        : [...current, { tooth, faces: [] }],
    );
  };

  const toggleFace = (tooth: number, face: ToothFace) => {
    setTeeth((current) =>
      current.map((t) =>
        t.tooth !== tooth
          ? t
          : {
              ...t,
              faces: t.faces.includes(face)
                ? t.faces.filter((f) => f !== face)
                : [...t.faces, face],
            },
      ),
    );
  };

  const changeDentition = (next: Dentition) => {
    if (locked || next === dentition) return;
    setDentition(next);
    setTeeth([]);
  };

  const save = () => {
    const withFinance = state === "DONE" && canFinance && generateTransaction;
    // Em "A realizar" o flag é guardado e só vira lançamento ao marcar como realizado.
    const generateOnDone =
      state === "PLANNED" &&
      !budgetApproved &&
      (canFinance ? generateTransaction : (procedure?.generateTransactionOnDone ?? false));
    const payload: PatientProcedureRequest =
      locked && procedure
        ? {
            catalogItemId: procedure.catalogItemId,
            state: procedure.state,
            dentition: procedure.dentition,
            priceCents: procedure.priceCents,
            professionalId: procedure.professionalId,
            notes: notes || null,
            performedAt: procedure.performedAt,
            teeth: procedure.teeth,
            generateTransaction: false,
            dueDate: null,
          }
        : {
            catalogItemId,
            state,
            dentition,
            priceCents: priceCents ?? 0,
            professionalId: professionalId === NO_PROFESSIONAL ? null : professionalId,
            notes: notes || null,
            performedAt: state === "DONE" ? performedAt : null,
            teeth,
            generateTransaction: withFinance || generateOnDone,
            dueDate: withFinance ? dueDate : null,
          };
    const options = { onSuccess: () => onOpenChange(false) };
    if (procedure) {
      update.mutate({ procedureId: procedure.id, payload }, options);
    } else {
      create.mutate(payload, options);
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{editing ? "Editar procedimento" : "Novo procedimento"}</SheetTitle>
        </SheetHeader>
        <div className="space-y-4 py-4">
          {locked && (
            <p className="rounded-md bg-muted p-3 text-sm text-muted-foreground">
              Procedimento realizado: só as observações podem ser alteradas.
            </p>
          )}

          <div className="space-y-2">
            <Label id={`${uid}-state-label`}>Estado</Label>
            <RadioGroup
              aria-labelledby={`${uid}-state-label`}
              value={state}
              onValueChange={(v) => setState(v as ProcedureState)}
              disabled={locked}
              className="flex flex-wrap gap-4"
            >
              {STATES.map((s) => (
                <div key={s} className="flex items-center gap-2">
                  <RadioGroupItem
                    id={`${uid}-state-${s}`}
                    value={s}
                    disabled={locked || (s === "DONE" && editing)}
                  />
                  <Label htmlFor={`${uid}-state-${s}`}>{STATE_LABELS[s]}</Label>
                </div>
              ))}
            </RadioGroup>
          </div>

          <div className="space-y-2">
            <Label htmlFor={`${uid}-service`}>Procedimentos</Label>
            <Select
              value={catalogItemId}
              onValueChange={chooseService}
              disabled={locked || editing}
            >
              <SelectTrigger id={`${uid}-service`}>
                <SelectValue placeholder="Selecione um procedimento" />
              </SelectTrigger>
              <SelectContent>
                {services.map((s) => (
                  <SelectItem key={s.id} value={s.id}>
                    {s.price != null ? `${s.name} — ${formatBRL(s.price)}` : s.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor={`${uid}-price`}>Valor (R$)</Label>
            <Input
              id={`${uid}-price`}
              type="text"
              inputMode="numeric"
              placeholder="0,00"
              disabled={locked}
              value={centsToMask(priceCents)}
              onChange={(e) => setPriceCents(maskToCents(e.target.value))}
            />
          </div>

          {state === "DONE" && (
            <div className="space-y-2">
              <Label htmlFor={`${uid}-performed-at`}>Data de realização</Label>
              <Input
                id={`${uid}-performed-at`}
                type="date"
                disabled={locked}
                value={performedAt}
                onChange={(e) => setPerformedAt(e.target.value)}
              />
            </div>
          )}

          {showTeeth && (
            <div className="space-y-2">
              <Label>Dentes</Label>
              <div className="flex gap-2">
                {(Object.keys(DENTITION_LABELS) as Dentition[]).map((value) => (
                  <Button
                    key={value}
                    type="button"
                    size="sm"
                    variant={value === dentition ? "default" : "outline"}
                    disabled={locked}
                    onClick={() => changeDentition(value)}
                  >
                    {DENTITION_LABELS[value]}
                  </Button>
                ))}
              </div>
              <ToothPicker
                dentition={dentition}
                selected={teeth.map((t) => t.tooth)}
                disabled={locked}
                onToggle={toggleTooth}
              />
              {teeth.map(({ tooth, faces }) => (
                <div key={tooth} className="flex flex-wrap items-center gap-1">
                  <span className="w-16 text-sm font-medium">Dente {tooth}</span>
                  {FACES.map((face) => (
                    <Button
                      key={face}
                      type="button"
                      size="sm"
                      variant={faces.includes(face) ? "default" : "outline"}
                      aria-label={`Face ${face} do dente ${tooth}`}
                      disabled={locked}
                      onClick={() => toggleFace(tooth, face)}
                    >
                      {face}
                    </Button>
                  ))}
                </div>
              ))}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor={`${uid}-professional`}>Profissional</Label>
            <Select value={professionalId} onValueChange={setProfessionalId} disabled={locked}>
              <SelectTrigger id={`${uid}-professional`}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_PROFESSIONAL}>Não informado</SelectItem>
                {(members ?? []).map((m) => (
                  <SelectItem key={m.userId} value={m.userId}>
                    {m.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor={`${uid}-notes`}>Observações</Label>
            <Textarea
              id={`${uid}-notes`}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          {budgetApproved && procedure?.budget && (
            <p className="text-sm text-muted-foreground">
              Cobrado pelo orçamento {procedure.budget.title}.
            </p>
          )}
          {canFinance && !locked && !budgetApproved && (
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Checkbox
                  id={`${uid}-generate`}
                  checked={state !== "PRE_EXISTING" && generateTransaction}
                  disabled={state === "PRE_EXISTING"}
                  onCheckedChange={(v) => setGenerateTransaction(v === true)}
                />
                <Label htmlFor={`${uid}-generate`}>Gerar lançamento financeiro</Label>
              </div>
              {state === "PLANNED" && (
                <p className="text-xs text-muted-foreground">
                  O lançamento será gerado quando o procedimento for marcado como realizado.
                </p>
              )}
              {state === "DONE" && generateTransaction && (
                <div className="space-y-2">
                  <Label htmlFor={`${uid}-due`}>Vencimento</Label>
                  <Input
                    id={`${uid}-due`}
                    type="date"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                  />
                </div>
              )}
            </div>
          )}
        </div>
        <SheetFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button onClick={save} disabled={isPending || !catalogItemId}>
            Salvar
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

type ToothPickerProps = {
  dentition: Dentition;
  selected: number[];
  disabled: boolean;
  onToggle: (tooth: number) => void;
};

/** Dropdown com os números dos dentes em toggle, por quadrante (vista do profissional, FDI). */
function ToothPicker({ dentition, selected, disabled, onToggle }: ToothPickerProps) {
  const [upperRight, upperLeft, lowerLeft, lowerRight] = QUADRANTS[dentition];
  const sorted = [...selected].sort((a, b) => a - b);
  const group = (teeth: number[], className: string) => (
    <div className={`flex gap-1 ${className}`}>
      {teeth.map((tooth) => {
        const pressed = selected.includes(tooth);
        return (
          <Button
            key={tooth}
            type="button"
            size="sm"
            variant={pressed ? "default" : "outline"}
            className="h-8 w-9 px-0 text-xs"
            aria-label={`Dente ${tooth}`}
            aria-pressed={pressed}
            disabled={disabled}
            onClick={() => onToggle(tooth)}
          >
            {tooth}
          </Button>
        );
      })}
    </div>
  );
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          disabled={disabled}
          aria-label="Selecionar dentes"
          className="h-auto min-h-10 w-full justify-between gap-2 font-normal"
        >
          {sorted.length === 0 ? (
            <span className="text-muted-foreground">Boca toda (selecione os dentes)</span>
          ) : (
            <span className="flex flex-wrap gap-1">
              {sorted.map((tooth) => (
                <span
                  key={tooth}
                  className="rounded bg-primary/10 px-1.5 py-0.5 text-xs font-semibold text-primary"
                >
                  {tooth}
                </span>
              ))}
            </span>
          )}
          <ChevronDown className="h-4 w-4 shrink-0 opacity-60" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-auto p-3">
        <div className="mb-2 flex justify-between text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
          <span>Direito</span>
          <span>Esquerdo</span>
        </div>
        <div className="grid w-max grid-cols-2 gap-x-2 gap-y-2">
          {group(upperRight, "justify-end border-b border-r pb-2 pr-2")}
          {group(upperLeft, "border-b pb-2")}
          {group(lowerRight.slice().reverse(), "justify-end border-r pr-2")}
          {group(lowerLeft, "")}
        </div>
      </PopoverContent>
    </Popover>
  );
}
