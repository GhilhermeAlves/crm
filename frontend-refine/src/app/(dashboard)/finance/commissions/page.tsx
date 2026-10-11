import { Percent } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";

export default function FinanceCommissionsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Comissões</h1>
        <p className="text-sm text-muted-foreground">
          Em breve você poderá calcular as comissões dos profissionais.
        </p>
      </div>

      <Card>
        <CardContent className="flex items-center justify-center py-16">
          <Percent className="h-12 w-12 text-muted-foreground" />
        </CardContent>
      </Card>
    </div>
  );
}
