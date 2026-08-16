"use client";

import { useState } from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Plus } from "lucide-react";
import {
  useActivities,
  useCreateActivity,
} from "@/features/activities/hooks/useActivities";
import { ActivityTimeline } from "@/features/activities/components/ActivityTimeline";
import { CreateActivityDialog } from "@/features/activities/components/CreateActivityDialog";
import { useActivityPermissions } from "@/features/activities/schemas/activity.schema";

export default function ActivitiesPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const perms = useActivityPermissions();

  const [createOpen, setCreateOpen] = useState(false);

  const { data: activities = [], isLoading } = useActivities(companyId);
  const createActivity = useCreateActivity(companyId);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <PageTitle>Timeline</PageTitle>
        {perms.canCreate && (
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Registrar Atividade
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">
            Atividades da empresa
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ActivityTimeline activities={activities} isLoading={isLoading} />
        </CardContent>
      </Card>

      <CreateActivityDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        isLoading={createActivity.isPending}
        onSubmit={(values) =>
          createActivity.mutate(values, {
            onSuccess: () => setCreateOpen(false),
          })
        }
      />
    </div>
  );
}
