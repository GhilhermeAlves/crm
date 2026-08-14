"use client";

import Link from "next/link";
import type { Contact } from "../types/contact.types";

const formatDate = (iso: string): string =>
  new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" }).format(new Date(iso));

const initials = (c: Contact): string => {
  const first = c.firstName?.[0] ?? "";
  const last = c.lastName?.[0] ?? "";
  return (first + last).toUpperCase() || "?";
};

type Props = {
  contacts: Contact[];
  isLoading?: boolean;
};

export function ContactTable({ contacts, isLoading }: Props) {
  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="flex items-center gap-3 rounded-lg border p-3">
            <div className="h-10 w-10 animate-pulse rounded-full bg-muted" />
            <div className="flex-1 space-y-2">
              <div className="h-3 w-1/3 animate-pulse rounded bg-muted" />
              <div className="h-3 w-1/4 animate-pulse rounded bg-muted" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (contacts.length === 0) {
    return (
      <p className="py-10 text-center text-sm text-muted-foreground">
        Nenhum contato cadastrado ainda.
      </p>
    );
  }

  return (
    <ul className="divide-y divide-border rounded-lg border">
      {contacts.map((c) => (
        <li key={c.id}>
          <Link
            href={`/contacts/${c.id}`}
            className="flex items-center gap-3 p-3 transition-colors hover:bg-accent"
          >
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
              {initials(c)}
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">
                {c.firstName} {c.lastName ?? ""}
              </p>
              <p className="truncate text-xs text-muted-foreground">
                {(c.email ?? c.phone ?? "—")}{" "}
                {c.createdAt ? `· desde ${formatDate(c.createdAt)}` : ""}
              </p>
            </div>
          </Link>
        </li>
      ))}
    </ul>
  );
}