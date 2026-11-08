"use client";

import { useState } from "react";
import { Lock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useMyCompanies, useSwitchCompany } from "@/features/identity/auth/hooks/useAuthMutations";
import { useCheckPayment, useMyBilling } from "../hooks";
import { formatCents, formatLocalDate } from "../format";

/** Saída e troca de empresa: só usam /auth/** e /me/**, liberados mesmo com a empresa bloqueada. */
function BlockedActions() {
  const { user, logout } = useAuth();
  const { data: companies = [] } = useMyCompanies(!!user?.companyId);
  const switchCompany = useSwitchCompany();
  const others = companies.filter((company) => company.companyId !== user?.companyId);

  return (
    <div className="space-y-2">
      {companies.length > 1 && others.length > 0 && (
        <div className="space-y-1">
          <p className="text-xs text-muted-foreground">Trocar de empresa</p>
          {others.map((company) => (
            <Button
              key={company.companyId}
              variant="ghost"
              size="sm"
              className="w-full"
              disabled={switchCompany.isPending}
              onClick={() =>
                switchCompany.mutate(company.companyId, {
                  onSuccess: () => window.location.assign("/"),
                })
              }
            >
              {company.name}
            </Button>
          ))}
        </div>
      )}
      <Button variant="ghost" size="sm" onClick={logout}>
        Sair
      </Button>
    </div>
  );
}

export function PaymentPendingView() {
  const { data, isLoading, isError, refetch } = useMyBilling();

  const check = useCheckPayment();
  const [notFound, setNotFound] = useState(false);
  const [checkFailed, setCheckFailed] = useState(false);

  if (isError && !data) {
    return (
      <div className="space-y-3 text-center">
        <p className="text-sm text-muted-foreground">
          Não foi possível carregar os dados de cobrança.
        </p>
        <Button variant="outline" onClick={() => refetch()}>
          Tentar de novo
        </Button>
        <BlockedActions />
      </div>
    );
  }

  if (isLoading || !data) {
    return <p className="text-sm text-muted-foreground">Carregando…</p>;
  }

  if (data.status !== "BLOCKED") {
    return (
      <Card className="w-full max-w-md">
        <CardContent className="space-y-4 pt-6 text-center">
          <p>Sua empresa está liberada.</p>
          <Button onClick={() => window.location.assign("/")}>Voltar ao sistema</Button>
        </CardContent>
      </Card>
    );
  }

  const onCheck = () => {
    setNotFound(false);
    setCheckFailed(false);
    check.mutate(undefined, {
      onSuccess: (view) => {
        if (view.status !== "BLOCKED") window.location.assign("/");
        else setNotFound(true);
      },
      onError: () => setCheckFailed(true),
    });
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader className="items-center text-center">
        <Lock className="h-10 w-10 text-amber-600" />
        <CardTitle>Pagamento pendente</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 text-center">
        <p className="text-sm">
          A mensalidade de <strong>{formatCents(data.monthlyPriceCents)}</strong> está em atraso
          desde {formatLocalDate(data.overdueSince)}. Seus dados continuam guardados e voltam assim
          que o pagamento for confirmado.
        </p>
        <div className="flex flex-col gap-2">
          {data.openInvoiceUrl && (
            <Button asChild>
              <a href={data.openInvoiceUrl} target="_blank" rel="noopener noreferrer">
                Pagar com Pix
              </a>
            </Button>
          )}
          <Button variant="outline" onClick={onCheck} disabled={check.isPending}>
            {check.isPending ? "Verificando…" : "Já paguei"}
          </Button>
        </div>
        {notFound && (
          <p className="text-sm text-muted-foreground">
            Ainda não identificamos o pagamento. Pode levar alguns minutos depois do Pix.
          </p>
        )}
        {checkFailed && (
          <p className="text-sm text-muted-foreground">
            Não foi possível verificar agora. Tente de novo em instantes.
          </p>
        )}
        <p className="text-xs text-muted-foreground">Dúvidas? Fale com o suporte do CRM.</p>
        <BlockedActions />
      </CardContent>
    </Card>
  );
}
