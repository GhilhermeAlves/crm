"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from "react";
import { initKeycloak, loginKeycloak, logoutKeycloak, refreshAccessToken } from "@/lib/keycloak";
import { TokenManager } from "@/store/token-manager";
import type Keycloak from "keycloak-js";

type KeycloakContextType = {
  keycloak: Keycloak | null;
  initialized: boolean;
  authenticated: boolean;
  login: (redirectPath?: string) => Promise<void>;
  logout: () => Promise<void>;
  token: string | null;
};

const KeycloakContext = createContext<KeycloakContextType | undefined>(undefined);

export function KeycloakProvider({ children }: { children: ReactNode }) {
  const [keycloak, setKeycloak] = useState<Keycloak | null>(null);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    let disposed = false;

    initKeycloak().then((kc) => {
      if (disposed) return;
      setKeycloak(kc);
      setInitialized(true);

      if (kc.authenticated && kc.token) {
        // Persiste a sessão (único escritor: TokenManager.setTokens).
        TokenManager.setTokens(kc.token, kc.refreshToken || null);
      } else {
        // Sem SSO ativo: remove tokens/flag de sessão obsoletos de visitas antigas.
        TokenManager.clearTokens();
      }

      // Renova automaticamente quando o token expira (sempre via keycloak-js).
      kc.onTokenExpired = () => {
        refreshAccessToken(30).then((ok) => {
          if (!ok) {
            TokenManager.clearTokens();
          }
        });
      };
    });

    return () => {
      disposed = true;
    };
  }, []);

  const login = useCallback(async (redirectPath?: string) => {
    await loginKeycloak(redirectPath);
  }, []);

  const logout = useCallback(async () => {
    TokenManager.clearTokens();
    await logoutKeycloak();
  }, []);

  return (
    <KeycloakContext.Provider
      value={{
        keycloak,
        initialized,
        authenticated: keycloak?.authenticated ?? false,
        login,
        logout,
        token: keycloak?.token ?? null,
      }}
    >
      {children}
    </KeycloakContext.Provider>
  );
}

export function useKeycloak() {
  const context = useContext(KeycloakContext);
  if (!context) {
    throw new Error("useKeycloak must be used within KeycloakProvider");
  }
  return context;
}
