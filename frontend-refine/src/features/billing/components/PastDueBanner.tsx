"use client";

import { AlertTriangle } from "lucide-react";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useMyBilling } from "../hooks";
import { formatLocalDate } from "../format";

/** Aviso de mensalidade vencida (uso normal). Só quem administra a clínica vê o botão de Pix. */
export function PastDueBanner() {
  const { can, isCrmAdmin } = useAuthorization();
  const { data } = useMyBilling(!isCrmAdmin);

  if (isCrmAdmin || !data || data.status !== "PAST_DUE") return null;

  return (
    <div
      role="alert"
      className="flex flex-wrap items-center gap-3 border-b border-amber-300 bg-amber-50 px-4 py-2 text-sm text-amber-900 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-100"
    >
      <AlertTriangle className="h-4 w-4 shrink-0" />
      <p className="flex-1">
        Mensalidade venceu em {formatLocalDate(data.overdueSince, "short")}.
        {data.graceUntil && <> Liberada até {formatLocalDate(data.graceUntil, "short")}.</>}
      </p>
      {can("company:update") ? (
        data.openInvoiceUrl && (
          <a
            href={data.openInvoiceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-md bg-amber-600 px-3 py-1 font-medium text-white hover:bg-amber-700"
          >
            Pagar com Pix
          </a>
        )
      ) : (
        <span>Avise o administrador da clínica.</span>
      )}
    </div>
  );
}
