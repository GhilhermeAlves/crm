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
  CATALOG_TYPE_LABELS,
  type CatalogItem,
  type CatalogItemRequest,
  type CatalogItemType,
} from "../types/catalog.types";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  item: CatalogItem | null;
  isSubmitting: boolean;
  onSubmit: (request: CatalogItemRequest) => void;
};

type FormState = {
  type: CatalogItemType;
  name: string;
  description: string;
  category: string;
  price: string;
  sku: string;
};

function initialState(item: CatalogItem | null): FormState {
  return {
    type: item?.type ?? "PRODUCT",
    name: item?.name ?? "",
    description: item?.description ?? "",
    category: item?.category ?? "",
    price: item?.price != null ? String(item.price).replace(".", ",") : "",
    sku: item?.sku ?? "",
  };
}

/** Aceita "1.234,56", "1234,56" ou "1234.56". Vazio = sob consulta. */
function parsePrice(raw: string): number | null | "invalid" {
  const value = raw.trim();
  if (!value) return null;
  const normalized = value.includes(",") ? value.replace(/\./g, "").replace(",", ".") : value;
  const parsed = Number(normalized);
  return Number.isFinite(parsed) && parsed >= 0 ? Math.round(parsed * 100) / 100 : "invalid";
}

export function CatalogItemDialog({ open, onOpenChange, item, isSubmitting, onSubmit }: Props) {
  const [form, setForm] = useState<FormState>(initialState(item));

  useEffect(() => {
    if (open) setForm(initialState(item));
  }, [open, item]);

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) {
      toast.error("Preencha o nome do item.");
      return;
    }
    const price = parsePrice(form.price);
    if (price === "invalid") {
      toast.error("Preço inválido. Use, por exemplo, 199,90.");
      return;
    }
    onSubmit({
      type: form.type,
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      category: form.category.trim() || undefined,
      price,
      currency: "BRL",
      sku: form.sku.trim() || undefined,
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{item ? "Editar item" : "Novo item do catálogo"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="catalog-type">Tipo</Label>
              <Select value={form.type} onValueChange={(v) => set("type", v as CatalogItemType)}>
                <SelectTrigger id="catalog-type">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(CATALOG_TYPE_LABELS) as CatalogItemType[]).map((t) => (
                    <SelectItem key={t} value={t}>
                      {CATALOG_TYPE_LABELS[t]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="catalog-category">Categoria</Label>
              <Input
                id="catalog-category"
                placeholder="Ex.: Planos"
                value={form.category}
                onChange={(e) => set("category", e.target.value)}
                maxLength={80}
              />
            </div>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="catalog-name">Nome</Label>
            <Input
              id="catalog-name"
              placeholder="Ex.: Plano Profissional"
              value={form.name}
              onChange={(e) => set("name", e.target.value)}
              maxLength={160}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="catalog-description">Descrição</Label>
            <Textarea
              id="catalog-description"
              placeholder="O que o cliente precisa saber sobre este item (o agente de IA usa este texto)."
              value={form.description}
              onChange={(e) => set("description", e.target.value)}
              rows={3}
              maxLength={4000}
            />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="catalog-price">Preço (R$)</Label>
              <Input
                id="catalog-price"
                inputMode="decimal"
                placeholder="Vazio = sob consulta"
                value={form.price}
                onChange={(e) => set("price", e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="catalog-sku">SKU</Label>
              <Input
                id="catalog-sku"
                placeholder="Opcional"
                value={form.sku}
                onChange={(e) => set("sku", e.target.value)}
                maxLength={64}
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Salvando…" : "Salvar"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
