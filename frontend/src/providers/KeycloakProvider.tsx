"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from "react";
import { initKeycloak, loginKeycloak, logoutKeycloak, refreshKeycloakToken } from "@/lib/keycloak";
import { TokenManager } from "@/store/token-manager";
import type Keycloak from "keycloak-js";

type KeycloakContextType = {
  keycloak: Keycloak | null;
  initialized: boolean;
  authenticated: boolean;
  login: (redirectPath?: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<boolean>;
  token: string | null;
};

const KeycloakContext = createContext<KeycloakContextType | undefined>(undefined);

export function KeycloakProvider({ children }: { children: ReactNode }) {
  const [keycloak, setKeycloak] = useState<Keycloak | null>(null);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    initKeycloak().then((kc) => {
      setKeycloak(kc);
      setInitialized(true);

      if (kc.authenticated && kc.token) {
        TokenManager.setKeycloakToken(kc.token);
        TokenManager.setKeycloakRefreshToken(kc.refreshToken || "");
      }
    });
  }, []);

  const login = useCallback(async (redirectPath?: string) => {
    await loginKeycloak(redirectPath);
  }, []);

  const logout = useCallback(async () => {
    TokenManager.clearTokens();
    await logoutKeycloak();
    window.location.href = "/login";
  }, []);

  const refreshToken = useCallback(async () => {
    const success = await refreshKeycloakToken();
    if (success && keycloak?.token) {
      TokenManager.setKeycloakToken(keycloak.token);
      if (keycloak.refreshToken) {
        TokenManager.setKeycloakRefreshToken(keycloak.refreshToken);
      }
    }
    return success;
  }, [keycloak]);

  return (
    <KeycloakContext.Provider
      value={{
        keycloak,
        initialized,
        authenticated: keycloak?.authenticated ?? false,
        login,
        logout,
        refreshToken,
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
