"use client";

import { AlertTriangle, Mail, Phone } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { ContactSummary } from "../types/contact.types";

const formatDate = (iso: string): string =>
  new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(new Date(iso));

export function ContactSummaryCard({ contact }: { contact: ContactSummary }) {
  return (
    <Card className={contact.atRisk ? "border-destructive/40" : undefined}>
      <CardContent className="flex flex-col gap-4 p-6 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-4">
          <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xl font-bold text-primary">
            {contact.initials}
          </span>
          <div className="space-y-1">
            <h2 className="text-2xl font-semibold">{contact.fullName}</h2>
            <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
              {contact.email && (
                <span className="flex items-center gap-1">
                  <Mail className="h-4 w-4" /> {contact.email}
                </span>
              )}
              {contact.phone && (
                <span className="flex items-center gap-1">
                  <Phone className="h-4 w-4" /> {contact.phone}
                </span>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              Cliente desde {formatDate(contact.createdAt)}
            </p>
          </div>
        </div>

        <div className="flex flex-col items-start gap-2 sm:items-end">
          {contact.atRisk ? (
            <Badge variant="destructive" className="gap-1 px-3 py-1">
              <AlertTriangle className="h-3 w-3" />
              Em risco
            </Badge>
          ) : (
            <Badge variant="secondary">Em dia</Badge>
          )}
          <p className="max-w-xs text-xs text-muted-foreground">
            {contact.atRisk && contact.riskMessage
              ? contact.riskMessage
              : `Última interação: ${formatDate(contact.lastInteractionAt)}`}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
