"use client";

import * as React from "react";

/** Preferências de colunas de um grid, guardadas no navegador do usuário. */
export interface ColumnPreferences {
  order: string[];
  hidden: string[];
  widths: Record<string, number>;
}

export const MIN_COLUMN_WIDTH = 60;

const STORAGE_PREFIX = "crm.grid.";

const EMPTY: ColumnPreferences = { order: [], hidden: [], widths: {} };

function readStorage(tableId: string): ColumnPreferences {
  try {
    const raw = window.localStorage.getItem(STORAGE_PREFIX + tableId);
    if (!raw) return EMPTY;
    const parsed = JSON.parse(raw) as Partial<ColumnPreferences>;
    return {
      order: Array.isArray(parsed.order) ? parsed.order.filter((v) => typeof v === "string") : [],
      hidden: Array.isArray(parsed.hidden)
        ? parsed.hidden.filter((v) => typeof v === "string")
        : [],
      widths:
        parsed.widths && typeof parsed.widths === "object"
          ? Object.fromEntries(
              Object.entries(parsed.widths).filter(
                ([, v]) => typeof v === "number" && Number.isFinite(v),
              ),
            )
          : {},
    };
  } catch {
    return EMPTY;
  }
}

function writeStorage(tableId: string, prefs: ColumnPreferences) {
  try {
    const isEmpty =
      prefs.order.length === 0 &&
      prefs.hidden.length === 0 &&
      Object.keys(prefs.widths).length === 0;
    if (isEmpty) {
      window.localStorage.removeItem(STORAGE_PREFIX + tableId);
    } else {
      window.localStorage.setItem(STORAGE_PREFIX + tableId, JSON.stringify(prefs));
    }
  } catch {
    // Navegador sem storage (aba anônima, bloqueio): segue só em memória.
  }
}

/**
 * Ordem, visibilidade e largura das colunas de um grid.
 * `defaultIds` é a ordem original das colunas; ids novos/removidos no código
 * são reconciliados com o que estava salvo.
 */
export function useColumnPreferences(tableId: string | undefined, defaultIds: string[]) {
  const [prefs, setPrefs] = React.useState<ColumnPreferences>(EMPTY);

  React.useEffect(() => {
    if (tableId) setPrefs(readStorage(tableId));
  }, [tableId]);

  const update = React.useCallback(
    (fn: (prev: ColumnPreferences) => ColumnPreferences) => {
      setPrefs((prev) => {
        const next = fn(prev);
        if (tableId) writeStorage(tableId, next);
        return next;
      });
    },
    [tableId],
  );

  const defaultKey = defaultIds.join("|");
  // Ordem fixa: a definida no código (ações por último).
  const order = defaultIds;

  const hidden = React.useMemo(
    () => new Set(prefs.hidden.filter((id) => defaultIds.includes(id))),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [prefs.hidden, defaultKey],
  );

  const isCustomized = hidden.size > 0 || Object.keys(prefs.widths).length > 0;

  const toggle = (id: string) =>
    update((prev) => {
      const set = new Set(prev.hidden);
      if (set.has(id)) {
        set.delete(id);
      } else {
        // Nunca esconder a última coluna visível.
        const visible = defaultIds.filter((c) => !set.has(c));
        if (visible.length <= 1) return prev;
        set.add(id);
      }
      return { ...prev, hidden: Array.from(set) };
    });

  const setWidth = (id: string, width: number) =>
    update((prev) => ({
      ...prev,
      widths: { ...prev.widths, [id]: Math.max(MIN_COLUMN_WIDTH, Math.round(width)) },
    }));

  const reset = () => update(() => EMPTY);

  return { order, hidden, widths: prefs.widths, isCustomized, toggle, setWidth, reset };
}
