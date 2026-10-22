"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { CalendarPlus, ChevronRight, Loader2, PhoneCall, TrendingUp } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { ROUTES } from "@/lib/constants";
import { useContact, useCustomer360 } from "@/features/masterdata/contacts/hooks/useContacts";
import { ContactPatientAction } from "@/features/clinical/patients/components/ContactPatientAction";
import { ContactSummaryCard } from "@/features/masterdata/contacts/components/ContactSummaryCard";
import { NextActionCard } from "@/features/masterdata/contacts/components/NextActionCard";
import { OpportunitiesPanel } from "@/features/masterdata/contacts/components/OpportunitiesPanel";
import { TasksPanel } from "@/features/masterdata/contacts/components/TasksPanel";
import { TimelinePanel } from "@/features/masterdata/contacts/components/TimelinePanel";
import { CreateActivityDialog } from "@/features/sales/activities/components/CreateActivityDialog";
import { useCreateActivity } from "@/features/sales/activities/hooks/useActivities";
import { useActivityPermissions } from "@/features/sales/activities/schemas/activity.schema";
import { CreateTaskDialog } from "@/features/sales/tasks/components/CreateTaskDialog";
import { useCreateTask } from "@/features/sales/tasks/hooks/useTasks";
import { useTaskPermissions } from "@/features/sales/tasks/schemas/task.schema";
import { useState } from "react";

export default function Customer360Page() {
  const params = useParams<{ id: string }>();
  const contactId = params?.id;

  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const { data: c360, isLoading, isError } = useCustomer360(companyId, contactId);
  const { data: contact } = useContact(companyId, contactId);
  const queryClient = useQueryClient();

  const activityPerms = useActivityPermissions();
  const taskPerms = useTaskPermissions();

  const createActivity = useCreateActivity(companyId);
  const createTask = useCreateTask(companyId);

  const [activityOpen, setActivityOpen] = useState(false);
  const [taskOpen, setTaskOpen] = useState(false);

  // Ações rápidas alteram o contexto; recarrega o Customer 360.
  useEffect(() => {
    if (createActivity.isSuccess || createTask.isSuccess) {
      queryClient.invalidateQueries({
        queryKey: ["customer360", companyId, contactId],
      });
    }
  }, [createActivity.isSuccess, createTask.isSuccess, companyId, contactId, queryClient]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando visão do cliente…
      </div>
    );
  }

  if (isError || !c360) {
    return (
      <p className="py-20 text-center text-sm text-destructive">
        Não foi possível carregar o Customer 360 deste contato.
      </p>
    );
  }

  const fullName = contact ? [contact.firstName, contact.lastName].filter(Boolean).join(" ") : "";

  return (
    <div className="space-y-6">
      <nav aria-label="Trilha de navegação" className="flex items-center gap-1 text-sm">
        <Link
          href={ROUTES.CONTACTS}
          className="text-muted-foreground transition-colors hover:text-foreground"
        >
          Contatos
        </Link>
        <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />
        <span className="font-medium text-foreground">{fullName || "Contato"}</span>
      </nav>

      <div className="flex flex-wrap justify-end gap-2">
        {contactId && (
          <ContactPatientAction
            companyId={companyId}
            contactId={contactId}
            isPatient={!!contact?.isPatient}
          />
        )}
        {activityPerms.canCreate && (
          <Button size="sm" onClick={() => setActivityOpen(true)}>
            <PhoneCall className="mr-1 h-4 w-4" /> Registrar atividade
          </Button>
        )}
        {taskPerms.canCreate && (
          <Button size="sm" variant="outline" onClick={() => setTaskOpen(true)}>
            <CalendarPlus className="mr-1 h-4 w-4" /> Nova tarefa
          </Button>
        )}
        <Button size="sm" variant="outline" asChild>
          <Link href={ROUTES.PIPELINE}>
            <TrendingUp className="mr-1 h-4 w-4" /> Pipeline
          </Link>
        </Button>
      </div>

      <ContactSummaryCard contact={c360.contact} />

      <NextActionCard nextAction={c360.nextAction} />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader className="pb-4">
            <CardTitle className="text-base font-semibold">Oportunidades</CardTitle>
          </CardHeader>
          <CardContent>
            <OpportunitiesPanel
              opportunities={c360.opportunities}
              openCount={c360.openOpportunities}
              openValue={c360.openValue}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-4">
            <CardTitle className="text-base font-semibold">Tarefas</CardTitle>
          </CardHeader>
          <CardContent>
            <TasksPanel tasks={c360.tasks} />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-base font-semibold">Linha do tempo</CardTitle>
        </CardHeader>
        <CardContent>
          <TimelinePanel events={c360.timeline} />
        </CardContent>
      </Card>

      <CreateActivityDialog
        open={activityOpen}
        onOpenChange={setActivityOpen}
        isLoading={createActivity.isPending}
        contactId={contactId}
        onSubmit={(values) => createActivity.mutate({ ...values, contactId })}
      />

      <CreateTaskDialog
        open={taskOpen}
        onOpenChange={setTaskOpen}
        isLoading={createTask.isPending}
        onSubmit={(values) => createTask.mutate({ ...values, contactId })}
      />
    </div>
  );
}
