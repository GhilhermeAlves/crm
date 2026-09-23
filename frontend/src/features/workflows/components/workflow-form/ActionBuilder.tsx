"use client";

import { Plus, Trash2 } from "lucide-react";
import { useWatch, type Control, type FieldArrayWithId } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { WORKFLOW_ACTION_LABELS } from "../../types/workflow.types";
import {
  ACTIVITY_TYPES,
  ACTIVITY_TYPE_LABELS,
  TASK_PRIORITIES,
  TASK_PRIORITY_LABELS,
  WORKFLOW_ACTION_TYPES,
  type WorkflowFormValues,
} from "../../schemas/workflow.schema";

type ActionBuilderProps = {
  control: Control<WorkflowFormValues>;
  fields: Array<FieldArrayWithId<WorkflowFormValues, "actions", "id">>;
  onAdd: () => void;
  onRemove: (index: number) => void;
};

export function ActionBuilder({ control, fields, onAdd, onRemove }: ActionBuilderProps) {
  return (
    <>
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">Ações</h3>
        <Button type="button" variant="outline" size="sm" onClick={onAdd}>
          <Plus className="mr-2 h-4 w-4" />
          Adicionar ação
        </Button>
      </div>

      {fields.map((field, index) => (
        <ActionItem
          key={field.id}
          control={control}
          index={index}
          onRemove={() => onRemove(index)}
        />
      ))}
    </>
  );
}

function ActionItem({
  control,
  index,
  onRemove,
}: {
  control: Control<WorkflowFormValues>;
  index: number;
  onRemove: () => void;
}) {
  const actionType = useWatch({ control, name: `actions.${index}.actionType` });

  return (
    <div className="space-y-3 rounded-md border p-4">
      <div className="flex items-center justify-between gap-3">
        <FormField
          control={control}
          name={`actions.${index}.actionType`}
          render={({ field }) => (
            <FormItem className="flex-1">
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Tipo de ação" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {WORKFLOW_ACTION_TYPES.map((at) => (
                    <SelectItem key={at} value={at}>
                      {WORKFLOW_ACTION_LABELS[at]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="button" variant="ghost" size="icon" onClick={onRemove}>
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <FormField
          control={control}
          name={`actions.${index}.title`}
          render={({ field }) => (
            <FormItem>
              <FormLabel>
                {actionType === "CREATE_TASK" ? "Título da tarefa" : "Assunto da atividade"}
              </FormLabel>
              <FormControl>
                <Input placeholder="Ex.: Entrar em contato" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        {actionType === "CREATE_TASK" ? (
          <FormField
            control={control}
            name={`actions.${index}.priority`}
            render={({ field }) => (
              <FormItem>
                <FormLabel>Prioridade</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Prioridade" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {TASK_PRIORITIES.map((p) => (
                      <SelectItem key={p} value={p}>
                        {TASK_PRIORITY_LABELS[p]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        ) : (
          <FormField
            control={control}
            name={`actions.${index}.activityType`}
            render={({ field }) => (
              <FormItem>
                <FormLabel>Tipo</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Tipo" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {ACTIVITY_TYPES.map((t) => (
                      <SelectItem key={t} value={t}>
                        {ACTIVITY_TYPE_LABELS[t]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        )}
      </div>

      {actionType === "CREATE_TASK" && (
        <FormField
          control={control}
          name={`actions.${index}.dueInDays`}
          render={({ field }) => (
            <FormItem>
              <FormLabel>Vencimento (dias)</FormLabel>
              <FormControl>
                <Input type="number" min={0} placeholder="0 = sem vencimento" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      )}

      <FormField
        control={control}
        name={`actions.${index}.description`}
        render={({ field }) => (
          <FormItem>
            <FormLabel>Descrição</FormLabel>
            <FormControl>
              <Textarea rows={2} placeholder="Detalhes" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}
