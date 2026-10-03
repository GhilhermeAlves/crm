"use client";

import { Badge } from "@/components/ui/badge";
import {
  APPOINTMENT_STATUS_LABELS,
  APPOINTMENT_STATUS_COLORS,
  type AppointmentStatus,
} from "../types/scheduling.types";

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus }) {
  return (
    <Badge variant="outline" className={APPOINTMENT_STATUS_COLORS[status]}>
      {APPOINTMENT_STATUS_LABELS[status]}
    </Badge>
  );
}
