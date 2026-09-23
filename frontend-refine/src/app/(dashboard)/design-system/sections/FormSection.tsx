import { Search } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";

export function FormSection() {
  return (
    <section className="space-y-8" aria-labelledby="design-system-form-title">
      <h2 id="design-system-form-title" className="text-xl font-bold tracking-tight">
        Formulários & Entradas
      </h2>

      {/* Controles de Formulário */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Controles de Formulário e Entradas</CardTitle>
          <CardDescription>
            Inputs, switches e caixas de texto com validações integradas.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <div className="space-y-2">
            <label className="text-xs font-semibold">Campo de Texto com Ícone</label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input placeholder="Pesquisar por nome ou e-mail..." className="pl-9" />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-semibold">Campo com Estado de Erro</label>
            <Input
              defaultValue="contato-invalido"
              className="border-rose-500 focus-visible:ring-rose-500"
            />
            <p className="text-[11px] text-rose-600 dark:text-rose-400">
              Insira um endereço de e-mail corporativo válido.
            </p>
          </div>

          <div className="space-y-2 md:col-span-2">
            <label className="text-xs font-semibold">Área de Texto (Notas & Observações)</label>
            <Textarea
              placeholder="Adicione notas da reunião de alinhamento com o cliente..."
              rows={3}
            />
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox id="demo-check" defaultChecked />
            <label htmlFor="demo-check" className="cursor-pointer text-xs font-medium">
              Disparar sequência de follow-up via WhatsApp automaticamente
            </label>
          </div>

          <div className="flex items-center space-x-2">
            <Switch id="demo-switch" defaultChecked />
            <label htmlFor="demo-switch" className="cursor-pointer text-xs font-medium">
              Ativar co-piloto IA para qualificação do Lead
            </label>
          </div>
        </CardContent>
      </Card>
    </section>
  );
}
