"use client";

import { useRef, useState } from "react";
import { format, parseISO } from "date-fns";
import { Download, Loader2, Paperclip, Trash2, Upload } from "lucide-react";
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
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import { useDeleteFile, useDownloadFile } from "@/features/masterdata/storage/hooks/useStorage";
import { formatBytes, type StorageObject } from "@/features/masterdata/storage/types/storage.types";
import { useContactFiles, useUploadContactFile } from "../hooks/usePatient";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

export function PatientFiles({ contact }: { contact: Contact }) {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const contactId = contact.id;
  const can = usePermission();
  const canUpdate = can("contact:update");

  const { data: files = [], isLoading } = useContactFiles(companyId, contactId);
  const upload = useUploadContactFile(companyId, contactId);
  const download = useDownloadFile(companyId);
  const remove = useDeleteFile(companyId);

  const inputRef = useRef<HTMLInputElement>(null);
  const [pendingDelete, setPendingDelete] = useState<StorageObject | null>(null);

  const handleFilesSelected = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(event.target.files ?? []);
    selected.forEach((file) => upload.mutate(file));
    event.target.value = "";
  };

  const handleConfirmDelete = () => {
    if (pendingDelete) remove.mutate(pendingDelete.id);
    setPendingDelete(null);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">Arquivos</h1>
        {canUpdate && (
          <>
            <input
              ref={inputRef}
              type="file"
              multiple
              className="hidden"
              onChange={handleFilesSelected}
            />
            <Button
              size="sm"
              className="gap-1.5"
              disabled={upload.isPending}
              onClick={() => inputRef.current?.click()}
            >
              {upload.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Upload className="h-4 w-4" />
              )}
              Enviar arquivo
            </Button>
          </>
        )}
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" /> Carregando arquivos…
        </div>
      ) : files.length === 0 ? (
        <div className="flex flex-col items-center gap-2 py-16 text-center text-muted-foreground">
          <Paperclip className="h-8 w-8 opacity-50" />
          <p className="text-sm">Nenhum arquivo enviado para este paciente.</p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50 text-left text-xs uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="px-4 py-2 font-medium">Nome</th>
                <th className="px-4 py-2 font-medium">Tipo</th>
                <th className="px-4 py-2 font-medium">Tamanho</th>
                <th className="px-4 py-2 font-medium">Enviado em</th>
                <th className="px-4 py-2 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {files.map((file) => (
                <tr key={file.id}>
                  <td className="max-w-[240px] truncate px-4 py-2 font-medium">
                    <button
                      type="button"
                      className="max-w-full truncate text-left text-primary underline-offset-4 hover:underline disabled:opacity-60"
                      title={`Baixar ${file.fileName}`}
                      disabled={download.isPending}
                      onClick={() => download.mutate(file)}
                    >
                      {file.fileName}
                    </button>
                  </td>
                  <td className="px-4 py-2 text-muted-foreground">{file.contentType}</td>
                  <td className="px-4 py-2 tabular-nums text-muted-foreground">
                    {formatBytes(file.sizeBytes)}
                  </td>
                  <td className="px-4 py-2 tabular-nums text-muted-foreground">
                    {format(parseISO(file.createdAt), "dd/MM/yyyy")}
                  </td>
                  <td className="px-4 py-2">
                    <div className="flex justify-end gap-1">
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8"
                        aria-label={`Baixar ${file.fileName}`}
                        onClick={() => download.mutate(file)}
                      >
                        <Download className="h-4 w-4" />
                      </Button>
                      {canUpdate && (
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-destructive hover:text-destructive"
                          aria-label={`Excluir ${file.fileName}`}
                          onClick={() => setPendingDelete(file)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <AlertDialog open={!!pendingDelete} onOpenChange={(open) => !open && setPendingDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir arquivo?</AlertDialogTitle>
            <AlertDialogDescription>
              O arquivo “{pendingDelete?.fileName}” será removido definitivamente. Esta ação não
              pode ser desfeita.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={handleConfirmDelete}
            >
              Excluir
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
