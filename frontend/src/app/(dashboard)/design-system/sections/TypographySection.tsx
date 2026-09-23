import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export function TypographySection() {
  return (
    <section className="space-y-8" aria-labelledby="design-system-typography-title">
      <div className="space-y-1">
        <h2 id="design-system-typography-title" className="text-xl font-bold tracking-tight">
          Hierarquia Tipográfica & Espaçamento
        </h2>
        <p className="text-sm text-muted-foreground">
          A tipografia utiliza a família sans-serif Inter para interfaces densas e legíveis, com
          JetBrains Mono para códigos e identificadores.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Escala de Títulos e Textos</CardTitle>
          <CardDescription>Padrões de tamanho, peso e espaçamento vertical.</CardDescription>
        </CardHeader>
        <CardContent className="divide-y">
          <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
            <div className="font-mono text-xs text-muted-foreground">
              H1 · text-3xl md:text-4xl · Bold
            </div>
            <div className="text-3xl font-extrabold tracking-tight md:col-span-3 md:text-4xl">
              Dashboard de Oportunidades
            </div>
          </div>

          <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
            <div className="font-mono text-xs text-muted-foreground">H2 · text-2xl · Semibold</div>
            <div className="text-2xl font-bold tracking-tight md:col-span-3">
              Pipeline de Vendas & Contatos
            </div>
          </div>

          <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
            <div className="font-mono text-xs text-muted-foreground">
              H3 · text-lg md:text-xl · Semibold
            </div>
            <div className="text-lg font-semibold md:col-span-3 md:text-xl">
              Detalhes do Cliente e Próxima Ação Recomendada
            </div>
          </div>

          <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
            <div className="font-mono text-xs text-muted-foreground">Body · text-sm · Regular</div>
            <div className="text-sm text-foreground md:col-span-3">
              Gerencie todo o fluxo omnichannel com histórico unificado de WhatsApp, e-mails, notas
              comerciais e automações inteligentes alimentadas pela IA Leo.
            </div>
          </div>

          <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
            <div className="font-mono text-xs text-muted-foreground">
              Small / Muted · text-xs · Muted
            </div>
            <div className="text-xs text-muted-foreground md:col-span-3">
              Criado em 12 de setembro de 2026 às 14:32 por Guilherme Alves · ID: deal_9012a4b
            </div>
          </div>

          <div className="grid grid-cols-1 items-baseline gap-4 py-4 md:grid-cols-4">
            <div className="font-mono text-xs text-muted-foreground">
              Code / Mono · font-mono text-xs
            </div>
            <div className="inline-block rounded bg-muted/60 px-2 py-1 font-mono text-xs md:col-span-3">
              POST /api/v1/omnichannel/messages/send
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Espaçamentos */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Tokens de Espaçamento e Grid</CardTitle>
          <CardDescription>Escala modular de 4px para paddings, margens e gaps.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
            {[
              { label: "space-1", px: "4px", w: "w-1 h-8" },
              { label: "space-2", px: "8px", w: "w-2 h-8" },
              { label: "space-3", px: "12px", w: "w-3 h-8" },
              { label: "space-4", px: "16px", w: "w-4 h-8" },
              { label: "space-6", px: "24px", w: "w-6 h-8" },
              { label: "space-8", px: "32px", w: "w-8 h-8" },
              { label: "space-12", px: "48px", w: "w-12 h-8" },
            ].map((s) => (
              <div
                key={s.label}
                className="flex flex-col items-center rounded-lg border bg-muted/20 p-3"
              >
                <span className="font-mono text-xs font-medium">{s.label}</span>
                <span className="mb-3 text-[11px] text-muted-foreground">{s.px}</span>
                <div className={`rounded bg-crm-primary ${s.w}`} />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </section>
  );
}
