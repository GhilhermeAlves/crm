"use client";

import { useState } from "react";
import { Pencil, Trash2, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { AppointmentType } from "../types/scheduling.types";
import { LOCATION_KIND_LABELS } from "../types/scheduling.types";

interface Props {
  types: AppointmentType[];
  isLoading: boolean;
  canConfigure: boolean;
  onCreateClick: () => void;
  onEditClick: (type: AppointmentType) => void;
  onDeleteClick: (type: AppointmentType) => void;
}

export function AppointmentTypeList({
  types,
  isLoading,
  canConfigure,
  onCreateClick,
  onEditClick,
  onDeleteClick,
}: Props) {
  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Tipos de agendamento</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">Carregando…</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-base">Tipos de agendamento</CardTitle>
        {canConfigure && (
          <Button size="sm" onClick={onCreateClick}>
            <Plus className="mr-2 h-4 w-4" />
            Novo tipo
          </Button>
        )}
      </CardHeader>
      <CardContent>
        {types.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nenhum tipo de agendamento cadastrado.</p>
        ) : (
          <div className="space-y-3">
            {types.map((t) => (
              <div
                key={t.id}
                className="flex items-center justify-between gap-3 rounded-lg border p-3"
              >
                <div className="flex min-w-0 items-center gap-3">
                  {t.color && (
                    <div
                      className="h-3 w-3 shrink-0 rounded-full"
                      style={{ backgroundColor: t.color }}
                    />
                  )}
                  <div className="min-w-0">
                    <p className="truncate font-medium">{t.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {t.durationMinutes}min
                      {t.locationKind ? ` · ${LOCATION_KIND_LABELS[t.locationKind]}` : ""}
                      {t.hostIds.length > 0 ? ` · ${t.hostIds.length} responsável(is)` : ""}
                    </p>
                  </div>
                </div>

                <div className="flex shrink-0 items-center gap-2">
                  {!t.active && <Badge variant="secondary">Inativo</Badge>}
                  {t.publicBookingEnabled && <Badge variant="outline">Público</Badge>}
                  {canConfigure && (
                    <>
                      <Button variant="ghost" size="icon" onClick={() => onEditClick(t)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => onDeleteClick(t)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
