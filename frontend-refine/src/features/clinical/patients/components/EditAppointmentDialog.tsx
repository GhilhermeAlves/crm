"use client";

import { useEffect, useState } from "react";
import { format, parseISO } from "date-fns";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { Appointment } from "@/features/sales/scheduling/types/scheduling.types";

export type EditAppointmentValues = {
  startAt: string;
  endAt: string;
  notes: string;
};

interface Props {
  appointment: Appointment | null;
  onOpenChange: (open: boolean) => void;
  onSubmit: (values: EditAppointmentValues) => void;
  isLoading: boolean;
}

/** Edição rápida de uma consulta na ficha do paciente: data, horário e observações. */
export function EditAppointmentDialog({ appointment, onOpenChange, onSubmit, isLoading }: Props) {
  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!appointment) return;
    const start = parseISO(appointment.startAt);
    const end = parseISO(appointment.endAt);
    setDate(format(start, "yyyy-MM-dd"));
    setStartTime(format(start, "HH:mm"));
    setEndTime(format(end, "HH:mm"));
    setNotes(appointment.notes ?? "");
    setError(null);
  }, [appointment]);

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const start = new Date(`${date}T${startTime}`);
    const end = new Date(`${date}T${endTime}`);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      setError("Informe data e horários válidos.");
      return;
    }
    if (end <= start) {
      setError("O término deve ser depois do início.");
      return;
    }
    onSubmit({ startAt: start.toISOString(), endAt: end.toISOString(), notes: notes.trim() });
  };

  return (
    <Dialog open={appointment !== null} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Editar consulta</DialogTitle>
          <DialogDescription>Altere a data, o horário ou as observações.</DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="edit-appointment-date">Data</Label>
            <Input
              id="edit-appointment-date"
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="edit-appointment-start">Início</Label>
              <Input
                id="edit-appointment-start"
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="edit-appointment-end">Término</Label>
              <Input
                id="edit-appointment-end"
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                required
              />
            </div>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="edit-appointment-notes">Observações</Label>
            <Textarea
              id="edit-appointment-notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
            />
          </div>
          {error && <p className="text-sm text-destructive">{error}</p>}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isLoading} className="gap-1.5">
              {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
              Salvar
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
