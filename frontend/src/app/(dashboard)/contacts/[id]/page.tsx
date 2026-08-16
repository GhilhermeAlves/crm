"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { CalendarPlus, Loader2, PhoneCall, TrendingUp } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { ROUTES } from "@/lib/constants";
import { useCustomer360 } from "@/features/contacts/hooks/useContacts";
import { ContactSummaryCard } from "@/features/contacts/components/ContactSummaryCard";
import { NextActionCard } from "@/features/contacts/components/NextActionCard";
import { OpportunitiesPanel } from "@/features/contacts/components/OpportunitiesPanel";
import { TasksPanel } from "@/features/contacts/components/TasksPanel";
import { TimelinePanel } from "@/features/contacts/components/TimelinePanel";
import { CreateActivityDialog } from "@/features/activities/components/CreateActivityDialog";
import { useCreateActivity } from "@/features/activities/hooks/useActivities";
import { useActivityPermissions } from "@/features/activities/schemas/activity.schema";
import { CreateTaskDialog } from "@/features/tasks/components/CreateTaskDialog";
import { useCreateTask } from "@/features/tasks/hooks/useTasks";
import { useTaskPermissions } from "@/features/tasks/schemas/task.schema";
import { useState } from "react";

export default function Customer360Page() {
  const params = useParams<{ id: string }>();
  const contactId = params?.id;

  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const {
    data: c360,
    isLoading,
    isError,
  } = useCustomer360(companyId, contactId);
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
  }, [
    createActivity.isSuccess,
    createTask.isSuccess,
    companyId,
    contactId,
    queryClient,
  ]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando visão do
        cliente…
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

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <Link
            href={ROUTES.CONTACTS}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            ← Contatos
          </Link>
          <h1 className="text-2xl font-bold tracking-tight">Customer 360</h1>
        </div>
        <div className="flex flex-wrap gap-2">
          {activityPerms.canCreate && (
            <Button size="sm" onClick={() => setActivityOpen(true)}>
              <PhoneCall className="mr-1 h-4 w-4" /> Registrar atividade
            </Button>
          )}
          {taskPerms.canCreate && (
            <Button
              size="sm"
              variant="outline"
              onClick={() => setTaskOpen(true)}
            >
              <CalendarPlus className="mr-1 h-4 w-4" /> Nova tarefa
            </Button>
          )}
          <Button size="sm" variant="outline" asChild>
            <Link href={ROUTES.PIPELINE}>
              <TrendingUp className="mr-1 h-4 w-4" /> Pipeline
            </Link>
          </Button>
        </div>
      </div>

      <ContactSummaryCard contact={c360.contact} />

      <NextActionCard nextAction={c360.nextAction} />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader className="pb-4">
            <CardTitle className="text-base font-semibold">
              Oportunidades
            </CardTitle>
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
          <CardTitle className="text-base font-semibold">
            Linha do tempo
          </CardTitle>
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
