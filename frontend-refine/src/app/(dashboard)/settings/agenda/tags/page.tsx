"use client";

import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useSchedulingPermissions } from "@/features/sales/scheduling/schemas/scheduling.schema";
import {
  useCreateTag,
  useDeleteTag,
  useTags,
  useUpdateTag,
} from "@/features/sales/tag/hooks/useTags";
import type { Tag } from "@/features/sales/tag/types/tag.types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";

const DEFAULT_COLOR = "#8B5CF6";

export default function TagsSettingsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { canConfigure } = useSchedulingPermissions();
  const { data: tags = [], isLoading } = useTags(companyId);
  const createTag = useCreateTag(companyId);
  const updateTag = useUpdateTag(companyId);
  const deleteTag = useDeleteTag(companyId);

  const [name, setName] = useState("");
  const [color, setColor] = useState(DEFAULT_COLOR);
  const [toDelete, setToDelete] = useState<Tag | null>(null);

  const sorted = [...tags].sort((a, b) => a.name.localeCompare(b.name));

  const add = () => {
    if (!name.trim()) return;
    createTag.mutate({ name: name.trim(), color }, { onSuccess: () => setName("") });
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h2 className="text-xl font-semibold">Marcadores</h2>
        <p className="text-sm text-muted-foreground">
          Etiquetas coloridas para classificar agendamentos e retornos (ex.: Ortodontia, Primeira
          consulta). Na agenda, dá para filtrar por marcador.
        </p>
      </div>

      {canConfigure && (
        <div className="flex items-center gap-2 rounded-lg border bg-card p-3">
          <input
            type="color"
            aria-label="Cor do novo marcador"
            value={color}
            onChange={(e) => setColor(e.target.value)}
            className="h-9 w-9 shrink-0 cursor-pointer rounded border bg-transparent"
          />
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && add()}
            placeholder="Nome do marcador"
            aria-label="Nome do novo marcador"
          />
          <Button onClick={add} disabled={!name.trim() || createTag.isPending}>
            <Plus className="h-4 w-4" />
            Adicionar
          </Button>
        </div>
      )}

      <div className="divide-y rounded-lg border bg-card">
        {isLoading && <p className="p-4 text-sm text-muted-foreground">Carregando…</p>}
        {!isLoading && sorted.length === 0 && (
          <p className="p-4 text-sm text-muted-foreground">Nenhum marcador cadastrado.</p>
        )}
        {sorted.map((tag) => (
          <div key={tag.id} className="flex items-center gap-2 p-3">
            <input
              type="color"
              aria-label={`Cor de ${tag.name}`}
              defaultValue={tag.color ?? DEFAULT_COLOR}
              disabled={!canConfigure}
              onBlur={(e) =>
                e.target.value.toUpperCase() !== (tag.color ?? "").toUpperCase() &&
                updateTag.mutate({ id: tag.id, data: { color: e.target.value } })
              }
              className="h-8 w-8 shrink-0 cursor-pointer rounded border bg-transparent"
            />
            <Input
              defaultValue={tag.name}
              aria-label={`Nome de ${tag.name}`}
              disabled={!canConfigure}
              onBlur={(e) => {
                const value = e.target.value.trim();
                if (value && value !== tag.name) {
                  updateTag.mutate({ id: tag.id, data: { name: value } });
                }
              }}
              className="h-8"
            />
            {canConfigure && (
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 shrink-0"
                aria-label={`Excluir ${tag.name}`}
                onClick={() => setToDelete(tag)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            )}
          </div>
        ))}
      </div>

      <ConfirmDialog
        open={!!toDelete}
        onOpenChange={(open) => !open && setToDelete(null)}
        title="Excluir marcador?"
        description="O marcador sai de todos os agendamentos e retornos que o usam."
        confirmLabel="Excluir"
        variant="destructive"
        onConfirm={() => {
          if (toDelete) deleteTag.mutate(toDelete.id);
          setToDelete(null);
        }}
      />
    </div>
  );
}
