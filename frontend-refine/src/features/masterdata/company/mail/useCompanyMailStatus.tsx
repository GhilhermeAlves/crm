"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle } from "lucide-react";
import api from "@/lib/api";
import { ROUTES } from "@/lib/constants";

/** Se a empresa já cadastrou a conta de e-mail de onde saem convites e formulários. */
export function useCompanyMailStatus(companyId: string | null | undefined) {
  return useQuery({
    queryKey: ["company-mail-status", companyId],
    queryFn: async () =>
      (await api.get<{ configured: boolean }>(`/companies/${companyId}/mail-settings/status`)).data
        .configured,
    enabled: Boolean(companyId),
  });
}

/** Aviso para cadastrar o e-mail da empresa antes de enviar por e-mail. */
export function MailNotConfiguredNotice() {
  return (
    <div
      role="alert"
      className="flex items-start gap-2 rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-100"
    >
      <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
      <p>
        Cadastre o e-mail da empresa antes de enviar convites por e-mail.{" "}
        <Link href={ROUTES.SETTINGS_EMAIL} className="font-medium underline">
          Ir para Configurações &gt; E-mail
        </Link>
        . Ou desmarque o envio por e-mail para gerar só o link.
      </p>
    </div>
  );
}
