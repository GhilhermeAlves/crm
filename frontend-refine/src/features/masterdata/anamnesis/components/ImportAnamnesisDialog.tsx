"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { FileUp, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ROUTES } from "@/lib/constants";
import { parseAnamnesisImport, type ImportedAnamnesis } from "../lib/importAnamnesis";
import { AnamnesisService } from "../services/anamnesis.service";
import { ANAMNESIS_QUESTION_TYPE_LABELS } from "../types/anamnesis.types";

interface Props {
  companyId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/**
 * Cria um modelo de anamnese a partir de um formulário HTML ou de um JSON.
 * Só a estrutura é importada (seções, perguntas, tipos e opções); scripts e
 * destinos de envio do HTML são ignorados. O editor abre em seguida para revisão.
 */
export function ImportAnamnesisDialog({ companyId, open, onOpenChange }: Props) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [content, setContent] = useState("");
  const [fileName, setFileName] = useState("");
  const [name, setName] = useState("");

  const parsed = useMemo<{ result?: ImportedAnamnesis; error?: string }>(() => {
    if (!content.trim()) return {};
    try {
      return { result: parseAnamnesisImport(content, fileName) };
    } catch (e) {
      return { error: e instanceof Error ? e.message : "Não foi possível ler o conteúdo." };
    }
  }, [content, fileName]);

  const importModel = useMutation({
    mutationFn: async (data: ImportedAnamnesis) => {
      const created = await AnamnesisService.create(companyId as string, {
        name: data.name,
        description: data.description,
      });
      await AnamnesisService.update(companyId as string, created.id, {
        name: data.name,
        description: data.description,
        sections: data.sections,
      });
      return created;
    },
    onSuccess: (created) => {
      void queryClient.invalidateQueries({ queryKey: ["anamnesis-models"] });
      toast.success("Modelo importado. Revise as perguntas antes de usar.");
      reset();
      onOpenChange(false);
      router.push(`${ROUTES.SETTINGS_DOCUMENTS_ANAMNESIS}/${created.id}`);
    },
    onError: () => toast.error("Erro ao importar o modelo."),
  });

  const reset = () => {
    setContent("");
    setFileName("");
    setName("");
  };

  const readFile = async (file: File | undefined) => {
    if (!file) return;
    setFileName(file.name);
    setContent(await file.text());
    setName("");
  };

  const result = parsed.result;
  const questionCount = result?.sections.reduce((n, s) => n + (s.questions?.length ?? 0), 0) ?? 0;

  return (
    <Dialog
      open={open}
      onOpenChange={(value) => {
        if (!value) reset();
        onOpenChange(value);
      }}
    >
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Importar anamnese (HTML ou JSON)</DialogTitle>
          <DialogDescription>
            Envie o arquivo do formulário ou cole o código. Campos viram perguntas; fieldsets e
            títulos (h1–h4) viram seções.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="anamnesis-import-file">Arquivo (.html ou .json)</Label>
            <Input
              id="anamnesis-import-file"
              type="file"
              accept=".html,.htm,.json,text/html,application/json"
              onChange={(e) => void readFile(e.target.files?.[0])}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="anamnesis-import-content">Ou cole o conteúdo</Label>
            <Textarea
              id="anamnesis-import-content"
              rows={8}
              className="font-mono text-xs"
              placeholder='<form>…</form>  ou  { "name": "...", "sections": [ ... ] }'
              value={content}
              onChange={(e) => {
                setContent(e.target.value);
                setFileName("");
              }}
            />
          </div>

          {parsed.error && <p className="text-sm text-destructive">{parsed.error}</p>}

          {result && (
            <div className="space-y-3 rounded-md border p-3">
              <div className="space-y-1.5">
                <Label htmlFor="anamnesis-import-name">Nome do modelo</Label>
                <Input
                  id="anamnesis-import-name"
                  value={name || result.name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
              <p className="text-sm text-muted-foreground">
                {result.sections.length} seção(ões) e {questionCount} pergunta(s) encontradas:
              </p>
              <ul className="max-h-48 space-y-2 overflow-y-auto text-sm">
                {result.sections.map((section, i) => (
                  <li key={`${section.title}-${i}`}>
                    <p className="font-medium">{section.title}</p>
                    <ul className="ml-4 list-disc text-muted-foreground">
                      {section.questions?.map((q, j) => (
                        <li key={j}>
                          {q.text}{" "}
                          <span className="text-xs">
                            ({ANAMNESIS_QUESTION_TYPE_LABELS[q.type]}
                            {q.options?.length
                              ? `: ${q.options.map((o) => o.label).join(", ")}`
                              : ""}
                            )
                          </span>
                        </li>
                      ))}
                    </ul>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button
            disabled={!result || importModel.isPending || !companyId}
            onClick={() =>
              result && importModel.mutate({ ...result, name: (name || result.name).trim() })
            }
            className="gap-1.5"
          >
            {importModel.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <FileUp className="h-4 w-4" />
            )}
            Importar e revisar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
