"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { Deal, DealFormValues } from "../types/deal.types";
import {
  dealFormSchema,
  dealFormDefaultValues,
  dealStages,
  forecastCategories,
} from "../schemas/deal.schema";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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

type DealFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (values: DealFormValues) => void;
  deal?: Deal | null;
};

export function DealFormDialog({ open, onOpenChange, onSubmit, deal }: DealFormDialogProps) {
  const isEdit = !!deal;
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<DealFormValues>({
    resolver: zodResolver(dealFormSchema),
    defaultValues: dealFormDefaultValues(deal ?? undefined),
  });

  const watchedStage = watch("stage");
  const watchedForecast = watch("forecastCategory");

  const close = () => {
    reset(dealFormDefaultValues());
    onOpenChange(false);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        if (!o) {
          reset(dealFormDefaultValues());
        }
        onOpenChange(o);
      }}
    >
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Editar Negociação" : "Nova Negociação"}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? "Atualize os dados da negociação."
              : "Preencha os dados da nova oportunidade comercial."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="name">Nome da oportunidade *</Label>
            <Input id="name" placeholder="Ex.: Negociação Google" {...register("name")} />
            {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label>Etapa</Label>
              <Select
                value={watchedStage}
                onValueChange={(val) => setValue("stage", val as DealFormValues["stage"])}
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
            <div className="space-y-1.5">
              <Label htmlFor="responsible">Responsável</Label>
              <Input
                id="responsible"
                placeholder="Responsável pela negociação"
                {...register("responsible")}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="value">Valor da negociação</Label>
              <Input id="value" placeholder="70000" inputMode="numeric" {...register("value")} />
              {errors.value && <p className="text-sm text-destructive">{errors.value.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="expectedValue">Valor previsto</Label>
              <Input
                id="expectedValue"
                placeholder="60000"
                inputMode="numeric"
                {...register("expectedValue")}
              />
              {errors.expectedValue && (
                <p className="text-sm text-destructive">{errors.expectedValue.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="contact">Contato *</Label>
            <Input id="contact" placeholder="Nome do contato" {...register("contact")} />
            {errors.contact && <p className="text-sm text-destructive">{errors.contact.message}</p>}
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="expectedCloseDate">Data de fechamento esperada</Label>
              <Input id="expectedCloseDate" type="date" {...register("expectedCloseDate")} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="probability">Probabilidade (%)</Label>
              <Input
                id="probability"
                placeholder="90"
                inputMode="numeric"
                {...register("probability")}
              />
              {errors.probability && (
                <p className="text-sm text-destructive">{errors.probability.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>Categoria de previsão</Label>
            <Select
              value={watchedForecast || "none"}
              onValueChange={(val) => setValue("forecastCategory", val === "none" ? "" : val)}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">Sem categoria</SelectItem>
                {forecastCategories.map((c) => (
                  <SelectItem key={c} value={c}>
                    {c}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={close}>
              Cancelar
            </Button>
            <Button type="submit">{isEdit ? "Salvar alterações" : "Criar negociação"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
