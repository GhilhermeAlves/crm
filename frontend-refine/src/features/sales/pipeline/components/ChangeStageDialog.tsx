"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import type { Deal, DealStage } from "../types/deal.types";
import { dealStages } from "../schemas/deal.schema";
import { DealStageBadge } from "./DealStageBadge";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const changeStageSchema = z.object({
  stage: z.enum(dealStages),
});
type ChangeStageValues = z.infer<typeof changeStageSchema>;

type ChangeStageDialogProps = {
  deal: Deal | null;
  onOpenChange: (open: boolean) => void;
  onConfirm: (stage: DealStage) => void;
};

export function ChangeStageDialog({ deal, onOpenChange, onConfirm }: ChangeStageDialogProps) {
  const { handleSubmit, setValue, watch } = useForm<ChangeStageValues>({
    resolver: zodResolver(changeStageSchema),
    defaultValues: { stage: deal?.stage ?? "Novo" },
  });

  const watchedStage = watch("stage");

  if (!deal) return null;

  return (
    <Dialog open={!!deal} onOpenChange={(o) => !o && onOpenChange(false)}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Alterar etapa</DialogTitle>
          <DialogDescription>
            Mova <strong>{deal.name}</strong> para uma nova etapa do funil.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit((values) => onConfirm(values.stage))} className="space-y-4">
          <div className="space-y-2">
            <Label>Etapa atual</Label>
            <div>
              <DealStageBadge stage={deal.stage} />
            </div>
          </div>
          <div className="space-y-2">
            <Label>Nova etapa</Label>
            <Select
              value={watchedStage}
              onValueChange={(val) => setValue("stage", val as DealStage)}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {dealStages.map((s) => (
                  <SelectItem key={s} value={s}>
                    {s}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit">Mover negociação</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
