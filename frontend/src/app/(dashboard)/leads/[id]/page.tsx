"use client";

import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { useLead, useUpdateLead } from "@/features/leads/hooks/useLeads";
import {
  LeadStatusBadge,
  LeadSourceBadge,
  LeadClassificationBadge,
} from "@/features/leads/components/LeadBadges";
import { ConvertLeadDialog } from "@/features/leads/components/ConvertLeadDialog";
import { useContact, useCustomer360 } from "@/features/contacts/hooks/useContacts";
import { useMembers } from "@/features/members/hooks/useMembers";
import { ContactSummaryCard } from "@/features/contacts/components/ContactSummaryCard";
import { TimelinePanel } from "@/features/contacts/components/TimelinePanel";
import { PageTitle } from "@/components/common/PageTitle";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorCard } from "@/components/common/ErrorCard";
import { Pencil, UserPlus } from "lucide-react";
import { ROUTES } from "@/lib/constants";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { useState, type ReactNode } from "react";
import type { Lead } from "@/features/leads/types/lead.types";

type FieldRow = { label: string; value: ReactNode };

function Field({ label, value }: FieldRow) {
  return (
    <div className="space-y-1">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-sm font-medium">{value}</dd>
    </div>
  );
}

function FieldsGrid({ fields }: { fields: FieldRow[] }) {
  return (
    <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      {fields.map((field) => (
        <Field key={field.label} {...field} />
      ))}
    </dl>
  );
}

const formatDateTime = (iso: string): string =>
  format(new Date(iso), "dd/MM/yyyy HH:mm", { locale: ptBR });

export default function LeadDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;

  const { data: lead, isLoading, error, refetch } = useLead(companyId, id);
  const { data: contact, isLoading: contactLoading } = useContact(
    companyId,
    lead?.contactId ?? null,
  );
  const { data: customer360 } = useCustomer360(companyId, lead?.contactId ?? null);
  const { data: members = [] } = useMembers(companyId);

  const [convertLead, setConvertLead] = useState<Lead | null>(null);
  const updateLeadMutation = useUpdateLead(companyId);
  const canUpdate = can("lead:update");

  if (isLoading) {
    return <Skeleton className="h-64 w-full" />;
  }

  if (error) {
    return <ErrorCard message={error.message} onRetry={() => refetch()} />;
  }

  if (!lead) {
    return (
      <div className="py-12 text-center">
        <p className="text-muted-foreground">Lead não encontrado.</p>
      </div>
    );
  }

  const contactName =
    customer360?.contact.fullName ??
    (contact ? `${contact.firstName}${contact.lastName ? ` ${contact.lastName}` : ""}` : null);
  const responsibleName = lead.assignedTo
    ? (members.find((m) => m.userId === lead.assignedTo)?.name ?? lead.assignedTo.slice(0, 8))
    : "—";
  const lastInteractionAt = customer360?.contact.lastInteractionAt ?? null;
  const nextAction = customer360?.nextAction;

  const identityFields: FieldRow[] = [
    { label: "Status", value: <LeadStatusBadge status={lead.status} /> },
    { label: "Origem", value: <LeadSourceBadge source={lead.source} /> },
    {
      label: "Classificação",
      value: <LeadClassificationBadge classification={lead.classification} />,
    },
    { label: "Score", value: lead.score },
  ];

  const contactFields: FieldRow[] = [
    { label: "Contato", value: contactName ?? "—" },
    { label: "Cargo", value: "—" },
    { label: "E-mail", value: customer360?.contact.email ?? contact?.email ?? "—" },
    { label: "Telefone", value: customer360?.contact.phone ?? contact?.phone ?? "—" },
  ];

  const relationshipFields: FieldRow[] = [
    { label: "Responsável", value: responsibleName },
    {
      label: "Última interação",
      value: lastInteractionAt ? formatDateTime(lastInteractionAt) : "—",
    },
    {
      label: "Próximo follow-up",
      value:
        nextAction && nextAction.type === "FOLLOW_UP" ? (
          <div className="space-y-0.5">
            <p>{nextAction.title}</p>
            {nextAction.description && (
              <p className="text-xs text-muted-foreground">{nextAction.description}</p>
            )}
          </div>
        ) : (
          "—"
        ),
    },
    { label: "Criado em", value: formatDateTime(lead.createdAt) },
    { label: "Atualizado em", value: formatDateTime(lead.updatedAt) },
  ];

  const handleConvert = () => {
    if (!convertLead) return;
    updateLeadMutation.mutate(
      { id: convertLead.id, data: { status: "CONVERTED" } },
      {
        onSuccess: () => setConvertLead(null),
      },
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <PageTitle>{contactName ?? "Lead"}</PageTitle>
        <div className="flex gap-2">
          {canUpdate && lead.status !== "CONVERTED" && (
            <Button variant="outline" onClick={() => setConvertLead(lead)}>
              <UserPlus className="mr-2 h-4 w-4" />
              Converter em contato
            </Button>
          )}
          <Button onClick={() => router.push(`${ROUTES.LEADS}/${lead.id}/edit`)}>
            <Pencil className="mr-2 h-4 w-4" />
            Editar
          </Button>
        </div>
      </div>

      {customer360 && <ContactSummaryCard contact={customer360.contact} />}

      <Card>
        <CardHeader>
          <CardTitle>Identificação</CardTitle>
        </CardHeader>
        <CardContent>
          <FieldsGrid fields={identityFields} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Contato</CardTitle>
        </CardHeader>
        <CardContent>
          <FieldsGrid fields={contactFields} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Relacionamento</CardTitle>
        </CardHeader>
        <CardContent>
          <FieldsGrid fields={relationshipFields} />
        </CardContent>
      </Card>

      {lead.notes && (
        <Card>
          <CardHeader>
            <CardTitle>Observações</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="whitespace-pre-wrap text-sm text-muted-foreground">{lead.notes}</p>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Histórico</CardTitle>
        </CardHeader>
        <CardContent>
          {customer360 ? (
            <TimelinePanel events={customer360.timeline} />
          ) : contactLoading ? (
            <Skeleton className="h-32 w-full" />
          ) : (
            <p className="text-sm text-muted-foreground">Indisponível para este lead.</p>
          )}
        </CardContent>
      </Card>

      <ConvertLeadDialog
        lead={convertLead}
        open={!!convertLead}
        onOpenChange={(open) => !open && setConvertLead(null)}
        onConfirm={handleConvert}
        isLoading={
          updateLeadMutation.isPending && updateLeadMutation.variables?.id === convertLead?.id
        }
      />
    </div>
  );
}
