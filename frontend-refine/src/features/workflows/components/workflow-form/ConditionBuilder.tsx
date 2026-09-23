"use client";

import { Plus, Trash2 } from "lucide-react";
import type { Control, FieldArrayWithId } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { CONDITION_FIELDS } from "../../types/workflow.types";
import {
  CONDITION_OPERATOR_LABELS,
  CONDITION_OPERATORS,
  type WorkflowFormValues,
} from "../../schemas/workflow.schema";

type ConditionBuilderProps = {
  control: Control<WorkflowFormValues>;
  trigger: WorkflowFormValues["trigger"];
  fields: Array<FieldArrayWithId<WorkflowFormValues, "conditions", "id">>;
  onAdd: () => void;
  onRemove: (index: number) => void;
};

export function ConditionBuilder({
  control,
  trigger,
  fields,
  onAdd,
  onRemove,
}: ConditionBuilderProps) {
  return (
    <>
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">Condições (opcional)</h3>
        <Button type="button" variant="outline" size="sm" onClick={onAdd}>
          <Plus className="mr-2 h-4 w-4" />
          Adicionar condição
        </Button>
      </div>

      {fields.map((field, index) => (
        <div key={field.id} className="grid grid-cols-1 gap-3 md:grid-cols-[1fr_1fr_1fr_auto]">
          <FormField
            control={control}
            name={`conditions.${index}.field`}
            render={({ field }) => (
              <FormItem>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Campo" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {CONDITION_FIELDS[trigger].map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={control}
            name={`conditions.${index}.operator`}
            render={({ field }) => (
              <FormItem>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Operador" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {CONDITION_OPERATORS.map((op) => (
                      <SelectItem key={op} value={op}>
                        {CONDITION_OPERATOR_LABELS[op]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={control}
            name={`conditions.${index}.value`}
            render={({ field }) => (
              <FormItem>
                <FormControl>
                  <Input placeholder="Valor (ex.: Negociação)" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="mt-1 self-end"
            onClick={() => onRemove(index)}
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      ))}
      {fields.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Sem condições — o workflow será executado para todo evento do disparo.
        </p>
      )}
    </>
  );
}
