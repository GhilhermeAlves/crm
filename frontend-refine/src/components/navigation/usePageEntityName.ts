"use client";

import { useEffect, useSyncExternalStore } from "react";

/**
 * Nome legível do registro aberto (paciente, contato, usuário...), por id.
 * O endereço continua com o id; a aba do navegador e o breadcrumb mostram o nome.
 */
const names = new Map<string, string>();
const listeners = new Set<() => void>();
let version = 0;

function emit() {
  version++;
  listeners.forEach((l) => l());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Lê o nome registrado para um id (usado pelo breadcrumb). */
export function useEntityNames(): (id: string) => string | undefined {
  useSyncExternalStore(
    subscribe,
    () => version,
    () => version,
  );
  return (id) => names.get(id);
}

const APP_TITLE = "CRM SaaS Omnichannel";

/** Registra o nome do registro `id` e põe no título da aba enquanto a página estiver aberta. */
export function usePageEntityName(id: string | null | undefined, name: string | null | undefined) {
  const label = name?.trim();
  useEffect(() => {
    if (!id || !label) return;
    if (names.get(id) !== label) {
      names.set(id, label);
      emit();
    }
    const previous = document.title;
    document.title = `${label} · ${APP_TITLE}`;
    return () => {
      document.title = previous || APP_TITLE;
    };
  }, [id, label]);
}
