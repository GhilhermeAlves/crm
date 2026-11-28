"use client";

import { AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useProfileDivergences, useResolveDivergence } from "../hooks/useProfileDivergences";
import { PROFILE_FIELD_LABELS } from "../services/profile-divergence.service";

/** Dados respondidos na anamnese que diferem do cadastro: a recepção aceita ou ignora. */
export function ProfileDivergenceBanner({
  companyId,
  contactId,
}: {
  companyId: string;
  contactId: string;
}) {
  const { can } = useAuthorization();
  const { data } = useProfileDivergences(companyId, contactId);
  const resolve = useResolveDivergence(companyId, contactId);
  const divergences = data ?? [];
  if (divergences.length === 0) return null;
  const canEdit = can("contact:update");

  return (
    <section
      className="space-y-3 rounded-md border border-amber-300 bg-amber-50 p-4 text-amber-950 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-100"
      aria-label="Divergências entre anamnese e cadastro"
    >
      <p className="flex items-center gap-2 text-sm font-semibold">
        <AlertTriangle className="h-4 w-4" />
        Dados da anamnese divergentes do cadastro ({divergences.length})
      </p>
      <ul className="space-y-2">
        {divergences.map((d) => (
          <li key={d.id} className="flex flex-wrap items-center justify-between gap-2 text-sm">
            <div className="min-w-0">
              <span className="font-medium">{PROFILE_FIELD_LABELS[d.field]}:</span> no cadastro{" "}
              <strong>{d.currentValue || "—"}</strong> · na anamnese{" "}
              <strong>{d.proposedValue}</strong>
              {d.reason && <p className="text-xs opacity-80">{d.reason}</p>}
            </div>
            {canEdit && (
              <div className="flex gap-2">
                <Button
                  size="sm"
                  disabled={resolve.isPending}
                  onClick={() => resolve.mutate({ id: d.id, action: "accept" })}
                >
                  Aceitar
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={resolve.isPending}
                  onClick={() => resolve.mutate({ id: d.id, action: "dismiss" })}
                >
                  Ignorar
                </Button>
              </div>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
