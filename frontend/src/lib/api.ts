import axios from "axios";
import { TokenManager } from "@/store/token-manager";
import { refreshAccessToken } from "@/lib/keycloak";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1",
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(async (config) => {
  if (TokenManager.getAccessToken()) {
    // Renova proativamente se o token expirar em breve (minValidity) —
    // evita enviar token expirado; o keycloak-js deduplica refreshes.
    await refreshAccessToken(30);
  }
  const token = TokenManager.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status !== 401) {
      return Promise.reject(error);
    }

    if (originalRequest._retry) {
      // Já renovamos o token e o backend voltou a rejeitar: sessão inválida.
      // Sem novo refresh nem retry — evita loop de 401.
      TokenManager.clearTokens();
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
      return Promise.reject(error);
    }

    if (TokenManager.getAccessToken()) {
      originalRequest._retry = true;
      const previousToken = TokenManager.getAccessToken();
      const refreshed = await refreshAccessToken(30);
      const newToken = TokenManager.getAccessToken();

      if (refreshed && newToken && newToken !== previousToken) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      }

      // Sessão do Keycloak inválida/expirada: limpa e volta ao login uma
      // única vez (sem loop de retry, graças ao guard `_retry`).
      TokenManager.clearTokens();
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
      return Promise.reject(error);
    }

    TokenManager.clearTokens();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

export default api;
