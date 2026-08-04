import axios, { type AxiosRequestConfig } from "axios";
import { refreshGatewaySession } from "@/lib/gateway-auth";
import { isPublicPathname } from "@/lib/middleware-auth";

/**
 * Client HTTP da aplicação (Sprint 6.4). Mesma origem via BFF relay:
 * o auth-service injeta o access token da sessão no backend — o browser nunca
 * envia `Authorization`. Autenticação = cookie HttpOnly `crm_session`.
 */
const api = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    // 403 (CRM_ACCESS_DENIED/CSRF) NUNCA vira login — é decisão de autorização.
    if (error.response?.status !== 401) {
      return Promise.reject(error);
    }

    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };
    if (!originalRequest) {
      return Promise.reject(error);
    }

    if (!originalRequest._retry) {
      originalRequest._retry = true;
      const refreshed = await refreshGatewaySession();
      if (refreshed) {
        return api(originalRequest);
      }
    }

    // Sessão realmente inválida (refresh falhou ou o backend rejeitou de novo):
    // volta ao login uma única vez — o guard `_retry` garante que não há loop.
    if (typeof window !== "undefined" && !isPublicPathname(window.location.pathname)) {
      window.location.assign("/login");
    }
    return Promise.reject(error);
  },
);

export default api;
