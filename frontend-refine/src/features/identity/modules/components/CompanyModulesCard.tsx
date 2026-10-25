"use client";

import { useEffect, useMemo, useState } from "react";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useCompanyModules, useUpdateCompanyModules } from "../hooks/useCompanyModules";
import {
  MODULE_GROUPS,
  MODULE_OPTIONS,
  SEGMENT_DEFAULT_MODULES,
  SEGMENT_OPTIONS,
  type CompanyModuleName,
  type CompanySegment,
} from "../types/modules.types";

/** Segmento e módulos da empresa — exclusivo do SUPER_ADMIN (dono da plataforma). */
export function CompanyModulesCard({ companyId }: { companyId: string }) {
  const { data, isLoading, isError } = useCompanyModules(companyId);
  const update = useUpdateCompanyModules(companyId);
  const { roles } = useAuth();
  const isSuperAdmin = roles.includes("SUPER_ADMIN");
  const [segment, setSegment] = useState<CompanySegment>("GENERIC");
  const [modules, setModules] = useState<CompanyModuleName[]>([]);

  useEffect(() => {
    if (data) {
      setSegment(data.segment);
      setModules(data.enabledModules);
    }
  }, [data]);

  const toggle = (module: CompanyModuleName, checked: boolean) => {
    if (!isSuperAdmin) return;
    setModules((current) =>
      checked
        ? [...current.filter((m) => m !== module), module]
        : current.filter((m) => m !== module),
    );
  };

  const handleSegmentChange = (value: CompanySegment) => {
    if (!isSuperAdmin) return;
    setSegment(value);
    setModules(SEGMENT_DEFAULT_MODULES[value] ?? []);
  };

  const moduleByValue = useMemo(
    () => new Map(MODULE_OPTIONS.map((option) => [option.value, option])),
    [],
  );

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Segmento e módulos</CardTitle>
        <CardDescription>
          Define o que esta empresa enxerga no CRM. Pacientes funciona mesmo com o Comercial
          desligado.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        {isLoading ? (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" /> Carregando módulos…
          </div>
        ) : isError ? (
          <p className="text-sm text-destructive">Não foi possível carregar os módulos.</p>
        ) : (
          <>
            <div className="space-y-2">
              <Label htmlFor="company-segment">Segmento</Label>
              <Select
                value={segment}
                onValueChange={(v) => handleSegmentChange(v as CompanySegment)}
                disabled={!isSuperAdmin}
              >
                <SelectTrigger id="company-segment" className="max-w-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SEGMENT_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <fieldset className="space-y-4">
              <legend className="text-sm font-medium">Módulos habilitados</legend>
              {MODULE_GROUPS.map((group) => (
                <div key={group.title} className="space-y-3">
                  <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    {group.title}
                  </p>
                  {group.modules.map((moduleValue) => {
                    const option = moduleByValue.get(moduleValue);
                    if (!option) return null;
                    const checked = modules.includes(option.value);
                    return (
                      <div key={option.value} className="flex items-start gap-3">
                        <Checkbox
                          id={`module-${option.value}`}
                          checked={checked}
                          disabled={!isSuperAdmin}
                          onCheckedChange={(checkedState) =>
                            toggle(option.value, checkedState === true)
                          }
                        />
                        <div className="grid gap-0.5">
                          <Label htmlFor={`module-${option.value}`}>{option.label}</Label>
                          <p className="text-xs text-muted-foreground">{option.description}</p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ))}
            </fieldset>

            {isSuperAdmin && (
              <Button
                type="button"
                disabled={update.isPending}
                onClick={() => update.mutate({ segment, enabledModules: modules })}
              >
                {update.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Salvar módulos
              </Button>
            )}
            {!isSuperAdmin && (
              <p className="text-xs text-muted-foreground">
                Somente o SUPER_ADMIN pode alterar segmento e módulos.
              </p>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
