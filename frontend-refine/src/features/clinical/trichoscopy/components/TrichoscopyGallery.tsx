"use client";

import { useState } from "react";
import { format, parseISO } from "date-fns";
import { Loader2, Trash2 } from "lucide-react";
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
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useDeleteTrichoscopy, useTrichoscopyList } from "../hooks/useTrichoscopy";
import type { TrichoscopyImageResponse } from "../types/trichoscopy.types";

type Props = {
  companyId: string;
  contactId: string;
  canEdit: boolean;
};

/** A URL de download é relativa à origem: o cookie de sessão autentica o <img>. */
export function TrichoscopyGallery({ companyId, contactId, canEdit }: Props) {
  const { data: images = [], isLoading } = useTrichoscopyList(companyId, contactId);
  const remove = useDeleteTrichoscopy(companyId, contactId);
  const [toDelete, setToDelete] = useState<TrichoscopyImageResponse | null>(null);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Exames anteriores</CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" /> Carregando…
          </div>
        ) : images.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nenhuma imagem registrada.</p>
        ) : (
          <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-4">
            {images.map((image) => (
              <figure
                key={image.id}
                className="group relative overflow-hidden rounded-lg border bg-muted/20"
              >
                <a href={image.downloadUrl} target="_blank" rel="noreferrer" title="Ampliar imagem">
                  {/* eslint-disable-next-line @next/next/no-img-element -- arquivo do storage autenticado por cookie */}
                  <img
                    src={image.downloadUrl}
                    alt={`Imagem tricoscópica de ${format(parseISO(image.takenAt), "dd/MM/yyyy")}`}
                    className="aspect-square w-full object-cover transition-transform group-hover:scale-[1.02]"
                  />
                </a>
                <figcaption className="space-y-0.5 px-2 py-1.5">
                  <p className="text-xs text-muted-foreground">
                    {format(parseISO(image.takenAt), "dd/MM/yyyy HH:mm")}
                  </p>
                  {image.notes && (
                    <p className="line-clamp-2 text-xs" title={image.notes}>
                      {image.notes}
                    </p>
                  )}
                </figcaption>
                {canEdit && (
                  <Button
                    size="icon"
                    variant="secondary"
                    className="absolute right-1.5 top-1.5 h-7 w-7 opacity-0 transition-opacity focus-visible:opacity-100 group-hover:opacity-100"
                    aria-label="Excluir imagem"
                    onClick={() => setToDelete(image)}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                )}
              </figure>
            ))}
          </div>
        )}
      </CardContent>

      <AlertDialog open={toDelete != null} onOpenChange={(open) => !open && setToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir imagem?</AlertDialogTitle>
            <AlertDialogDescription>
              A imagem e o arquivo serão removidos. Esta ação não pode ser desfeita.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (toDelete) remove.mutate(toDelete.id);
                setToDelete(null);
              }}
            >
              Excluir
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
