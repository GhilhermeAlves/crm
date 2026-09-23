"use client";

import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "@/components/ui/form";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
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
import {
  workflowFormSchema,
  workflowFormToPayload,
  workflowToFormValues,
  WORKFLOW_TRIGGERS,
  type WorkflowFormValues,
} from "../schemas/workflow.schema";
import { WORKFLOW_TRIGGER_LABELS, CONDITION_FIELDS, type Workflow } from "../types/workflow.types";
import { WorkflowFormSection } from "./workflow-form/WorkflowFormSection";
import { ConditionBuilder } from "./workflow-form/ConditionBuilder";
import { ActionBuilder } from "./workflow-form/ActionBuilder";

type Props = {
  initial?: Workflow | null;
  isLoading?: boolean;
  submitLabel?: string;
  onSubmit: (payload: ReturnType<typeof workflowFormToPayload>) => void;
};

export function WorkflowForm({ initial, isLoading, submitLabel = "Salvar", onSubmit }: Props) {
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

  const addCondition = () =>
    conditions.append({
      field: CONDITION_FIELDS[trigger][0]?.value ?? "",
      operator: "EQUALS",
      value: "",
    });

  const addAction = () =>
    actions.append({
      actionType: "CREATE_TASK",
      title: "",
      description: "",
      priority: "MEDIUM",
      dueInDays: "",
      activityType: "OTHER",
    });

  const handleSubmit = (values: WorkflowFormValues) => {
    onSubmit(workflowFormToPayload(values));
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6">
        <WorkflowFormSection>
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Nome</FormLabel>
                <FormControl>
                  <Input placeholder="Ex.: Follow-up de proposta" {...field} />
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
                  <Textarea placeholder="Descreva o objetivo do workflow" {...field} />
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
                <Select value={field.value} onValueChange={handleTriggerChange}>
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
        </WorkflowFormSection>

        <WorkflowFormSection>
          <ConditionBuilder
            control={form.control}
            trigger={trigger}
            fields={conditions.fields}
            onAdd={addCondition}
            onRemove={conditions.remove}
          />
        </WorkflowFormSection>

        <WorkflowFormSection>
          <ActionBuilder
            control={form.control}
            fields={actions.fields}
            onAdd={addAction}
            onRemove={actions.remove}
          />
        </WorkflowFormSection>

        <div className="flex justify-end gap-2">
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Salvando…" : submitLabel}
          </Button>
        </div>
      </form>
    </Form>
  );
}
