"use client";

import { Building2, Check, Loader2 } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useMyCompanies, useSwitchCompany } from "@/features/auth/hooks/useAuthMutations";
import { DropdownMenuLabel, DropdownMenuSeparator } from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";

/**
 * Company Switcher (Sprint 8.4). Renderizado no UserMenu/Header: exibe a empresa
 * ativa (nome + logo quando disponível) e as demais empresas do usuário com
 * membership ativa. Ao selecionar outra empresa, alterna sem logout/login — o
 * backend valida a membership ativa, invalida `/me` e o contexto da aplicação.
 */
export function CompanySwitcher() {
  const { user } = useAuth();
  const { data: companies = [], isLoading } = useMyCompanies(!!user?.companyId);
  const switchCompany = useSwitchCompany();

  const activeId = user?.companyId ?? null;
  if (!activeId || (!isLoading && companies.length === 0)) {
    return null;
  }

  return (
    <>
      <DropdownMenuSeparator />
      <DropdownMenuLabel className="font-normal text-muted-foreground">Empresas</DropdownMenuLabel>
      <div className="space-y-0.5 px-2 py-1">
        {isLoading ? (
          <div className="flex items-center gap-2 px-2 py-1.5 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando…
          </div>
        ) : (
          companies.map((company) => {
            const isActive = company.companyId === activeId;
            return (
              <button
                key={company.companyId}
                type="button"
                disabled={isActive || switchCompany.isPending}
                data-testid={`company-option-${company.companyId}`}
                onClick={() => switchCompany.mutate(company.companyId)}
                className={cn(
                  "flex w-full items-center justify-between gap-2 rounded-sm px-2 py-1.5 text-sm outline-none transition-colors",
                  "hover:bg-accent hover:text-accent-foreground",
                  "disabled:pointer-events-none disabled:opacity-60",
                )}
                title={company.name}
              >
                <span className="flex min-w-0 items-center gap-2">
                  {company.logo ? (
                    <img
                      src={company.logo}
                      alt=""
                      className="h-4 w-4 shrink-0 rounded-sm object-contain"
                    />
                  ) : (
                    <Building2 className="h-4 w-4 shrink-0" />
                  )}
                  <span className="truncate font-medium">{company.name}</span>
                </span>
                {isActive ? (
                  <Check className="h-4 w-4 shrink-0" data-testid="active-company-check" />
                ) : (
                  switchCompany.isPending && <Loader2 className="h-4 w-4 shrink-0 animate-spin" />
                )}
              </button>
            );
          })
        )}
      </div>
    </>
  );
}
