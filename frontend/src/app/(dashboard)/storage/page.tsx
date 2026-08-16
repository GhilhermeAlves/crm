"use client";

import { useRef, useState } from "react";
import { Download, Loader2, Trash2, Upload } from "lucide-react";
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
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { useAuth } from "@/features/auth/hooks/useAuth";
import {
  useStorageObjects,
  useUploadFile,
  useDownloadFile,
  useDeleteFile,
} from "@/features/storage/hooks/useStorage";
import {
  formatBytes,
  type StorageObject,
} from "@/features/storage/types/storage.types";

export default function StoragePage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const { data: files, isLoading } = useStorageObjects(companyId);
  const uploadFile = useUploadFile(companyId);
  const downloadFile = useDownloadFile(companyId);
  const deleteFile = useDeleteFile(companyId);

  const inputRef = useRef<HTMLInputElement>(null);
  const [selected, setSelected] = useState<File | null>(null);
  const [toDelete, setToDelete] = useState<StorageObject | null>(null);

  const handlePick = (file: File | undefined) => {
    if (!file) return;
    setSelected(file);
    uploadFile.mutate(file);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Arquivos</h1>
          <p className="text-sm text-muted-foreground">
            Armazenamento da sua empresa (escopado por tenant). Envie e baixe
            arquivos.
          </p>
        </div>
        <div>
          <input
            ref={inputRef}
            type="file"
            className="hidden"
            onChange={(e) => handlePick(e.target.files?.[0])}
          />
          <Button
            onClick={() => inputRef.current?.click()}
            disabled={!companyId || uploadFile.isPending}
          >
            {uploadFile.isPending ? (
              <Loader2 className="mr-1 h-4 w-4 animate-spin" />
            ) : (
              <Upload className="mr-1 h-4 w-4" />
            )}
            Enviar arquivo
          </Button>
        </div>
      </div>

      {selected && (
        <Card>
          <CardContent className="py-3 text-sm text-muted-foreground">
            Enviando{" "}
            <span className="font-medium text-foreground">{selected.name}</span>
            …
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex items-center justify-center gap-2 py-16 text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" /> Carregando…
            </div>
          ) : files && files.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Tamanho</TableHead>
                  <TableHead className="text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {files.map((file) => (
                  <TableRow key={file.id}>
                    <TableCell className="font-medium">
                      {file.fileName}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {file.contentType}
                    </TableCell>
                    <TableCell>{formatBytes(file.sizeBytes)}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => downloadFile.mutate(file)}
                          disabled={downloadFile.isPending}
                        >
                          <Download className="mr-1 h-4 w-4" /> Baixar
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="text-destructive"
                          onClick={() => setToDelete(file)}
                        >
                          <Trash2 className="mr-1 h-4 w-4" /> Excluir
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <EmptyState
              title="Nenhum arquivo"
              description="Envie um arquivo para começar a usar o armazenamento."
            />
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={!!toDelete}
        onOpenChange={(open) => !open && setToDelete(null)}
        title="Excluir arquivo"
        description={`Excluir "${toDelete?.fileName}"? Essa ação não pode ser desfeita.`}
        confirmLabel="Excluir"
        variant="destructive"
        isLoading={deleteFile.isPending}
        onConfirm={() => {
          if (toDelete) deleteFile.mutate(toDelete.id);
          setToDelete(null);
        }}
      />
    </div>
  );
}
