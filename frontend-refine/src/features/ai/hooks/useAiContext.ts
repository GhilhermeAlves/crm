import { usePathname } from "next/navigation";
import type { AiContextPayload, AiRecordType } from "../types/ai.types";

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function isUuid(value: string | null | undefined): value is string {
  return !!value && UUID_RE.test(value);
}

type RoutePattern = {
  screen: string;
  recordType: AiRecordType;
};

/** Padrões de rota com registro em foco resolvidos com segurança (AI-04 §9-11). */
const ROUTE_PATTERNS: Record<string, RoutePattern> = {
  customers: { screen: "customer360", recordType: "CUSTOMER" },
  contacts: { screen: "contact", recordType: "CONTACT" },
  opportunities: { screen: "opportunity", recordType: "OPPORTUNITY" },
};

/**
 * Resolve o contexto de aplicação a partir da rota atual (AI-04 §8). Retorna
 * null quando a rota não permite determinar tela/registro com segurança
 * (rota inválida, id não-UUID, tela sem registro em foco).
 */
export function resolveAiContext(pathname: string): AiContextPayload | null {
  const parts = pathname.split("/").filter(Boolean);
  if (parts.length < 2) {
    return null;
  }

  const [segment, rawId] = parts;
  if (!isUuid(rawId)) {
    return null;
  }

  const pattern = ROUTE_PATTERNS[segment];
  if (!pattern) {
    return null;
  }

  return {
    screen: pattern.screen,
    route: `/${parts.join("/")}`,
    recordType: pattern.recordType,
    recordId: rawId,
  };
}

/** Contexto de aplicação da rota atual do App Router (null se indeterminável). */
export function useAiContext(): AiContextPayload | null {
  const pathname = usePathname();
  return resolveAiContext(pathname);
}
