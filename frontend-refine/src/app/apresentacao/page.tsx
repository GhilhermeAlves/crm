import type { Metadata } from "next";
import Link from "next/link";
import {
  BarChart3,
  Bot,
  CalendarCheck,
  Check,
  ChevronsRight,
  ClipboardList,
  KanbanSquare,
  MessageCircle,
  ShieldCheck,
  Sparkles,
  Users,
  X,
} from "lucide-react";

import { HorizonBackground } from "@/components/brand/HorizonBackground";
import { ROUTES } from "@/lib/constants";

export const metadata: Metadata = {
  title: "CRM Omnichannel — atendimento, agenda e vendas com IA",
  description:
    "WhatsApp, pacientes, funil de vendas, agenda e financeiro num só lugar, com um agente de IA que atende e marca horários por você.",
};

/** Prints do sistema ficam em public/marketing (capturados do CRM com dados de demonstração). */
const SHOT = (name: string) => `/marketing/${name}.jpg`;
/** Prints já capturados; recursos sem print mostram um painel com o ícone. */
const AVAILABLE_SHOTS = new Set(["dashboard", "pipeline", "agenda", "patients", "reports"]);

const PILLARS = [
  {
    icon: MessageCircle,
    title: "Atenda em um só lugar",
    text: "Todas as conversas do WhatsApp numa caixa de entrada compartilhada pela equipe.",
  },
  {
    icon: Bot,
    title: "IA que trabalha por você",
    text: "Um agente responde dúvidas, qualifica contatos e marca horários direto na agenda.",
  },
  {
    icon: BarChart3,
    title: "Decida com números",
    text: "Funil, faturamento e agenda em relatórios claros, atualizados em tempo real.",
  },
];

const PAINS = [
  "Mensagens espalhadas em celulares diferentes",
  "Horários marcados no papel e faltas sem aviso",
  "Orçamentos que ninguém acompanha até fechar",
  "Nenhuma visão real de quanto a clínica fatura",
];

const STEPS = [
  "Crie a conta da sua empresa",
  "Conecte o WhatsApp lendo um QR Code",
  "Convide sua equipe e defina as permissões",
  "Configure agenda, serviços e o agente de IA",
  "Atenda, agende e venda — tudo acompanhado",
];

const FEATURES = [
  {
    icon: MessageCircle,
    tag: "Caixa de entrada",
    title: "Todo o WhatsApp da equipe numa tela",
    text: "Conversas organizadas por responsável e status, histórico completo do contato e respostas rápidas. Ninguém fica sem resposta.",
    shot: "inbox",
  },
  {
    icon: KanbanSquare,
    tag: "Funil de vendas",
    title: "Acompanhe cada oportunidade até o fechamento",
    text: "Arraste negócios entre etapas, veja valores por fase e saiba exatamente onde as vendas estão travando.",
    shot: "pipeline",
  },
  {
    icon: CalendarCheck,
    tag: "Agenda",
    title: "Agenda inteligente para toda a equipe",
    text: "Horários por profissional, confirmações e link de agendamento. O agente de IA marca consultas sozinho.",
    shot: "agenda",
  },
  {
    icon: ClipboardList,
    tag: "Pacientes",
    title: "Cadastro, anamnese e histórico completos",
    text: "O paciente preenche o cadastro e a anamnese pelo link, com assinatura, ou no tablet da recepção.",
    shot: "patients",
  },
  {
    icon: BarChart3,
    tag: "Relatórios",
    title: "Faturamento e desempenho sem planilha",
    text: "Receita, fluxo de caixa, agenda e funil em painéis prontos, com exportação quando precisar.",
    shot: "reports",
  },
];

const BENEFITS = [
  "WhatsApp conectado por QR Code",
  "Agente de IA para atendimento e agendamento",
  "Funil de vendas e oportunidades",
  "Agenda por profissional",
  "Cadastro e anamnese do paciente por link",
  "Controle financeiro e relatórios",
  "Equipe com papéis e permissões",
  "Dados isolados e seguros por empresa",
];

const FAQ = [
  {
    q: "Preciso instalar alguma coisa?",
    a: "Não. O CRM funciona no navegador, no computador, tablet ou celular.",
  },
  {
    q: "Como o WhatsApp é conectado?",
    a: "Você lê um QR Code nas configurações, como no WhatsApp Web, e as conversas passam a chegar na caixa de entrada.",
  },
  {
    q: "O agente de IA responde sozinho?",
    a: "Sim, dentro das regras que você configurar. Ele tira dúvidas e marca horários, e a equipe pode assumir a conversa a qualquer momento.",
  },
  {
    q: "Posso controlar o que cada pessoa vê?",
    a: "Sim. Cada usuário tem um papel com permissões por módulo, e os dados de cada empresa ficam isolados.",
  },
  {
    q: "Serve para qual tipo de negócio?",
    a: "Foi pensado para clínicas e consultórios, mas atende qualquer empresa que vende e agenda pelo WhatsApp.",
  },
];

function Screenshot({ name, alt, priority }: { name: string; alt: string; priority?: boolean }) {
  return (
    <div className="overflow-hidden rounded-xl border border-white/15 bg-zinc-950 shadow-[0_30px_120px_-20px_rgba(255,255,255,.18)]">
      <div className="flex items-center gap-1.5 border-b border-white/10 px-4 py-2.5">
        <span className="h-2.5 w-2.5 rounded-full bg-white/20" />
        <span className="h-2.5 w-2.5 rounded-full bg-white/20" />
        <span className="h-2.5 w-2.5 rounded-full bg-white/20" />
      </div>
      {/* eslint-disable-next-line @next/next/no-img-element -- print estático, sem otimização necessária */}
      <img
        src={SHOT(name)}
        alt={alt}
        loading={priority ? "eager" : "lazy"}
        decoding="async"
        className="block w-full"
      />
    </div>
  );
}

function CtaButton({ children }: { children: React.ReactNode }) {
  return (
    <Link
      href={ROUTES.LOGIN}
      className="inline-flex items-center gap-2 rounded-full bg-white px-7 py-3.5 text-sm font-semibold uppercase text-black transition-opacity hover:opacity-90"
    >
      {children} <ChevronsRight className="h-4 w-4" aria-hidden="true" />
    </Link>
  );
}

function SectionTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mx-auto max-w-2xl text-center">
      <h2 className="text-balance text-3xl font-medium tracking-tight sm:text-4xl">{title}</h2>
      {subtitle && <p className="mt-4 text-white/65">{subtitle}</p>}
    </div>
  );
}

export default function PresentationPage() {
  return (
    // html/body têm overflow hidden (shell do dashboard): a página rola por conta própria.
    <main className="h-dvh overflow-y-auto scroll-smooth bg-black text-white">
      {/* HERO */}
      <section className="relative overflow-hidden">
        <HorizonBackground className="absolute inset-0" />

        <header className="relative z-10 mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
          <span className="text-sm font-semibold tracking-[0.2em]">CRM OMNICHANNEL</span>
          <nav className="hidden gap-8 text-sm text-white/70 md:flex">
            <a href="#recursos" className="hover:text-white">
              Recursos
            </a>
            <a href="#como-funciona" className="hover:text-white">
              Como funciona
            </a>
            <a href="#duvidas" className="hover:text-white">
              Dúvidas
            </a>
          </nav>
          <Link
            href={ROUTES.LOGIN}
            className="rounded-full border border-white/40 px-4 py-1.5 text-xs font-semibold uppercase tracking-wide transition-colors hover:bg-white hover:text-black"
          >
            Entrar
          </Link>
        </header>

        <div className="relative z-10 mx-auto max-w-6xl px-6 pb-16 pt-12 text-center sm:pt-20">
          <div className="flex flex-wrap justify-center gap-2">
            {[
              { icon: MessageCircle, label: "WhatsApp integrado" },
              { icon: Sparkles, label: "Agente de IA" },
              { icon: ShieldCheck, label: "Dados seguros" },
            ].map(({ icon: Icon, label }) => (
              <span
                key={label}
                className="inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-white/5 px-3 py-1 text-xs text-white/80 backdrop-blur"
              >
                <Icon className="h-3.5 w-3.5" aria-hidden="true" /> {label}
              </span>
            ))}
          </div>
          <h1 className="mx-auto mt-8 max-w-4xl text-balance text-4xl font-medium leading-tight tracking-tight sm:text-6xl">
            O CRM que atende, agenda e vende
            <span className="block bg-gradient-to-b from-white via-white/85 to-white/40 bg-clip-text text-transparent">
              enquanto você cuida do negócio
            </span>
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-base text-white/75 sm:text-lg">
            WhatsApp, pacientes, funil de vendas, agenda e financeiro conectados — com um agente de
            IA que responde e marca horários por você.
          </p>
          <div className="mt-10 flex flex-wrap items-center justify-center gap-3">
            <CtaButton>Começar agora</CtaButton>
            <a
              href="#como-funciona"
              className="rounded-full border border-white/30 px-7 py-3.5 text-sm font-semibold uppercase transition-colors hover:bg-white/10"
            >
              Saiba como funciona
            </a>
          </div>

          <div className="mx-auto mt-16 max-w-5xl">
            <Screenshot name="dashboard" alt="Painel inicial do CRM" priority />
          </div>
        </div>
      </section>

      {/* PILARES */}
      <section className="mx-auto max-w-6xl px-6 py-24">
        <SectionTitle
          title="Sua empresa organizada do primeiro contato ao pós-venda"
          subtitle="Se você vende e agenda pelo WhatsApp, o CRM foi feito para você."
        />
        <div className="mt-14 grid gap-5 md:grid-cols-3">
          {PILLARS.map(({ icon: Icon, title, text }) => (
            <div key={title} className="rounded-2xl border border-white/10 bg-white/[0.03] p-7">
              <Icon className="h-6 w-6 text-white/80" aria-hidden="true" />
              <h3 className="mt-5 text-lg font-medium">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-white/60">{text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* PROBLEMA */}
      <section className="border-y border-white/10 bg-white/[0.02]">
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-6 py-24 md:grid-cols-2">
          <div>
            <h2 className="text-3xl font-medium tracking-tight sm:text-4xl">
              Por que um CRM omnichannel?
            </h2>
            <p className="mt-4 text-white/65">
              A maioria das clínicas e pequenas empresas trabalha muito, mas no improviso:
            </p>
            <ul className="mt-6 space-y-3">
              {PAINS.map((pain) => (
                <li key={pain} className="flex items-start gap-3 text-white/80">
                  <X className="mt-0.5 h-5 w-5 shrink-0 text-red-400/80" aria-hidden="true" />
                  {pain}
                </li>
              ))}
            </ul>
          </div>
          <div className="rounded-2xl border border-white/10 bg-black p-8">
            <p className="text-xl font-medium">O CRM resolve isso.</p>
            <p className="mt-4 leading-relaxed text-white/65">
              Estrutura, visibilidade e automação num único sistema. Sua equipe deixa de trabalhar
              no improviso e passa a atender com processo, histórico e metas — sem perder o jeito
              próximo de falar com o cliente.
            </p>
          </div>
        </div>
      </section>

      {/* COMO FUNCIONA */}
      <section id="como-funciona" className="mx-auto max-w-6xl scroll-mt-8 px-6 py-24">
        <SectionTitle
          title="Como funciona"
          subtitle="Do cadastro ao primeiro atendimento em minutos."
        />
        <ol className="mt-14 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {STEPS.map((step, i) => (
            <li key={step} className="rounded-2xl border border-white/10 p-6">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-white text-sm font-semibold text-black">
                {i + 1}
              </span>
              <p className="mt-4 text-sm text-white/80">{step}</p>
            </li>
          ))}
        </ol>
        <div className="mt-12 text-center">
          <CtaButton>Criar minha conta</CtaButton>
        </div>
      </section>

      {/* RECURSOS COM PRINTS */}
      <section id="recursos" className="scroll-mt-8 border-t border-white/10">
        <div className="mx-auto max-w-6xl px-6 py-24">
          <SectionTitle
            title="Tudo o que sua equipe usa no dia a dia"
            subtitle="Veja o sistema por dentro."
          />
          <div className="mt-20 space-y-28">
            {FEATURES.map(({ icon: Icon, tag, title, text, shot }, i) => (
              <div key={shot} className="grid items-center gap-10 lg:grid-cols-5">
                <div className={`lg:col-span-2 ${i % 2 === 1 ? "lg:order-2" : ""}`}>
                  <span className="inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-white/60">
                    <Icon className="h-4 w-4" aria-hidden="true" /> {tag}
                  </span>
                  <h3 className="mt-3 text-2xl font-medium tracking-tight sm:text-3xl">{title}</h3>
                  <p className="mt-4 leading-relaxed text-white/65">{text}</p>
                </div>
                <div className={`lg:col-span-3 ${i % 2 === 1 ? "lg:order-1" : ""}`}>
                  {AVAILABLE_SHOTS.has(shot) ? (
                    <Screenshot name={shot} alt={`${tag} do CRM`} />
                  ) : (
                    <div className="flex aspect-[16/9] items-center justify-center rounded-xl border border-white/10 bg-[radial-gradient(circle_at_center,rgba(255,255,255,.08),transparent_70%)]">
                      <Icon className="h-20 w-20 text-white/25" aria-hidden="true" />
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* BENEFÍCIOS */}
      <section className="border-y border-white/10 bg-white/[0.02]">
        <div className="mx-auto max-w-6xl px-6 py-24">
          <SectionTitle title="Tudo incluso para você crescer" />
          <ul className="mx-auto mt-12 grid max-w-4xl gap-x-10 gap-y-4 sm:grid-cols-2">
            {BENEFITS.map((b) => (
              <li key={b} className="flex items-center gap-3 text-white/85">
                <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-white/10">
                  <Check className="h-3.5 w-3.5" aria-hidden="true" />
                </span>
                {b}
              </li>
            ))}
          </ul>
        </div>
      </section>

      {/* FAQ */}
      <section id="duvidas" className="mx-auto max-w-3xl scroll-mt-8 px-6 py-24">
        <SectionTitle title="Dúvidas frequentes" />
        <div className="mt-12 divide-y divide-white/10 border-y border-white/10">
          {FAQ.map(({ q, a }) => (
            <details key={q} className="group py-5">
              <summary className="flex cursor-pointer list-none items-center justify-between font-medium">
                {q}
                <span className="text-xl text-white/50 transition-transform group-open:rotate-45">
                  +
                </span>
              </summary>
              <p className="mt-3 text-white/65">{a}</p>
            </details>
          ))}
        </div>
      </section>

      {/* CTA FINAL */}
      <section className="relative overflow-hidden border-t border-white/10">
        <HorizonBackground className="absolute inset-0" />
        <div className="relative z-10 mx-auto max-w-3xl px-6 py-32 text-center">
          <Users className="mx-auto h-8 w-8 text-white/70" aria-hidden="true" />
          <h2 className="mt-6 text-balance text-3xl font-medium tracking-tight sm:text-5xl">
            Pronto para atender de forma profissional?
          </h2>
          <p className="mt-5 text-white/70">
            Organize o WhatsApp, a agenda e as vendas da sua equipe hoje mesmo.
          </p>
          <div className="mt-10">
            <CtaButton>Começar agora</CtaButton>
          </div>
        </div>
      </section>

      <footer className="border-t border-white/10 py-8 text-center text-xs text-white/40">
        © {new Date().getFullYear()} CRM Omnichannel
      </footer>
    </main>
  );
}
