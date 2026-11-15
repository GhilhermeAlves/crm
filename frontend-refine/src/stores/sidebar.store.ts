"use client";

import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

/**
 * Estado de UI do menu lateral (padrão `stores/` do Portal Praia).
 * Só `collapsed` é persistido; `mobileOpen` é efêmero.
 * `skipHydration`: o servidor renderiza com o valor padrão e o cliente
 * reidrata depois de montar (`rehydrateSidebarStore`), sem hydration mismatch.
 */
type SidebarState = {
  collapsed: boolean;
  mobileOpen: boolean;
  toggle: () => void;
  setMobileOpen: (open: boolean) => void;
};

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set) => ({
      collapsed: false,
      mobileOpen: false,
      toggle: () => set((s) => ({ collapsed: !s.collapsed })),
      setMobileOpen: (open) => set({ mobileOpen: open }),
    }),
    {
      name: "sidebar-ui",
      storage: createJSONStorage(() => localStorage),
      partialize: (s) => ({ collapsed: s.collapsed }),
      skipHydration: true,
    },
  ),
);

export const useSidebar = useSidebarStore;

/** Chave usada antes da store (valor JSON puro: `true`/`false`). */
const LEGACY_KEY = "sidebar-collapsed";

export function rehydrateSidebarStore() {
  try {
    const legacy = window.localStorage.getItem(LEGACY_KEY);
    if (legacy !== null) {
      useSidebarStore.setState({ collapsed: JSON.parse(legacy) === true });
      window.localStorage.removeItem(LEGACY_KEY);
    }
  } catch {
    // localStorage indisponível: segue com o padrão
  }
  void useSidebarStore.persist.rehydrate();
}
