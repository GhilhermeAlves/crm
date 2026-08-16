"use client";

import { RefreshCw } from "lucide-react";
import { Button, type ButtonProps } from "@/components/ui/button";

type RetryButtonProps = {
  onRetry: () => void;
  label?: string;
} & Omit<ButtonProps, "onClick">;

export function RetryButton({
  onRetry,
  label = "Tentar novamente",
  ...props
}: RetryButtonProps) {
  return (
    <Button
      variant="outline"
      size="sm"
      onClick={onRetry}
      className="gap-2"
      {...props}
    >
      <RefreshCw className="h-4 w-4" />
      {label}
    </Button>
  );
}
