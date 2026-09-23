"use client";

import { Badge } from "@/components/ui/badge";
import { CHANNEL_STATUS_LABELS, type ChannelStatus } from "../types/omnichannel.types";

const VARIANTS: Record<ChannelStatus, "default" | "secondary" | "destructive"> = {
  ACTIVE: "default",
  INACTIVE: "secondary",
  ERROR: "destructive",
};

export function ChannelStatusBadge({ status }: { status: ChannelStatus }) {
  return <Badge variant={VARIANTS[status]}>{CHANNEL_STATUS_LABELS[status]}</Badge>;
}
