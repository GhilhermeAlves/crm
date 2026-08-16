"use client";

import { AlertTriangle, RefreshCw } from "lucide-react";
import { cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

type ErrorCardProps = {
  title?: string;
  message: string;
  onRetry?: () => void;
  className?: string;
};

export function ErrorCard({
  title = "Algo deu errado",
  message,
  onRetry,
  className,
}: ErrorCardProps) {
  return (
    <Card className={cn("border-destructive/50", className)}>
      <CardContent className="flex flex-col items-center justify-center py-8 text-center">
        <div className="mb-4 rounded-full bg-destructive/10 p-3">
          <AlertTriangle className="h-6 w-6 text-destructive" />
        </div>
        <h3 className="text-lg font-semibold">{title}</h3>
        <p className="mt-1 max-w-sm text-sm text-muted-foreground">{message}</p>
        {onRetry && (
          <Button variant="outline" size="sm" className="mt-4 gap-2" onClick={onRetry}>
            <RefreshCw className="h-4 w-4" />
            Tentar novamente
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
