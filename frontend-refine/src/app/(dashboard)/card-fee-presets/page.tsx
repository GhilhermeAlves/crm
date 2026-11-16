"use client";

import { useState } from "react";
import { format, parseISO } from "date-fns";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { RatesForm, draftsAreComplete } from "@/features/finance/credit/components/RatesForm";
import {
  mapToRates,
  ratesToMap,
  useAdminCardFeePresets,
  useDeactivateCardFeePreset,
  useSaveCardFeePreset,
  type CardFeePreset,
} from "@/features/finance/credit/hooks/useCardFees";
import {
  draftsFromRates,
  ratesFromDrafts,
  type RateDrafts,
} from "@/features/finance/credit/utils/rateInput";

type Editing = {
  id?: string;
  name: string;
  estimated: boolean;
  checkedAt: string;
  sourceNote: string;
  active: boolean;
  drafts: RateDrafts;
};

const today = () => format(new Date(), "yyyy-MM-dd");

function toEditing(p?: CardFeePreset): Editing {
  return {
    id: p?.id,
    name: p?.name ?? "",
    estimated: p?.estimated ?? false,
    checkedAt: p?.checkedAt ?? today(),
    sourceNote: p?.sourceNote ?? "",
    active: p?.active ?? true,
    drafts: draftsFromRates(ratesToMap(p?.rates)),
  };
}

export default function CardFeePresetsPage() {
  const { data: presets = [], isLoading } = useAdminCardFeePresets();
  const save = useSaveCardFeePreset();
  const deactivate = useDeactivateCardFeePreset();
  const [editing, setEditing] = useState<Editing | null>(null);

  const canSave =
    editing != null && editing.name.trim() !== "" && draftsAreComplete(editing.drafts);

  const onSave = () => {
    if (!editing) return;
    const rates = mapToRates(ratesFromDrafts(editing.drafts));
    if (!rates) return;
    save.mutate(
      {
        id: editing.id,
        input: {
          name: editing.name.trim(),
          estimated: editing.estimated,
          checkedAt: editing.checkedAt || null,
          sourceNote: editing.sourceNote.trim() || null,
          active: editing.active,
          rates,
        },
      },
      {
        onSuccess: () => {
          setEditing(null);
          toast.success("Modelo salvo");
        },
      },
    );
  };

  return (
    <div className="mx-auto w-full max-w-5xl space-y-6 p-4 sm:p-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Modelos de taxa</h1>
          <p className="text-sm text-muted-foreground">
            Taxas de referência das maquininhas, oferecidas a todas as clínicas como ponto de
            partida.
          </p>
        </div>
        <Button onClick={() => setEditing(toEditing())}>Novo modelo</Button>
      </div>

      <Card>
        <CardContent className="overflow-x-auto pt-6">
          {isLoading ? (
            <p className="text-sm text-muted-foreground">Carregando…</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Modelo</TableHead>
                  <TableHead>1x</TableHead>
                  <TableHead>12x</TableHead>
                  <TableHead>Conferido em</TableHead>
                  <TableHead>Situação</TableHead>
                  <TableHead>Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {presets.map((p) => {
                  const map = ratesToMap(p.rates);
                  return (
                    <TableRow key={p.id}>
                      <TableCell className="font-medium">{p.name}</TableCell>
                      <TableCell>{map[1] ?? "—"}%</TableCell>
                      <TableCell>{map[12] ?? "—"}%</TableCell>
                      <TableCell>
                        {p.checkedAt ? format(parseISO(p.checkedAt), "dd/MM/yyyy") : "—"}
                      </TableCell>
                      <TableCell className="space-x-1">
                        <Badge variant={p.active ? "outline" : "secondary"}>
                          {p.active ? "Ativo" : "Inativo"}
                        </Badge>
                        {p.estimated && <Badge variant="secondary">Estimado</Badge>}
                      </TableCell>
                      <TableCell className="space-x-1">
                        <Button size="sm" variant="ghost" onClick={() => setEditing(toEditing(p))}>
                          Editar
                        </Button>
                        {p.active && (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => deactivate.mutate(p.id)}
                            disabled={deactivate.isPending}
                          >
                            Desativar
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={editing != null} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>{editing?.id ? "Editar modelo" : "Novo modelo"}</DialogTitle>
          </DialogHeader>
          {editing && (
            <div className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="space-y-1">
                  <Label htmlFor="preset-name">Nome</Label>
                  <Input
                    id="preset-name"
                    maxLength={120}
                    placeholder="Ex.: Ton — 1 dia útil — até R$ 3 mil/mês"
                    value={editing.name}
                    onChange={(e) => setEditing({ ...editing, name: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <Label htmlFor="preset-checked">Conferido em</Label>
                  <Input
                    id="preset-checked"
                    type="date"
                    value={editing.checkedAt}
                    onChange={(e) => setEditing({ ...editing, checkedAt: e.target.value })}
                  />
                </div>
              </div>
              <RatesForm
                idPrefix="preset-rate"
                drafts={editing.drafts}
                onChange={(drafts) => setEditing({ ...editing, drafts })}
              />
              <div className="space-y-1">
                <Label htmlFor="preset-note">Observação</Label>
                <Textarea
                  id="preset-note"
                  maxLength={500}
                  value={editing.sourceNote}
                  onChange={(e) => setEditing({ ...editing, sourceNote: e.target.value })}
                />
              </div>
              <div className="flex flex-wrap gap-6">
                <div className="flex items-center gap-2">
                  <Checkbox
                    id="preset-estimated"
                    checked={editing.estimated}
                    onCheckedChange={(v) => setEditing({ ...editing, estimated: v === true })}
                  />
                  <Label htmlFor="preset-estimated">Parcelas 2x–11x estimadas</Label>
                </div>
                <div className="flex items-center gap-2">
                  <Checkbox
                    id="preset-active"
                    checked={editing.active}
                    onCheckedChange={(v) => setEditing({ ...editing, active: v === true })}
                  />
                  <Label htmlFor="preset-active">Ativo</Label>
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>
              Cancelar
            </Button>
            <Button onClick={onSave} disabled={!canSave || save.isPending}>
              {save.isPending ? "Salvando…" : "Salvar"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
