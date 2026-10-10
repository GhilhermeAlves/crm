"use client";

import { useState } from "react";
import Link from "next/link";
import { FileText, Pencil, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonTable } from "@/components/feedback/SkeletonTable";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { AnamnesisModelDialog } from "@/features/masterdata/anamnesis/components/AnamnesisModelDialog";
import {
  useAnamnesisModels,
  useCreateAnamnesisModel,
  useDeleteAnamnesisModel,
  useSetAnamnesisModelActive,
} from "@/features/masterdata/anamnesis/hooks/useAnamnesis";
import { ROUTES } from "@/lib/constants";

export default function AnamnesisListPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { can } = useAuthorization();
  const canManage = can("anamnesis:manage");

  const { data, isLoading, error, refetch } = useAnamnesisModels(companyId);
  const createModel = useCreateAnamnesisModel(companyId);
  const setActive = useSetAnamnesisModelActive(companyId);
  const deleteModel = useDeleteAnamnesisModel(companyId);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<{ id: string; name: string } | null>(null);

  const models = data ?? [];

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold">Modelos de anamnese</h2>
          <p className="text-sm text-muted-foreground">
            A empresa já começa com a Anamnese Odontológica Padrão, editável. Crie modelos
            adicionais quando precisar.
          </p>
        </div>
        {canManage && (
          <Button size="sm" onClick={() => setDialogOpen(true)}>
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            Novo modelo
          </Button>
        )}
      </div>

      {isLoading ? (
        <SkeletonTable rows={4} columns={4} />
      ) : error ? (
        <ErrorCard message={error.message} onRetry={() => refetch()} />
      ) : (
        <Card>
          <CardContent className="p-0">
            {models.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="text-xs">Nome do modelo</TableHead>
                    <TableHead className="text-xs">Estrutura</TableHead>
                    <TableHead className="text-xs">Modelo ativo?</TableHead>
                    {canManage && <TableHead className="text-right text-xs">Ação</TableHead>}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {models.map((model) => (
                    <TableRow key={model.id}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium">{model.name}</span>
                          {model.isDefault && <Badge variant="secondary">Padrão</Badge>}
                        </div>
                        {model.description && (
                          <p className="text-xs text-muted-foreground">{model.description}</p>
                        )}
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground">
                        {model.sectionCount} {model.sectionCount === 1 ? "seção" : "seções"} ·{" "}
                        {model.questionCount}{" "}
                        {model.questionCount === 1 ? "pergunta" : "perguntas"}
                      </TableCell>
                      <TableCell>
                        <Switch
                          checked={model.active}
                          disabled={!canManage || setActive.isPending}
                          onCheckedChange={(checked) =>
                            setActive.mutate({ modelId: model.id, active: checked })
                          }
                        />
                      </TableCell>
                      {canManage && (
                        <TableCell className="text-right">
                          <div className="inline-flex gap-1">
                            <Button variant="ghost" size="icon" className="h-7 w-7" asChild>
                              <Link
                                href={`${ROUTES.SETTINGS_DOCUMENTS_ANAMNESIS}/${model.id}`}
                                aria-label={`Editar ${model.name}`}
                              >
                                <Pencil className="h-3.5 w-3.5" />
                              </Link>
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7 text-destructive hover:text-destructive"
                              disabled={model.isDefault || deleteModel.isPending}
                              onClick={() => {
                                if (model.isDefault) {
                                  toast.error("O modelo padrão não pode ser excluído.");
                                  return;
                                }
                                setPendingDelete({ id: model.id, name: model.name });
                              }}
                              aria-label={`Excluir ${model.name}`}
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                          </div>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <EmptyState
                icon={<FileText className="h-8 w-8" />}
                title="Nenhum modelo cadastrado"
                description="Crie um modelo de anamnese para padronizar o atendimento da clínica."
              />
            )}
          </CardContent>
        </Card>
      )}

      <AnamnesisModelDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        isSubmitting={createModel.isPending}
        onSubmit={(request) =>
          createModel.mutate(request, { onSuccess: () => setDialogOpen(false) })
        }
      />

      <AlertDialog open={!!pendingDelete} onOpenChange={(open) => !open && setPendingDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir modelo</AlertDialogTitle>
            <AlertDialogDescription>
              Tem certeza que deseja excluir “{pendingDelete?.name}”? Esta ação não pode ser
              desfeita.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (pendingDelete) deleteModel.mutate(pendingDelete.id);
                setPendingDelete(null);
              }}
            >
              Excluir
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
