"use client";

import { useState } from "react";
import { Loader2, Plus } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { FollowUpSequenceDialog } from "@/features/omnichannel/components/FollowUpSequenceDialog";
import {
  useActivateFollowUpSequence,
  useCreateFollowUpSequence,
  useDeactivateFollowUpSequence,
  useDeleteFollowUpSequence,
  useFollowUpSequences,
  useUpdateFollowUpSequence,
} from "@/features/omnichannel/hooks/useFollowUpSequences";
import {
  FOLLOW_UP_SEQUENCE_STATUS_LABELS,
  type FollowUpSequence,
} from "@/features/omnichannel/types/followup-sequence.types";

export default function FollowUpSequencesPage() {
  const { data: page, isLoading } = useFollowUpSequences();
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
          <h1 className="text-2xl font-bold tracking-tight">
            Sequências de Follow-up
          </h1>
          <p className="text-sm text-muted-foreground">
            Gerencie grupos de follow-up reutilizáveis para automação de retorno.
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-1 h-4 w-4" /> Nova sequência
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex items-center justify-center gap-2 py-16 text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" /> Carregando…
            </div>
          ) : sequences.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Descrição</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Criado em</TableHead>
                  <TableHead className="text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {sequences.map((seq) => (
                  <TableRow key={seq.id}>
                    <TableCell className="font-medium">{seq.name}</TableCell>
                    <TableCell className="max-w-[30ch] truncate text-muted-foreground">
                      {seq.description ?? "—"}
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          seq.status === "ACTIVE" ? "default" : "secondary"
                        }
                      >
                        {FOLLOW_UP_SEQUENCE_STATUS_LABELS[seq.status]}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {new Date(seq.createdAt).toLocaleDateString("pt-BR")}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={
                            activateSequence.isPending ||
                            deactivateSequence.isPending
                          }
                          onClick={() =>
                            seq.status === "ACTIVE"
                              ? deactivateSequence.mutate(seq.id)
                              : activateSequence.mutate(seq.id)
                          }
                        >
                          {seq.status === "ACTIVE" ? "Desativar" : "Ativar"}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openEdit(seq)}
                        >
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
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <EmptyState
              title="Nenhuma sequência"
              description="Crie uma sequência de follow-up para automatizar follow-ups recorrentes."
            />
          )}
        </CardContent>
      </Card>

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
