"use client";

import { CheckCircle2, Loader2, PenLine, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { useAiCancelAction, useAiConfirmAction } from "../hooks/useAi";
import type { AiAction } from "../types/ai.types";

type Props = {
  action: AiAction;
};

/**
 * Cartao de proposta de acao de escrita do Assistente de IA (AI-05). Para uma
 * proposta {@code PROPOSED} oferece Confirmar/Cancelar; estados terminais sao
 * exibidos de forma informativa. Os botoes ficam desabilitados enquanto uma
 * acao esta em andamento (prevencao de duplo clique); o estado e sempre o do
 * servidor (refetch via query).
 */
export function AiActionProposalCard({ action }: Props) {
  const confirmMutation = useAiConfirmAction(action.conversationId);
  const cancelMutation = useAiCancelAction(action.conversationId);
  const pending = confirmMutation.isPending || cancelMutation.isPending;

  const status = action.status;

  if (status === "EXECUTED") {
    return (
      <Card className="mx-1 my-1 border-emerald-600/30 bg-emerald-500/5">
        <CardContent className="flex items-center gap-2 py-3 text-sm text-emerald-700">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <div className="min-w-0">
            <p className="font-medium">Ação executada</p>
            <p className="whitespace-pre-wrap break-words text-emerald-700/80">
              {action.description ?? "Ação concluída."}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (status === "FAILED") {
    return (
      <Card className="mx-1 my-1 border-destructive/30 bg-destructive/5">
        <CardContent className="flex items-center gap-2 py-3 text-sm text-destructive">
          <XCircle className="h-4 w-4 shrink-0" />
          <div className="min-w-0">
            <p className="font-medium">Falha ao executar a ação</p>
            <p className="whitespace-pre-wrap break-words text-destructive/80">
              {action.errorMessage ?? action.description}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (status === "CANCELLED") {
    return (
      <Card className="mx-1 my-1 border-border bg-muted/40">
        <CardContent className="flex items-center gap-2 py-3 text-sm text-muted-foreground">
          <PenLine className="h-4 w-4 shrink-0" />
          <div className="min-w-0">
            <p className="font-medium">Ação cancelada</p>
            <p className="whitespace-pre-wrap break-words text-muted-foreground/80">
              {action.description ?? ""}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (status === "CONFIRMED" || status === "EXECUTING") {
    return (
      <Card className="mx-1 my-1 border-border">
        <CardContent className="flex items-center gap-2 py-3 text-sm">
          <Loader2 className="h-4 w-4 shrink-0 animate-spin text-primary" />
          <div className="min-w-0">
            <p className="font-medium">Executando ação...</p>
            <p className="whitespace-pre-wrap break-words text-muted-foreground">
              {action.description ?? ""}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={cn("mx-1 my-1 border-primary/30 bg-primary/5")}>
      <CardContent className="flex items-start gap-2 py-3 text-sm">
        <PenLine className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
        <div className="min-w-0">
          <p className="font-medium">O assistente deseja executar esta ação</p>
          <p className="whitespace-pre-wrap break-words text-muted-foreground">
            {action.description ?? ""}
          </p>
        </div>
      </CardContent>
      <CardFooter className="gap-2 pt-0">
        <Button
          size="sm"
          onClick={() => confirmMutation.mutate(action.id)}
          disabled={pending}
          aria-label="Confirmar ação"
        >
          {confirmMutation.isPending ? (
            <Loader2 className="mr-1 h-4 w-4 animate-spin" />
          ) : (
            <CheckCircle2 className="mr-1 h-4 w-4" />
          )}
          Confirmar
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => cancelMutation.mutate(action.id)}
          disabled={pending}
          aria-label="Cancelar ação"
        >
          Cancelar
        </Button>
      </CardFooter>
    </Card>
  );
}
