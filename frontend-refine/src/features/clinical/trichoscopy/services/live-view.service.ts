import api from "@/lib/api";

/** Chaves da tela ao vivo: a de envio fica no computador, a de visualização vai no QR. */
export type LiveViewTokens = { publishToken: string; viewToken: string };

/** Envio e leitura dos quadros vão direto ao backend (rota pública, fora do limite do gateway). */
const LIVE_BASE = "/api/v1/public/forms/live";

export type LiveFrameResult =
  { status: "frame"; blob: Blob; seq: number } | { status: "unchanged" } | { status: "ended" };

export const LiveViewService = {
  open: async (companyId: string): Promise<LiveViewTokens> => {
    const response = await api.post<LiveViewTokens>(`/companies/${companyId}/trichoscopy/live`);
    return response.data;
  },

  /** false quando a sessão acabou (expirou ou foi encerrada). */
  push: async (publishToken: string, jpeg: Blob): Promise<boolean> => {
    const response = await fetch(`${LIVE_BASE}/push/${publishToken}`, {
      method: "PUT",
      headers: { "Content-Type": "image/jpeg" },
      body: jpeg,
    });
    return response.status !== 410 && response.status !== 404;
  },

  close: (publishToken: string): void => {
    void fetch(`${LIVE_BASE}/push/${publishToken}`, { method: "DELETE", keepalive: true }).catch(
      () => undefined,
    );
  },

  frame: async (viewToken: string, after: number): Promise<LiveFrameResult> => {
    const response = await fetch(`${LIVE_BASE}/view/${viewToken}?after=${after}`, {
      cache: "no-store",
    });
    if (response.status === 410 || response.status === 404) return { status: "ended" };
    if (response.status === 204 || !response.ok) return { status: "unchanged" };
    const seq = Number(response.headers.get("X-Frame-Seq") ?? after + 1);
    return { status: "frame", blob: await response.blob(), seq };
  },
};
