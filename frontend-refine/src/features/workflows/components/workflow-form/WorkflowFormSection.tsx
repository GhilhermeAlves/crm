import type { ReactNode } from "react";

import { Card, CardContent } from "@/components/ui/card";

type WorkflowFormSectionProps = {
  children: ReactNode;
};

export function WorkflowFormSection({ children }: WorkflowFormSectionProps) {
  return (
    <Card>
      <CardContent className="space-y-4 pt-6">{children}</CardContent>
    </Card>
  );
}
