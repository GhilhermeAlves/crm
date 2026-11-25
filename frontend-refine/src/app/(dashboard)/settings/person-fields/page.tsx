"use client";

import { Fragment, useEffect, useMemo, useState } from "react";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { ErrorCard } from "@/components/common/ErrorCard";
import { toHttpError } from "@/lib/http-error";
import {
  useCompanyId,
  useFieldSettings,
  useHealthSegment,
  usePeoplePermissions,
  useSaveFieldSettings,
} from "@/features/masterdata/people/hooks/usePeople";
import { PeopleService } from "@/features/masterdata/people/services/people.service";
import {
  CONTEXT_LABELS,
  FIELD_GROUP_LABELS,
  formatDateTime,
} from "@/features/masterdata/people/lib/people-labels";
import type {
  FieldContext,
  FieldSettingChange,
} from "@/features/masterdata/people/types/people.types";

const CONTEXTS: FieldContext[] = ["PF", "PJ", "CUSTOMER", "SUPPLIER", "EMPLOYEE"];
const GROUP_ORDER = [
  "IDENTIFICATION",
  "ADDRESS",
  "CONTACT",
  "COMPLEMENTARY",
  "PATIENT",
  "PROFESSIONAL",
];

/** Minha Empresa → Cadastro de pessoas: obrigatoriedade de campos (doc 18.1). */
export default function PersonFieldsSettingsPage() {
  const companyId = useCompanyId();
  const qc = useQueryClient();
  const health = useHealthSegment();
  const { canSettings } = usePeoplePermissions();
  const { data, isLoading, isError, refetch } = useFieldSettings();
  const save = useSaveFieldSettings();
  const [draft, setDraft] = useState<Record<string, boolean>>({});
  const [resetting, setResetting] = useState(false);

  useEffect(() => setDraft({}), [data]);
  const groups = useMemo(() => {
    const out: Record<string, NonNullable<typeof data>["fields"]> = {};
    (data?.fields ?? [])
      .filter((f) => health || f.groupName !== "PATIENT")
      .forEach((f) => {
        (out[f.groupName] ??= []).push(f);
      });
    return out;
  }, [data, health]);

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 py-20 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando…
      </div>
    );
  }
  if (isError || !data)
    return (
      <ErrorCard message="Não foi possível carregar a configuração." onRetry={() => refetch()} />
    );

  const value = (key: string, ctx: FieldContext, base: boolean | null) =>
    draft[`${key}|${ctx}`] ?? base;
  const changes: FieldSettingChange[] = Object.entries(draft).map(([k, required]) => {
    const [fieldKey, context] = k.split("|");
    return { fieldKey, context: context as FieldContext, required };
  });
  const countFor = (ctx: FieldContext) =>
    data.fields.filter((f) => value(f.fieldKey, ctx, f.contexts[ctx]) === true).length;
  const setAll = (ctx: FieldContext, on: boolean) => {
    const next = { ...draft };
    data.fields.forEach((f) => {
      if (f.contexts[ctx] !== null && !f.systemRequired) next[`${f.fieldKey}|${ctx}`] = on;
    });
    setDraft(next);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">
          Cadastro de pessoas — campos obrigatórios
        </h1>
        <p className="text-sm text-muted-foreground">
          Marque os campos exigidos ao incluir ou atualizar pessoas, por tipo de pessoa e papel.
          Paciente usa a coluna Cliente. Cadastros provisórios só exigem o nome.
        </p>
        {data.updatedAt && (
          <p className="text-xs text-muted-foreground">
            Última alteração por {data.updatedByName ?? "—"} em {formatDateTime(data.updatedAt)}
          </p>
        )}
      </div>

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="bg-slate-900 text-white">
            <tr>
              <th className="px-4 py-2 text-left">Campo</th>
              {CONTEXTS.map((c) => (
                <th key={c} className="px-3 py-2 text-center">
                  <div>{CONTEXT_LABELS[c]}</div>
                  <div className="text-[11px] font-normal opacity-80">
                    {countFor(c)} obrigatórios
                  </div>
                  {canSettings && (
                    <div className="mt-1 flex justify-center gap-1 text-[11px] font-normal">
                      <button type="button" className="underline" onClick={() => setAll(c, true)}>
                        todos
                      </button>
                      <button type="button" className="underline" onClick={() => setAll(c, false)}>
                        nenhum
                      </button>
                    </div>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {GROUP_ORDER.filter((g) => groups[g]).map((g) => (
              <Fragment key={g}>
                <tr className="bg-blue-50 dark:bg-blue-950">
                  <td
                    colSpan={6}
                    className="px-4 py-1.5 text-xs font-semibold uppercase tracking-wide text-blue-900 dark:text-blue-100"
                  >
                    {FIELD_GROUP_LABELS[g]}
                  </td>
                </tr>
                {groups[g].map((f) => (
                  <tr key={f.fieldKey} className="border-t">
                    <td className="px-4 py-2">
                      {f.label}
                      {f.systemRequired && <span className="ml-1 text-destructive">*</span>}
                    </td>
                    {CONTEXTS.map((c) => {
                      const base = f.contexts[c];
                      if (base === null) {
                        return (
                          <td
                            key={c}
                            className="px-3 py-2 text-center text-muted-foreground"
                            aria-label="Não se aplica"
                          >
                            —
                          </td>
                        );
                      }
                      const checked = !!value(f.fieldKey, c, base);
                      return (
                        <td key={c} className="px-3 py-2 text-center">
                          <input
                            type="checkbox"
                            aria-label={`${f.label} obrigatório para ${CONTEXT_LABELS[c]}`}
                            checked={checked}
                            disabled={!canSettings || f.systemRequired}
                            onChange={(e) =>
                              setDraft({ ...draft, [`${f.fieldKey}|${c}`]: e.target.checked })
                            }
                          />
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </Fragment>
            ))}
          </tbody>
        </table>
      </div>
      <p className="text-xs text-muted-foreground">
        * obrigatório pelo sistema (não pode ser desmarcado) · — não se aplica
      </p>

      {canSettings && (
        <div className="flex flex-wrap justify-end gap-2">
          <Button
            variant="outline"
            disabled={resetting}
            onClick={async () => {
              setResetting(true);
              try {
                await PeopleService.resetFieldSettings(companyId as string);
                await qc.invalidateQueries({ queryKey: ["person-field-settings", companyId] });
                toast.success("Padrão restaurado");
              } catch (e) {
                toast.error(toHttpError(e).message);
              } finally {
                setResetting(false);
              }
            }}
          >
            Restaurar padrão
          </Button>
          <Button variant="outline" disabled={changes.length === 0} onClick={() => setDraft({})}>
            Cancelar
          </Button>
          <Button
            disabled={changes.length === 0 || save.isPending}
            onClick={() => save.mutate(changes)}
          >
            {save.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Salvar
          </Button>
        </div>
      )}
    </div>
  );
}
