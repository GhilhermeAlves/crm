"use client";

import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";

import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useAppointmentTypes,
  useCreateAppointmentType,
  useUpdateAppointmentType,
  useDeleteAppointmentType,
} from "@/features/sales/scheduling/hooks/useScheduling";
import { useSchedulingPermissions } from "@/features/sales/scheduling/schemas/scheduling.schema";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { AppointmentTypeFormDialog } from "@/features/sales/scheduling/components/AppointmentTypeFormDialog";
import type { AppointmentType } from "@/features/sales/scheduling/types/scheduling.types";
import type { CreateAppointmentTypeFormValues } from "@/features/sales/scheduling/schemas/scheduling.schema";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

const TIMEZONE_OPTIONS = [
  { value: "America/Sao_Paulo", label: "Brasília · UTC -3:00" },
  { value: "America/Manaus", label: "Manaus · UTC -4:00" },
  { value: "America/Belem", label: "Belém · UTC -3:00" },
  { value: "America/Cuiaba", label: "Cuiabá · UTC -4:00" },
  { value: "America/Rio_Branco", label: "Rio Branco · UTC -5:00" },
  { value: "America/Noronha", label: "Fernando de Noronha · UTC -2:00" },
];

const PRESET_COLORS = [
  { value: "#f97316", label: "Laranja" },
  { value: "#8b5cf6", label: "Roxo" },
  { value: "#3b82f6", label: "Azul" },
  { value: "#22c55e", label: "Verde" },
  { value: "#ef4444", label: "Vermelho" },
  { value: "#eab308", label: "Amarelo" },
  { value: "#ec4899", label: "Rosa" },
  { value: "#6b7280", label: "Cinza" },
];

type Marker = { id: string; name: string; color: string };

export default function AgendaGeneralPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const perms = useSchedulingPermissions();

  const { data: types = [] } = useAppointmentTypes(companyId);
  const { data: members = [] } = useMembers(companyId);
  const createType = useCreateAppointmentType(companyId);
  const updateType = useUpdateAppointmentType(companyId);
  const deleteType = useDeleteAppointmentType(companyId);

  const [typeFormOpen, setTypeFormOpen] = useState(false);
  const [editingType, setEditingType] = useState<AppointmentType | null>(null);

  const [timezone, setTimezone] = useState("America/Sao_Paulo");
  const [markers, setMarkers] = useState<Marker[]>([
    { id: "1", name: "Cadeira 1", color: "#f97316" },
    { id: "2", name: "Cirurgia", color: "#8b5cf6" },
  ]);
  const [newMarkerName, setNewMarkerName] = useState("");
  const [newMarkerColor, setNewMarkerColor] = useState("#3b82f6");

  const addMarker = () => {
    if (!newMarkerName.trim()) return;
    setMarkers((prev) => [
      ...prev,
      { id: crypto.randomUUID(), name: newMarkerName.trim(), color: newMarkerColor },
    ]);
    setNewMarkerName("");
  };

  const removeMarker = (id: string) => {
    setMarkers((prev) => prev.filter((m) => m.id !== id));
  };

  const handleCreateOrUpdate = (values: CreateAppointmentTypeFormValues) => {
    if (editingType) {
      updateType.mutate(
        { id: editingType.id, data: values },
        { onSuccess: () => { setTypeFormOpen(false); setEditingType(null); } },
      );
    } else {
      createType.mutate(values, { onSuccess: () => setTypeFormOpen(false) });
    }
  };

  const memberOptions = members.map((m) => ({ id: m.userId, name: m.name }));

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Ajustes gerais</h2>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <p className="text-sm text-muted-foreground">
          Defina os ajustes gerais da agenda da empresa.
        </p>

        <div className="space-y-1.5">
          <Label>Fuso horário</Label>
          <Select value={timezone} onValueChange={setTimezone}>
            <SelectTrigger className="w-72">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {TIMEZONE_OPTIONS.map((tz) => (
                <SelectItem key={tz.value} value={tz.value}>
                  {tz.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </section>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold">Marcadores</h3>
            <p className="text-xs text-muted-foreground">
              Gerencie os marcadores que poderão ser exibidos nos eventos da
              agenda.
            </p>
          </div>
        </div>

        <div className="flex items-end gap-2">
          <div className="flex-1 space-y-1.5">
            <Label>Nome do marcador</Label>
            <Input
              placeholder="Ex.: Sala 1"
              value={newMarkerName}
              onChange={(e) => setNewMarkerName(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addMarker())}
            />
          </div>
          <div className="space-y-1.5">
            <Label>Cor</Label>
            <Select value={newMarkerColor} onValueChange={setNewMarkerColor}>
              <SelectTrigger className="w-36">
                <div className="flex items-center gap-2">
                  <span
                    className="inline-block h-3 w-3 rounded-full"
                    style={{ backgroundColor: newMarkerColor }}
                  />
                  <SelectValue />
                </div>
              </SelectTrigger>
              <SelectContent>
                {PRESET_COLORS.map((c) => (
                  <SelectItem key={c.value} value={c.value}>
                    <div className="flex items-center gap-2">
                      <span
                        className="inline-block h-3 w-3 rounded-full"
                        style={{ backgroundColor: c.value }}
                      />
                      {c.label}
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Button size="sm" onClick={addMarker}>
            <Plus className="mr-1 h-3.5 w-3.5" />
            Novo marcador
          </Button>
        </div>

        {markers.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="text-xs">Marcador</TableHead>
                <TableHead className="text-xs">Cor</TableHead>
                <TableHead className="text-right text-xs">Ação</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {markers.map((m) => {
                const colorLabel =
                  PRESET_COLORS.find((c) => c.value === m.color)?.label ?? m.color;
                return (
                  <TableRow key={m.id}>
                    <TableCell className="text-sm">{m.name}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2 text-sm">
                        <span
                          className="inline-block h-3 w-3 rounded-full"
                          style={{ backgroundColor: m.color }}
                        />
                        {colorLabel}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => removeMarker(m.id)}
                      >
                        <Trash2 className="mr-1.5 h-3.5 w-3.5" />
                        Excluir
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </section>

      <AppointmentTypeFormDialog
        open={typeFormOpen}
        onOpenChange={(open) => {
          setTypeFormOpen(open);
          if (!open) setEditingType(null);
        }}
        isLoading={createType.isPending || updateType.isPending}
        onSubmit={handleCreateOrUpdate}
        members={memberOptions}
        editingType={editingType}
      />
    </div>
  );
}
