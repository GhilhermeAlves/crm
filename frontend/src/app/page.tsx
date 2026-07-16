import Link from "next/link";

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <h1 className="mb-4 text-4xl font-bold">CRM SaaS Omnichannel</h1>
      <p className="mb-8 text-lg text-muted-foreground">
        Sistema CRM Omnichannel para gestão de leads, contatos e comunicação
      </p>
      <div className="flex gap-4">
        <Link
          href="/login"
          className="rounded-md bg-primary px-6 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Entrar
        </Link>
        <Link
          href="/docs/swagger"
          className="rounded-md border border-input bg-background px-6 py-3 text-sm font-medium hover:bg-accent hover:text-accent-foreground"
        >
          API Docs
        </Link>
      </div>
    </main>
  );
}
