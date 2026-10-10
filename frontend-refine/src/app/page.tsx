import Link from "next/link";
import { ChevronsRight } from "lucide-react";

import { VortexBackground } from "@/components/brand/VortexBackground";
import { ROUTES } from "@/lib/constants";

export default function Home() {
  return (
    <main className="relative flex min-h-dvh flex-col overflow-hidden bg-black text-white">
      <VortexBackground className="absolute inset-0 h-full w-full" />

      {/* moldura de linhas finas */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-3 border border-white/10 sm:inset-6"
      />

      <header className="relative z-10 mx-3 flex items-center justify-between border-b border-white/10 px-5 py-4 sm:mx-6 sm:px-8">
        <span className="text-sm font-semibold tracking-[0.2em]">CRM OMNICHANNEL</span>
        <Link
          href={ROUTES.LOGIN}
          className="inline-flex items-center gap-1 rounded-full border border-white/40 px-4 py-1.5 text-xs font-semibold uppercase tracking-wide transition-colors hover:bg-white hover:text-black"
        >
          Entrar <ChevronsRight className="h-3.5 w-3.5" aria-hidden="true" />
        </Link>
      </header>

      <section className="relative z-10 flex flex-1 flex-col items-center justify-center px-6 pb-24 text-center">
        <h1 className="max-w-4xl text-balance text-4xl font-medium leading-tight tracking-tight sm:text-6xl">
          Atendimento, agenda e vendas
          <span className="block bg-gradient-to-b from-white via-white/85 to-white/40 bg-clip-text text-transparent">
            num só lugar, com IA
          </span>
        </h1>
        <p className="mt-6 max-w-xl text-base text-white/75 sm:text-lg">
          WhatsApp, contatos, oportunidades e agendamentos conectados — com um agente que atende e
          marca horários por você.
        </p>
        <div className="mt-10 flex flex-wrap items-center justify-center gap-3">
          <Link
            href={ROUTES.LOGIN}
            className="inline-flex items-center gap-2 rounded-full bg-white px-6 py-3 text-sm font-semibold uppercase text-black transition-opacity hover:opacity-90"
          >
            Entrar no CRM <ChevronsRight className="h-4 w-4" aria-hidden="true" />
          </Link>
        </div>
      </section>
    </main>
  );
}
