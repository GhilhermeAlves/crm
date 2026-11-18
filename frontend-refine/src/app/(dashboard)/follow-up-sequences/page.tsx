"use client";

import { useState } from "react";
import { Plus, Repeat } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { DataTable } from "@/components/ui/data-table";
import { Badge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonTable } from "@/components/feedback/SkeletonTable";
import { FollowUpSequenceDialog } from "@/features/communication/omnichannel/components/FollowUpSequenceDialog";
import {
  useActivateFollowUpSequence,
  useCreateFollowUpSequence,
  useDeactivateFollowUpSequence,
  useDeleteFollowUpSequence,
  useFollowUpSequences,
  useUpdateFollowUpSequence,
} from "@/features/communication/omnichannel/hooks/useFollowUpSequences";
import {
  FOLLOW_UP_SEQUENCE_STATUS_LABELS,
  type FollowUpSequence,
} from "@/features/communication/omnichannel/types/followup-sequence.types";

export default function FollowUpSequencesPage() {
  const { data: page, isLoading, error, refetch } = useFollowUpSequences();
  const createSequence = useCreateFollowUpSequence();
  const updateSequence = useUpdateFollowUpSequence();
  const deleteSequence = useDeleteFollowUpSequence();
  const activateSequence = useActivateFollowUpSequence();
  const deactivateSequence = useDeactivateFollowUpSequence();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<FollowUpSequence | null>(null);
  const [toDelete, setToDelete] = useState<FollowUpSequence | null>(null);

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  const openEdit = (seq: FollowUpSequence) => {
    setEditing(seq);
    setDialogOpen(true);
  };

  const sequences = page?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Sequências de Follow-up</h1>
          <p className="text-sm text-muted-foreground">
            Gerencie grupos de follow-up reutilizáveis para automação de retorno.
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-1 h-4 w-4" /> Nova sequência
        </Button>
      </div>

      {isLoading ? (
        <SkeletonTable rows={5} columns={4} />
      ) : error ? (
        <ErrorCard message={error.message} onRetry={() => refetch()} />
      ) : (
        <Card>
          <CardContent className="p-0">
            {sequences.length > 0 ? (
              <DataTable
                tableId="follow-up-sequences"
                data={sequences ?? []}
                bordered={false}
                columns={[
                  {
                    id: "nome",
                    header: "Nome",
                    className: "font-medium",
                    sortValue: (seq) => seq.name,
                    cell: (seq) => <>{seq.name}</>,
                  },
                  {
                    id: "descricao",
                    header: "Descrição",
                    className: "max-w-[30ch] truncate text-muted-foreground",
                    sortValue: (seq) => seq.description,
                    cell: (seq) => <>{seq.description ?? "—"}</>,
                  },
                  {
                    id: "status",
                    header: "Status",
                    cell: (seq) => (
                      <Badge variant={seq.status === "ACTIVE" ? "default" : "secondary"}>
                        {FOLLOW_UP_SEQUENCE_STATUS_LABELS[seq.status]}
                      </Badge>
                    ),
                  },
                  {
                    id: "criado-em",
                    header: "Criado em",
                    className: "text-muted-foreground",
                    cell: (seq) => <>{new Date(seq.createdAt).toLocaleDateString("pt-BR")}</>,
                  },
                  {
                    id: "acoes",
                    header: "Ações",
                    hideable: false,
                    headerClassName: "text-right",
                    className: "text-right",
                    cell: (seq) => (
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={activateSequence.isPending || deactivateSequence.isPending}
                          onClick={() =>
                            seq.status === "ACTIVE"
                              ? deactivateSequence.mutate(seq.id)
                              : activateSequence.mutate(seq.id)
                          }
                        >
                          {seq.status === "ACTIVE" ? "Desativar" : "Ativar"}
                        </Button>
                        <Button variant="outline" size="sm" onClick={() => openEdit(seq)}>
                          Editar
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="text-destructive"
                          onClick={() => setToDelete(seq)}
                        >
                          Excluir
                        </Button>
                      </div>
                    ),
                  },
                ]}
                keyExtractor={(seq) => seq.id}
              />
            ) : (
              <EmptyState
                icon={<Repeat className="h-8 w-8" />}
                title="Nenhuma sequência"
                description="Crie uma sequência de follow-up para automatizar follow-ups recorrentes."
              />
            )}
          </CardContent>
        </Card>
      )}

      <FollowUpSequenceDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        initialName={editing?.name}
        initialDescription={editing?.description ?? undefined}
        isSubmitting={createSequence.isPending || updateSequence.isPending}
        onSubmit={(request) => {
          if (editing) {
            updateSequence.mutate(
              { id: editing.id, data: request },
              { onSuccess: () => setDialogOpen(false) },
            );
          } else {
            createSequence.mutate(request, {
              onSuccess: () => setDialogOpen(false),
            });
          }
        }}
      />

      <ConfirmDialog
        open={!!toDelete}
        onOpenChange={(open) => !open && setToDelete(null)}
        title="Excluir sequência"
        description={`Excluir a sequência "${toDelete?.name}"? Follow-ups vinculados perderão a associação.`}
        confirmLabel="Excluir"
        variant="destructive"
        isLoading={deleteSequence.isPending}
        onConfirm={() => {
          if (toDelete) deleteSequence.mutate(toDelete.id);
          setToDelete(null);
        }}
      />
    </div>
  );
}
