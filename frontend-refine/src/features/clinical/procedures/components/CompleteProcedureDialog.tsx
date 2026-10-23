"use client";

import { useEffect, useState } from "react";
import { format } from "date-fns";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { useCompleteProcedure } from "../hooks/useProcedures";
import type { PatientProcedureResponse } from "../types/procedure.types";

type Props = {
  companyId: string;
  contactId: string;
  procedure: PatientProcedureResponse | null;
  onOpenChange: (open: boolean) => void;
};

const NO_PROFESSIONAL = "none";
const today = () => format(new Date(), "yyyy-MM-dd");

export function CompleteProcedureDialog({ companyId, contactId, procedure, onOpenChange }: Props) {
  const { can } = useAuthorization();
  const { data: members } = useMembers(companyId);
  const complete = useCompleteProcedure(companyId, contactId);
  const [performedAt, setPerformedAt] = useState(today());
  const [professionalId, setProfessionalId] = useState<string>(NO_PROFESSIONAL);
  const [generateTransaction, setGenerateTransaction] = useState(false);
  const [dueDate, setDueDate] = useState(today());

  useEffect(() => {
    if (!procedure) return;
    setPerformedAt(today());
    setProfessionalId(procedure.professionalId ?? NO_PROFESSIONAL);
    setGenerateTransaction(procedure.generateTransactionOnDone);
    setDueDate(today());
  }, [procedure]);

  const budgetApproved = procedure?.budget?.status === "APPROVED";
  const canFinance = can("finance:create") && !budgetApproved;

  const confirm = () => {
    if (!procedure) return;
    complete.mutate(
      {
        procedureId: procedure.id,
        payload: {
          performedAt,
          professionalId: professionalId === NO_PROFESSIONAL ? null : professionalId,
          generateTransaction: canFinance && generateTransaction,
          dueDate: canFinance && generateTransaction ? dueDate : null,
        },
      },
      { onSuccess: () => onOpenChange(false) },
    );
  };

  return (
    <Dialog open={procedure != null} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Marcar como realizado</DialogTitle>
          <DialogDescription>{procedure?.name}</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="complete-performed-at">Data de realização</Label>
            <Input
              id="complete-performed-at"
              type="date"
              value={performedAt}
              onChange={(e) => setPerformedAt(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="complete-professional">Profissional</Label>
            <Select value={professionalId} onValueChange={setProfessionalId}>
              <SelectTrigger id="complete-professional">
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
          {budgetApproved && procedure?.budget && (
            <p className="text-sm text-muted-foreground">
              Cobrado pelo orçamento {procedure.budget.title}.
            </p>
          )}
          {canFinance && (
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Checkbox
                  id="complete-generate"
                  checked={generateTransaction}
                  onCheckedChange={(v) => setGenerateTransaction(v === true)}
                />
                <Label htmlFor="complete-generate">Gerar lançamento financeiro</Label>
              </div>
              {generateTransaction && (
                <div className="space-y-2">
                  <Label htmlFor="complete-due">Vencimento</Label>
                  <Input
                    id="complete-due"
                    type="date"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                  />
                </div>
              )}
            </div>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button onClick={confirm} disabled={complete.isPending || !performedAt}>
            Confirmar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
