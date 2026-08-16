import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { StorageService } from "../services/storage.service";
import type { StorageObject } from "../types/storage.types";

export function useStorageObjects(companyId: string | null) {
  return useQuery({
    queryKey: ["storage", companyId],
    queryFn: () => StorageService.list(companyId as string),
    enabled: !!companyId,
  });
}

export function useUploadFile(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) =>
      StorageService.upload(companyId as string, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["storage", companyId] });
      toast.success("Arquivo enviado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao enviar arquivo");
    },
  });
}

export function useDeleteFile(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (objectId: string) =>
      StorageService.remove(companyId as string, objectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["storage", companyId] });
      toast.success("Arquivo excluído");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir arquivo");
    },
  });
}

export function useDownloadFile(companyId: string | null) {
  return useMutation({
    mutationFn: async (obj: StorageObject) => {
      const blob = await StorageService.download(companyId as string, obj.id);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = obj.fileName;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao baixar arquivo");
    },
  });
}
