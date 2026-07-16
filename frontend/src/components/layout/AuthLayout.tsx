import { type ReactNode } from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

type AuthLayoutProps = {
  title?: string;
  description?: string;
  children: ReactNode;
};

export function AuthLayout({ title, description, children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-md">
        {title && (
          <CardHeader className="space-y-1 text-center">
            <h1 className="text-2xl font-bold">{title}</h1>
            {description && (
              <p className="text-sm text-muted-foreground">{description}</p>
            )}
          </CardHeader>
        )}
        <CardContent>{children}</CardContent>
      </Card>
    </div>
  );
}
