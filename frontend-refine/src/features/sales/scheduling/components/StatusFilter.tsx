"use client";

import { ChevronDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { APPOINTMENT_STATUS_LABELS, type AppointmentStatus } from "../types/scheduling.types";

const STATUSES = Object.keys(APPOINTMENT_STATUS_LABELS) as AppointmentStatus[];

type Props = {
  /** Vazio = todos os status. */
  value: Set<AppointmentStatus>;
  onChange: (value: Set<AppointmentStatus>) => void;
};

export function StatusFilter({ value, onChange }: Props) {
  const allSelected = value.size === 0 || value.size === STATUSES.length;

  const toggle = (status: AppointmentStatus) => {
    const next = new Set(allSelected ? [] : value);
    if (next.has(status)) next.delete(status);
    else next.add(status);
    onChange(next.size === STATUSES.length ? new Set() : next);
  };

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" className="h-8 gap-1.5">
          Status
          {!allSelected && (
            <span className="rounded-full bg-primary px-1.5 text-xs text-primary-foreground">
              {value.size}
            </span>
          )}
          <ChevronDown className="h-3.5 w-3.5" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-52 space-y-2 p-3">
        <div className="flex items-center gap-2 border-b pb-2">
          <Checkbox
            id="status-all"
            checked={allSelected}
            onCheckedChange={() => onChange(new Set())}
          />
          <Label htmlFor="status-all" className="text-sm font-medium">
            Marcar todos
          </Label>
        </div>
        {STATUSES.map((status) => (
          <div key={status} className="flex items-center gap-2">
            <Checkbox
              id={`status-${status}`}
              checked={!allSelected && value.has(status)}
              onCheckedChange={() => toggle(status)}
            />
            <Label htmlFor={`status-${status}`} className="text-sm">
              {APPOINTMENT_STATUS_LABELS[status]}
            </Label>
          </div>
        ))}
      </PopoverContent>
    </Popover>
  );
}
