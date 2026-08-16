"use client";

import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useLead } from "@/features/leads/hooks/useLeads";
import {
  LeadStatusBadge,
  LeadSourceBadge,
  LeadClassificationBadge,
} from "@/features/leads/components/LeadBadges";
import { PageTitle } from "@/components/common/PageTitle";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Pencil } from "lucide-react";
import { ROUTES } from "@/lib/constants";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";

export default function LeadDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const { data: lead, isLoading } = useLead(companyId, id);

  if (isLoading) {
    return <Skeleton className="h-64 w-full" />;
  }

  if (!lead) {
    return (
      <div className="py-12 text-center">
        <p className="text-muted-foreground">Lead não encontrado.</p>
      </div>
    );
  }

  const rows = [
    { label: "Status", value: <LeadStatusBadge status={lead.status} /> },
    { label: "Origem", value: <LeadSourceBadge source={lead.source} /> },
    {
      label: "Classificação",
      value: <LeadClassificationBadge classification={lead.classification} />,
    },
    { label: "Score", value: lead.score },
    { label: "Contato (ID)", value: lead.contactId },
    { label: "Responsável (ID)", value: lead.assignedTo ?? "—" },
    {
      label: "Criado em",
      value: format(new Date(lead.createdAt), "dd/MM/yyyy HH:mm", {
        locale: ptBR,
      }),
    },
    {
      label: "Atualizado em",
      value: format(new Date(lead.updatedAt), "dd/MM/yyyy HH:mm", {
        locale: ptBR,
      }),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageTitle>Lead</PageTitle>
        <Button onClick={() => router.push(`${ROUTES.LEADS}/${lead.id}/edit`)}>
          <Pencil className="mr-2 h-4 w-4" />
          Editar
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Detalhes do lead</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {rows.map((row) => (
              <div key={row.label} className="space-y-1">
                <dt className="text-sm text-muted-foreground">{row.label}</dt>
                <dd className="text-sm font-medium">{row.value}</dd>
              </div>
            ))}
          </dl>
          {lead.notes && (
            <div className="mt-6 space-y-1">
              <dt className="text-sm text-muted-foreground">Notas</dt>
              <dd className="whitespace-pre-wrap text-sm text-muted-foreground">{lead.notes}</dd>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
