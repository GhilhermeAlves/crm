"use client";

import type { UseFormSetValue, UseFormWatch } from "react-hook-form";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  MODULE_GROUPS,
  MODULE_OPTIONS,
  SEGMENT_DEFAULT_MODULES,
  SEGMENT_OPTIONS,
  type CompanyModuleName,
  type CompanySegment,
} from "@/features/identity/modules/types/modules.types";
import type { TenantFormData } from "../../schemas/tenant.schema";

type ModulesFieldsProps = {
  setValue: UseFormSetValue<TenantFormData>;
  watch: UseFormWatch<TenantFormData>;
};

const MODULE_LABELS = new Map(MODULE_OPTIONS.map((option) => [option.value, option.label]));

/**
 * Segmento e módulos na criação da empresa (só SUPER_ADMIN acessa /tenants).
 * Trocar o segmento marca a sugestão do segmento; depois cada caixa é livre.
 */
export function ModulesFields({ setValue, watch }: ModulesFieldsProps) {
  const segment = watch("segment") as CompanySegment;
  const modules = watch("enabledModules") as CompanyModuleName[];

  const handleSegmentChange = (value: CompanySegment) => {
    setValue("segment", value);
    setValue("enabledModules", [...SEGMENT_DEFAULT_MODULES[value]]);
  };

  const toggle = (module: CompanyModuleName, checked: boolean) =>
    setValue(
      "enabledModules",
      checked
        ? [...modules.filter((m) => m !== module), module]
        : modules.filter((m) => m !== module),
    );

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="tenant-segment">Segmento</Label>
        <Select value={segment} onValueChange={(v) => handleSegmentChange(v as CompanySegment)}>
          <SelectTrigger id="tenant-segment" className="max-w-sm">
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
        <p className="text-xs text-muted-foreground">
          Ao trocar o segmento, os módulos sugeridos são marcados. Você pode ajustar depois.
        </p>
      </div>

      <fieldset className="space-y-4">
        <legend className="text-sm font-medium">Módulos habilitados</legend>
        {MODULE_GROUPS.map((group) => (
          <div key={group.title} className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              {group.title}
            </p>
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
              {group.modules.map((module) => (
                <div key={module} className="flex items-center gap-2">
                  <Checkbox
                    id={`tenant-module-${module}`}
                    checked={modules.includes(module)}
                    onCheckedChange={(checked) => toggle(module, checked === true)}
                  />
                  <Label htmlFor={`tenant-module-${module}`}>{MODULE_LABELS.get(module)}</Label>
                </div>
              ))}
            </div>
          </div>
        ))}
      </fieldset>
    </div>
  );
}
