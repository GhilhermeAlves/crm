import { type ReactNode } from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

type AuthLayoutProps = {
  title?: string;
  description?: string;
  children: ReactNode;
};

/**
 * html/body têm overflow hidden (shell do dashboard), então esta área rola por
 * conta própria; `m-auto` centraliza o card quando cabe e deixa rolar quando não.
 */
export function AuthLayout({ title, description, children }: AuthLayoutProps) {
  return (
    <div className="flex h-dvh overflow-y-auto bg-crm-background p-4">
      <Card className="m-auto w-full max-w-md border-crm-border bg-crm-surface">
        {title && (
          <CardHeader className="space-y-1 text-center">
            <h1 className="text-2xl font-semibold tracking-tight text-crm-text">{title}</h1>
            {description && <p className="text-sm text-crm-text-secondary">{description}</p>}
          </CardHeader>
        )}
        <CardContent className={title ? undefined : "pt-5 sm:pt-6"}>{children}</CardContent>
      </Card>
    </div>
  );
}
