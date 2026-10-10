import { Landmark } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";

export default function FinanceReconciliationPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Conciliação</h1>
        <p className="text-sm text-muted-foreground">
          Em breve você poderá conciliar os recebimentos com o extrato bancário.
        </p>
      </div>

      <Card>
        <CardContent className="flex items-center justify-center py-16">
          <Landmark className="h-12 w-12 text-muted-foreground" />
        </CardContent>
      </Card>
    </div>
  );
}
