"use client";

import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, Trash2 } from "lucide-react";
import {
  workflowFormSchema,
  workflowFormToPayload,
  workflowToFormValues,
  WORKFLOW_TRIGGERS,
  CONDITION_OPERATORS,
  CONDITION_OPERATOR_LABELS,
  WORKFLOW_ACTION_TYPES,
  TASK_PRIORITIES,
  TASK_PRIORITY_LABELS,
  ACTIVITY_TYPES,
  ACTIVITY_TYPE_LABELS,
  type WorkflowFormValues,
} from "../schemas/workflow.schema";
import {
  WORKFLOW_TRIGGER_LABELS,
  WORKFLOW_ACTION_LABELS,
  CONDITION_FIELDS,
  type Workflow,
} from "../types/workflow.types";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";

type Props = {
  initial?: Workflow | null;
  isLoading?: boolean;
  submitLabel?: string;
  onSubmit: (payload: ReturnType<typeof workflowFormToPayload>) => void;
};

export function WorkflowForm({
  initial,
  isLoading,
  submitLabel = "Salvar",
  onSubmit,
}: Props) {
  const form = useForm<WorkflowFormValues>({
    resolver: zodResolver(workflowFormSchema),
    defaultValues: initial
      ? workflowToFormValues(initial)
      : {
          name: "",
          description: "",
          trigger: "OPPORTUNITY_STAGE_CHANGED",
          conditions: [],
          actions: [
            {
              actionType: "CREATE_TASK",
              title: "",
              description: "",
              priority: "MEDIUM",
              dueInDays: "",
              activityType: "OTHER",
            },
          ],
        },
  });

  const trigger = form.watch("trigger");

  const conditions = useFieldArray({
    control: form.control,
    name: "conditions",
  });
  const actions = useFieldArray({ control: form.control, name: "actions" });

  const handleTriggerChange = (value: string) => {
    form.setValue("trigger", value as WorkflowFormValues["trigger"]);
    form.setValue("conditions", []);
  };

  const handleSubmit = (values: WorkflowFormValues) => {
    onSubmit(workflowFormToPayload(values));
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6">
        <Card>
          <CardContent className="space-y-4 pt-6">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nome</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="Ex.: Follow-up de proposta"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descrição</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Descreva o objetivo do workflow"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="trigger"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Disparo</FormLabel>
                  <Select
                    value={field.value}
                    onValueChange={handleTriggerChange}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione o evento" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {WORKFLOW_TRIGGERS.map((t) => (
                        <SelectItem key={t} value={t}>
                          {WORKFLOW_TRIGGER_LABELS[t]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardContent className="space-y-4 pt-6">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold">Condições (opcional)</h3>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() =>
                  conditions.append({
                    field: CONDITION_FIELDS[trigger][0]?.value ?? "",
                    operator: "EQUALS",
                    value: "",
                  })
                }
              >
                <Plus className="mr-2 h-4 w-4" />
                Adicionar condição
              </Button>
            </div>

            {conditions.fields.map((field, index) => (
              <div
                key={field.id}
                className="grid grid-cols-1 gap-3 md:grid-cols-[1fr_1fr_1fr_auto]"
              >
                <FormField
                  control={form.control}
                  name={`conditions.${index}.field`}
                  render={({ field }) => (
                    <FormItem>
                      <Select
                        value={field.value}
                        onValueChange={field.onChange}
                      >
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
                  control={form.control}
                  name={`conditions.${index}.operator`}
                  render={({ field }) => (
                    <FormItem>
                      <Select
                        value={field.value}
                        onValueChange={field.onChange}
                      >
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
                  control={form.control}
                  name={`conditions.${index}.value`}
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <Input
                          placeholder="Valor (ex.: Negociação)"
                          {...field}
                        />
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
                  onClick={() => conditions.remove(index)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
            {conditions.fields.length === 0 && (
              <p className="text-sm text-muted-foreground">
                Sem condições — o workflow será executado para todo evento do
                disparo.
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardContent className="space-y-4 pt-6">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold">Ações</h3>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() =>
                  actions.append({
                    actionType: "CREATE_TASK",
                    title: "",
                    description: "",
                    priority: "MEDIUM",
                    dueInDays: "",
                    activityType: "OTHER",
                  })
                }
              >
                <Plus className="mr-2 h-4 w-4" />
                Adicionar ação
              </Button>
            </div>

            {actions.fields.map((field, index) => {
              const actionType = form.watch(`actions.${index}.actionType`);
              return (
                <div key={field.id} className="space-y-3 rounded-md border p-4">
                  <div className="flex items-center justify-between gap-3">
                    <FormField
                      control={form.control}
                      name={`actions.${index}.actionType`}
                      render={({ field }) => (
                        <FormItem className="flex-1">
                          <Select
                            value={field.value}
                            onValueChange={field.onChange}
                          >
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
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => actions.remove(index)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>

                  <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                    <FormField
                      control={form.control}
                      name={`actions.${index}.title`}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>
                            {actionType === "CREATE_TASK"
                              ? "Título da tarefa"
                              : "Assunto da atividade"}
                          </FormLabel>
                          <FormControl>
                            <Input
                              placeholder="Ex.: Entrar em contato"
                              {...field}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    {actionType === "CREATE_TASK" ? (
                      <FormField
                        control={form.control}
                        name={`actions.${index}.priority`}
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Prioridade</FormLabel>
                            <Select
                              value={field.value}
                              onValueChange={field.onChange}
                            >
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
                        control={form.control}
                        name={`actions.${index}.activityType`}
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Tipo</FormLabel>
                            <Select
                              value={field.value}
                              onValueChange={field.onChange}
                            >
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
                      control={form.control}
                      name={`actions.${index}.dueInDays`}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Vencimento (dias)</FormLabel>
                          <FormControl>
                            <Input
                              type="number"
                              min={0}
                              placeholder="0 = sem vencimento"
                              {...field}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  )}

                  <FormField
                    control={form.control}
                    name={`actions.${index}.description`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Descrição</FormLabel>
                        <FormControl>
                          <Textarea
                            rows={2}
                            placeholder="Detalhes"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              );
            })}
          </CardContent>
        </Card>

        <div className="flex justify-end gap-2">
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Salvando…" : submitLabel}
          </Button>
        </div>
      </form>
    </Form>
  );
}
