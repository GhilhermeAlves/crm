"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  STOCK_MOVEMENT_TYPE_LABELS,
  STOCK_MOVEMENT_TYPES,
  type InventoryItem,
  type RegisterMovementRequest,
  type StockMovementType,
} from "../types/inventory.types";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  item: InventoryItem;
  isSubmitting: boolean;
  onSubmit: (request: RegisterMovementRequest) => void;
};

type FormState = {
  type: StockMovementType;
  quantity: string;
  note: string;
};

const initialState: FormState = { type: "ENTRADA", quantity: "", note: "" };

export function StockMovementDialog({ open, onOpenChange, item, isSubmitting, onSubmit }: Props) {
  const [form, setForm] = useState<FormState>(initialState);

  useEffect(() => {
    if (open) setForm(initialState);
  }, [open]);

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const quantity = Number(form.quantity);
    if (!form.quantity.trim() || !Number.isInteger(quantity) || quantity < 1) {
      toast.error("Informe uma quantidade inteira maior que zero.");
      return;
    }
    onSubmit({
      type: form.type,
      quantity,
      note: form.note.trim() || undefined,
    });
  };

  const quantityLabel = form.type === "AJUSTE" ? "Contagem real" : "Quantidade";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Movimentar estoque — {item.name}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="movement-type">Tipo</Label>
            <Select value={form.type} onValueChange={(v) => set("type", v as StockMovementType)}>
              <SelectTrigger id="movement-type">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STOCK_MOVEMENT_TYPES.map((type) => (
                  <SelectItem key={type} value={type}>
                    {STOCK_MOVEMENT_TYPE_LABELS[type]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="movement-quantity">{quantityLabel}</Label>
            <Input
              id="movement-quantity"
              type="number"
              inputMode="numeric"
              min={1}
              step={1}
              value={form.quantity}
              onChange={(e) => set("quantity", e.target.value)}
            />
            {form.type === "AJUSTE" ? (
              <p className="text-xs text-muted-foreground">
                Quantidade real contada. O sistema calcula a diferença (atual: {item.quantity}).
              </p>
            ) : null}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="movement-note">Observação</Label>
            <Textarea
              id="movement-note"
              value={form.note}
              onChange={(e) => set("note", e.target.value)}
              rows={2}
              maxLength={500}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Salvando…" : "Registrar"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
