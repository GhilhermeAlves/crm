"use client";

import { useState } from "react";
import { ArrowDown, ArrowUp, Plus, Trash2 } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useSchedulingPermissions } from "@/features/sales/scheduling/schemas/scheduling.schema";
import {
  useChairs,
  useCreateChair,
  useDeleteChair,
  useUpdateChair,
} from "@/features/sales/chair/hooks/useChairs";
import type { Chair } from "@/features/sales/chair/types/chair.types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";

const DEFAULT_COLOR = "#0EA5E9";

export default function ChairsSettingsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { canConfigure } = useSchedulingPermissions();
  const { data: chairs = [], isLoading } = useChairs(companyId);
  const createChair = useCreateChair(companyId);
  const updateChair = useUpdateChair(companyId);
  const deleteChair = useDeleteChair(companyId);

  const [name, setName] = useState("");
  const [color, setColor] = useState(DEFAULT_COLOR);
  const [toDelete, setToDelete] = useState<Chair | null>(null);

  const sorted = [...chairs].sort(
    (a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name),
  );

  const add = () => {
    if (!name.trim()) return;
    const sortOrder = sorted.length ? Math.max(...sorted.map((c) => c.sortOrder)) + 1 : 0;
    createChair.mutate(
      { name: name.trim(), color, active: true, sortOrder },
      { onSuccess: () => setName("") },
    );
  };

  /** Troca a posição com a cadeira vizinha. */
  const move = (index: number, delta: -1 | 1) => {
    const a = sorted[index];
    const b = sorted[index + delta];
    if (!a || !b) return;
    updateChair.mutate({ id: a.id, data: { sortOrder: index + delta } });
    updateChair.mutate({ id: b.id, data: { sortOrder: index } });
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h2 className="text-xl font-semibold">Cadeiras</h2>
        <p className="text-sm text-muted-foreground">
          Cadastre as cadeiras (consultórios) para escolher no agendamento e ver a agenda por
          cadeira. Duas consultas não podem ocupar a mesma cadeira no mesmo horário.
        </p>
      </div>

      {canConfigure && (
        <div className="flex items-center gap-2 rounded-lg border bg-card p-3">
          <input
            type="color"
            aria-label="Cor da nova cadeira"
            value={color}
            onChange={(e) => setColor(e.target.value)}
            className="h-9 w-9 shrink-0 cursor-pointer rounded border bg-transparent"
          />
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && add()}
            placeholder="Nome da cadeira (ex.: Cadeira 1)"
            aria-label="Nome da nova cadeira"
          />
          <Button onClick={add} disabled={!name.trim() || createChair.isPending}>
            <Plus className="h-4 w-4" />
            Adicionar
          </Button>
        </div>
      )}

      <div className="divide-y rounded-lg border bg-card">
        {isLoading && <p className="p-4 text-sm text-muted-foreground">Carregando…</p>}
        {!isLoading && sorted.length === 0 && (
          <p className="p-4 text-sm text-muted-foreground">Nenhuma cadeira cadastrada.</p>
        )}
        {sorted.map((chair, index) => (
          <div key={chair.id} className="flex items-center gap-2 p-3">
            <input
              type="color"
              aria-label={`Cor de ${chair.name}`}
              defaultValue={chair.color ?? DEFAULT_COLOR}
              disabled={!canConfigure}
              onBlur={(e) =>
                e.target.value.toUpperCase() !== (chair.color ?? "").toUpperCase() &&
                updateChair.mutate({ id: chair.id, data: { color: e.target.value } })
              }
              className="h-8 w-8 shrink-0 cursor-pointer rounded border bg-transparent"
            />
            <Input
              defaultValue={chair.name}
              aria-label={`Nome de ${chair.name}`}
              disabled={!canConfigure}
              onBlur={(e) => {
                const value = e.target.value.trim();
                if (value && value !== chair.name) {
                  updateChair.mutate({ id: chair.id, data: { name: value } });
                }
              }}
              className="h-8"
            />
            <label className="flex shrink-0 items-center gap-2 text-xs text-muted-foreground">
              Ativa
              <Switch
                checked={chair.active}
                disabled={!canConfigure}
                aria-label={`${chair.name} ativa`}
                onCheckedChange={(active) => updateChair.mutate({ id: chair.id, data: { active } })}
              />
            </label>
            {canConfigure && (
              <div className="flex shrink-0">
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  aria-label={`Subir ${chair.name}`}
                  disabled={index === 0}
                  onClick={() => move(index, -1)}
                >
                  <ArrowUp className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  aria-label={`Descer ${chair.name}`}
                  disabled={index === sorted.length - 1}
                  onClick={() => move(index, 1)}
                >
                  <ArrowDown className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  aria-label={`Excluir ${chair.name}`}
                  onClick={() => setToDelete(chair)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            )}
          </div>
        ))}
      </div>

      <ConfirmDialog
        open={!!toDelete}
        onOpenChange={(open) => !open && setToDelete(null)}
        title="Excluir cadeira?"
        description="Os agendamentos dessa cadeira continuam na agenda, mas ficam sem cadeira. Para só esconder a cadeira, desative-a."
        confirmLabel="Excluir"
        variant="destructive"
        onConfirm={() => {
          if (toDelete) deleteChair.mutate(toDelete.id);
          setToDelete(null);
        }}
      />
    </div>
  );
}
