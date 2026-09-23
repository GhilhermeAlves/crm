import { type ReactNode } from "react";
import { StatusBadge, type StatusIntent } from "@/components/ui/status-badge";

type BadgeStatusProps = {
  children: ReactNode;
  variant?: "default" | "success" | "warning" | "danger" | "info";
  className?: string;
  withDot?: boolean;
  pulseDot?: boolean;
};

const intentMap: Record<string, StatusIntent> = {
  default: "neutral",
  success: "success",
  warning: "warning",
  danger: "danger",
  info: "info",
};

export function BadgeStatus({
  children,
  variant = "default",
  className,
  withDot = false,
  pulseDot = false,
}: BadgeStatusProps) {
  return (
    <StatusBadge
      intent={intentMap[variant] ?? "neutral"}
      className={className}
      withDot={withDot}
      pulseDot={pulseDot}
    >
      {children}
    </StatusBadge>
  );
}
