import { type ReactNode } from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

type AuthLayoutProps = {
  title?: string;
  description?: string;
  children: ReactNode;
};

export function AuthLayout({ title, description, children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-crm-background p-4">
      <Card className="w-full max-w-md border-crm-border bg-crm-surface">
        {title && (
          <CardHeader className="space-y-1 text-center">
            <h1 className="text-2xl font-semibold tracking-tight text-crm-text">
              {title}
            </h1>
            {description && (
              <p className="text-sm text-crm-text-secondary">{description}</p>
            )}
          </CardHeader>
        )}
        <CardContent>{children}</CardContent>
      </Card>
    </div>
  );
}
