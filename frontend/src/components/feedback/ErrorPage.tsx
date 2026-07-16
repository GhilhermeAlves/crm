"use client";

import Link from "next/link";
import { AlertTriangle, Home, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";

type ErrorPageProps = {
  code: string;
  title: string;
  description: string;
};

export function ErrorPage({ code, title, description }: ErrorPageProps) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-6 text-center">
      <div className="mb-6 rounded-full bg-muted p-4">
        <AlertTriangle className="h-12 w-12 text-muted-foreground" />
      </div>
      <h1 className="mb-2 text-7xl font-bold tracking-tighter">{code}</h1>
      <h2 className="mb-2 text-2xl font-semibold">{title}</h2>
      <p className="mb-8 max-w-md text-muted-foreground">{description}</p>
      <div className="flex gap-3">
        <Button variant="outline" asChild>
          <Link href="/dashboard">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Voltar
          </Link>
        </Button>
        <Button asChild>
          <Link href="/dashboard">
            <Home className="mr-2 h-4 w-4" />
            Dashboard
          </Link>
        </Button>
      </div>
    </div>
  );
}
