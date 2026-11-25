"use client";

import { useState } from "react";
import Link from "next/link";
import { CopyCheck, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { ROUTES } from "@/lib/constants";
import { SectionCard } from "@/features/masterdata/people/components/FormBits";
import {
  useDuplicates,
  usePeopleMutation,
  usePeoplePermissions,
} from "@/features/masterdata/people/hooks/usePeople";
import { PeopleService } from "@/features/masterdata/people/services/people.service";
import {
  formatDate,
  formatDateTime,
  formatPhone,
  formatTaxId,
} from "@/features/masterdata/people/lib/people-labels";
import type { DuplicateGroup, MergeResult } from "@/features/masterdata/people/types/people.types";

/** Pessoas → Duplicados: pendências da migração e possíveis duplicados (doc 22.8). */
export default function DuplicatesPage() {
  const { canMerge } = usePeoplePermissions();
  const { data, isLoading, isError, refetch } = useDuplicates(canMerge);
  const [group, setGroup] = useState<DuplicateGroup | null>(null);
  const [keepId, setKeepId] = useState<string>("");
  const [reason, setReason] = useState("");
  const [result, setResult] = useState<MergeResult | null>(null);
  const merge = usePeopleMutation<
    { keepId: string; removeId: string; reason: string },
    MergeResult
  >((c, v) => PeopleService.merge(c, v), "Cadastros unificados");
  const dismiss = usePeopleMutation<string>(
    (c, id) => PeopleService.dismissIssue(c, id),
    "Pendência dispensada",
  );

  if (!canMerge) {
    return <ErrorCard message="Você não tem permissão para unificar cadastros." />;
  }

  const removeId = group?.members.find((m) => m.id !== keepId)?.id ?? "";

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Duplicados</h1>
      <p className="text-sm text-muted-foreground">
        Unificar move agenda, prontuário, orçamentos, financeiro e conversas para o cadastro que
        fica e exclui o outro (exclusão lógica, com histórico).
      </p>
      {isLoading ? (
        <div className="flex items-center gap-2 text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" /> Procurando…
        </div>
      ) : isError ? (
        <ErrorCard message="Não foi possível carregar os duplicados." onRetry={() => refetch()} />
      ) : (data ?? []).length === 0 ? (
        <EmptyState
          icon={<CopyCheck className="h-8 w-8" />}
          title="Nenhum duplicado"
          description="Não há pendências nem cadastros parecidos."
        />
      ) : (
        <div className="space-y-4">
          {(data ?? []).map((g, i) => (
            <SectionCard
              key={`${g.reason}-${g.key}-${i}`}
              title={g.reason}
              description={g.key ?? undefined}
              actions={
                <>
                  {g.members.length >= 2 && (
                    <Button
                      size="sm"
                      onClick={() => {
                        setGroup(g);
                        setKeepId(g.members[0].id);
                        setReason("");
                        setResult(null);
                      }}
                    >
                      Unificar
                    </Button>
                  )}
                  {g.members.find((m) => m.issueId) && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => dismiss.mutate(g.members.find((m) => m.issueId)!.issueId!)}
                    >
                      Dispensar pendência
                    </Button>
                  )}
                </>
              }
            >
              <ul className="grid gap-2 sm:grid-cols-2">
                {g.members.map((m) => (
                  <li key={m.id} className="rounded-md border p-3 text-sm">
                    <Link
                      className="font-semibold text-primary hover:underline"
                      href={`${ROUTES.PEOPLE}/${m.id}`}
                    >
                      {m.name}
                    </Link>
                    <div className="text-muted-foreground">
                      {formatTaxId(m.taxId)} · nasc. {formatDate(m.birthDate)} ·{" "}
                      {formatPhone(m.mobile)}
                    </div>
                    <div className="text-xs text-muted-foreground">
                      Cadastrado em {formatDateTime(m.createdAt)}
                    </div>
                  </li>
                ))}
              </ul>
            </SectionCard>
          ))}
        </div>
      )}

      <Dialog open={!!group} onOpenChange={(o) => !o && setGroup(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Unificar cadastros</DialogTitle>
          </DialogHeader>
          {result ? (
            <div className="space-y-2 text-sm">
              <p>Cadastros unificados.</p>
              {result.movedTables.length > 0 && <p>Movidos: {result.movedTables.join(", ")}</p>}
              {result.skippedTables.length > 0 && (
                <p className="text-amber-700">
                  Não movidos (conflito, revise à mão): {result.skippedTables.join(", ")}
                </p>
              )}
            </div>
          ) : (
            group && (
              <div className="space-y-4">
                <fieldset className="space-y-2 text-sm">
                  <legend className="mb-1 font-medium">Qual cadastro fica?</legend>
                  {group.members.slice(0, 2).map((m) => (
                    <label key={m.id} className="flex items-center gap-2">
                      <input
                        type="radio"
                        checked={keepId === m.id}
                        onChange={() => setKeepId(m.id)}
                      />
                      {m.name} ({formatTaxId(m.taxId)}, desde {formatDate(m.createdAt)})
                    </label>
                  ))}
                </fieldset>
                <label htmlFor="merge-reason" className="text-sm font-medium">
                  Motivo<span className="text-destructive">*</span>
                </label>
                <Input
                  id="merge-reason"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
              </div>
            )
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setGroup(null)}>
              {result ? "Fechar" : "Cancelar"}
            </Button>
            {!result && (
              <Button
                disabled={!reason.trim() || !removeId || merge.isPending}
                onClick={() =>
                  merge.mutate(
                    { keepId, removeId, reason: reason.trim() },
                    { onSuccess: setResult },
                  )
                }
              >
                {merge.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Unificar
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
