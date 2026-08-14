"use client";

import { useState } from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Plus } from "lucide-react";
import {
  useTasks,
  useTasksDueToday,
  useCreateTask,
  useChangeTaskStatus,
} from "@/features/tasks/hooks/useTasks";
import { TaskList } from "@/features/tasks/components/TaskList";
import { CreateTaskDialog } from "@/features/tasks/components/CreateTaskDialog";
import { useTaskPermissions } from "@/features/tasks/schemas/task.schema";
import type { TaskStatus } from "@/features/tasks/types/task.types";

export default function TasksPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const perms = useTaskPermissions();

  const [createOpen, setCreateOpen] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);

  const { data: allTasks = [], isLoading } = useTasks(companyId);
  const { data: dueToday = [], isLoading: dueLoading } = useTasksDueToday(companyId);

  const createTask = useCreateTask(companyId);
  const changeStatus = useChangeTaskStatus(companyId);

  const handleStatus = (id: string, status: TaskStatus) => {
    setBusyId(id);
    changeStatus.mutate({ id, status }, { onSettled: () => setBusyId(null) });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <PageTitle>Tarefas</PageTitle>
        {perms.canCreate && (
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Nova Tarefa
          </Button>
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base font-semibold">Vencendo hoje</CardTitle>
          </CardHeader>
          <CardContent>
            <TaskList
              tasks={dueToday}
              isLoading={dueLoading}
              canUpdate={perms.canUpdate}
              busyId={busyId}
              onChangeStatus={handleStatus}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base font-semibold">Todas as tarefas</CardTitle>
          </CardHeader>
          <CardContent>
            <TaskList
              tasks={allTasks}
              isLoading={isLoading}
              canUpdate={perms.canUpdate}
              busyId={busyId}
              onChangeStatus={handleStatus}
            />
          </CardContent>
        </Card>
      </div>

      <CreateTaskDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        isLoading={createTask.isPending}
        onSubmit={(values) =>
          createTask.mutate(values, { onSuccess: () => setCreateOpen(false) })
        }
      />
    </div>
  );
}