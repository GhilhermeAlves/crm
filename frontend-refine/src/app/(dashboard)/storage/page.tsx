"use client";

import { useRef, useState, useMemo } from "react";
import {
  Download,
  Eye,
  FileText,
  FolderOpen,
  Loader2,
  MoreVertical,
  Plus,
  Search,
  SearchX,
  Send,
  Trash2,
  Upload,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonTable } from "@/components/feedback/SkeletonTable";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useStorageObjects,
  useUploadFile,
  useDownloadFile,
  useDeleteFile,
} from "@/features/masterdata/storage/hooks/useStorage";
import {
  formatBytes,
  type StorageObject,
} from "@/features/masterdata/storage/types/storage.types";

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export default function StoragePage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const {
    data: files,
    isLoading,
    error,
    refetch,
  } = useStorageObjects(companyId);
  const uploadFile = useUploadFile(companyId);
  const downloadFile = useDownloadFile(companyId);
  const deleteFile = useDeleteFile(companyId);

  const inputRef = useRef<HTMLInputElement>(null);
  const [toDelete, setToDelete] = useState<StorageObject | null>(null);
  const [search, setSearch] = useState("");

  const filteredFiles = useMemo(() => {
    const q = search.trim().toLowerCase();
    return (files ?? []).filter((f) => {
      if (!q) return true;
      return (
        f.fileName.toLowerCase().includes(q) ||
        f.contentType.toLowerCase().includes(q)
      );
    });
  }, [files, search]);

  const totalCount = files?.length ?? 0;

  const handleNewDocument = (type: string) => {
    toast.info(`Documento "${type}" será implementado em breve.`);
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Documentos</h1>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-2">
          <div className="relative max-w-lg flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar documento"
              className="pl-9"
            />
          </div>
        </div>

        <input
          ref={inputRef}
          type="file"
          className="hidden"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) uploadFile.mutate(file);
            e.target.value = "";
          }}
        />

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button disabled={!companyId || uploadFile.isPending}>
              {uploadFile.isPending ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <Plus className="mr-2 h-4 w-4" />
              )}
              Novo
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuItem onClick={() => inputRef.current?.click()}>
              <Upload className="mr-2 h-4 w-4" />
              Adicionar arquivos
            </DropdownMenuItem>
            <DropdownMenuSub>
              <DropdownMenuSubTrigger>
                <FileText className="mr-2 h-4 w-4" />
                Criar novo documento
              </DropdownMenuSubTrigger>
              <DropdownMenuSubContent className="w-48">
                <DropdownMenuItem
                  onClick={() => handleNewDocument("Customizável")}
                >
                  <FileText className="mr-2 h-4 w-4" />
                  Customizável
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => handleNewDocument("Receita")}>
                  <FileText className="mr-2 h-4 w-4" />
                  Receita
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={() => handleNewDocument("Atestado")}
                >
                  <FileText className="mr-2 h-4 w-4" />
                  Atestado
                </DropdownMenuItem>
              </DropdownMenuSubContent>
            </DropdownMenuSub>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <div className="border-t pt-4">
        {isLoading ? (
          <SkeletonTable rows={5} columns={5} />
        ) : error ? (
          <ErrorCard
            message="Não foi possível carregar os documentos."
            onRetry={() => refetch()}
          />
        ) : search.trim() && filteredFiles.length === 0 ? (
          <EmptyState
            icon={<SearchX className="h-8 w-8" />}
            title="Nenhum resultado"
            description="Não encontramos documentos para a pesquisa aplicada."
            action={
              <Button
                variant="outline"
                size="sm"
                onClick={() => setSearch("")}
              >
                Limpar busca
              </Button>
            }
          />
        ) : filteredFiles.length === 0 ? (
          <EmptyState
            icon={<FolderOpen className="h-8 w-8" />}
            title="Nenhum documento"
            description="Envie um arquivo ou crie um documento para começar."
          />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Data</TableHead>
                <TableHead>Nome</TableHead>
                <TableHead>Tipo</TableHead>
                <TableHead>Tamanho</TableHead>
                <TableHead className="text-right">Ações</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredFiles.map((file) => (
                <TableRow key={file.id}>
                  <TableCell className="text-muted-foreground">
                    {formatDate(file.createdAt)}
                  </TableCell>
                  <TableCell className="font-medium">{file.fileName}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {file.contentType}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {formatBytes(file.sizeBytes)}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <TooltipProvider delayDuration={300}>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="outline"
                              size="icon"
                              className="h-8 w-8"
                              onClick={() => downloadFile.mutate(file)}
                              disabled={downloadFile.isPending}
                            >
                              <Send className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Baixar</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>

                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="outline"
                            size="icon"
                            className="h-8 w-8"
                          >
                            <MoreVertical className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem
                            onClick={() => downloadFile.mutate(file)}
                          >
                            <Eye className="mr-2 h-4 w-4" />
                            Visualizar documento
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => downloadFile.mutate(file)}
                          >
                            <Download className="mr-2 h-4 w-4" />
                            Baixar
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            className="text-destructive focus:text-destructive"
                            onClick={() => setToDelete(file)}
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            Excluir
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <ConfirmDialog
        open={!!toDelete}
        onOpenChange={(open) => !open && setToDelete(null)}
        title="Excluir documento"
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
