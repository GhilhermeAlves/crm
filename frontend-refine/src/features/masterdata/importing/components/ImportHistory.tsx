"use client";

import { Undo2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import { useImports, useUndoImport } from "../useImports";
import { errorMessage } from "./ImportPanel";

const KIND_LABEL = { PATIENTS: "Pacientes", PROCEDURES: "Procedimentos" } as const;

export function ImportHistory({ companyId }: { companyId: string }) {
  const { data: history = [] } = useImports(companyId);
  const undoMut = useUndoImport(companyId);

  function onUndo(id: string) {
    undoMut.mutate(id, {
      onSuccess: (r) =>
        toast.success(
          `${r.removed} registro(s) removido(s)` +
            (r.kept ? `; ${r.kept} mantido(s) por já terem agenda ou procedimentos` : ""),
        ),
      onError: (e) => toast.error(errorMessage(e)),
    });
  }

  if (history.length === 0) return null;

  return (
    <section className="space-y-2">
      <h2 className="font-medium">Importações anteriores</h2>
      <p className="text-sm text-muted-foreground">
        Para desfazer pacientes, desfaça antes os procedimentos importados para eles.
      </p>
      <ul className="divide-y rounded-md border text-sm">
        {history.map((h) => (
          <li key={h.id} className="flex items-center justify-between gap-2 p-2">
            <span>
              {KIND_LABEL[h.kind] ?? h.kind} · {new Date(h.createdAt).toLocaleString("pt-BR")} —{" "}
              {h.sourceFile ?? "arquivo"}: {h.imported} importado(s), {h.skipped} pulado(s),{" "}
              {h.failed} erro(s)
              {h.undoneAt && " · desfeita"}
            </span>
            {!h.undoneAt && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onUndo(h.id)}
                disabled={undoMut.isPending}
              >
                <Undo2 className="mr-1 h-4 w-4" /> Desfazer
              </Button>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
