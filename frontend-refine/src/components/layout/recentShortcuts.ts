"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { NAVIGATION, type NavItem } from "./navigation";
import { ROUTES } from "@/lib/constants";

const STORAGE_KEY = "home.recentShortcuts";
const MAX_STORED = 12;

const ITEMS: NavItem[] = NAVIGATION.flatMap((group) => group.items).filter(
  (item) => item.href !== ROUTES.CRM,
);

/** Item do menu da página atual (o href mais longo que é prefixo do caminho). */
export function navItemForPath(pathname: string): NavItem | undefined {
  return ITEMS.filter(
    (item) => pathname === item.href || pathname.startsWith(`${item.href}/`),
  ).sort((a, b) => b.href.length - a.href.length)[0];
}

/** Hrefs dos módulos abertos por último (mais recente primeiro). Preferência só deste navegador. */
export function readRecentShortcuts(): string[] {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === "string") : [];
  } catch {
    return [];
  }
}

function recordVisit(pathname: string) {
  const item = navItemForPath(pathname);
  if (!item) return;
  try {
    const next = [item.href, ...readRecentShortcuts().filter((h) => h !== item.href)].slice(
      0,
      MAX_STORED,
    );
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    // sem armazenamento: o início usa os primeiros itens do menu
  }
}

/** Guarda o módulo aberto, para o "Acesso rápido" da tela inicial. */
export function useRecordRecentShortcut() {
  const pathname = usePathname();
  useEffect(() => {
    if (pathname) recordVisit(pathname);
  }, [pathname]);
}
