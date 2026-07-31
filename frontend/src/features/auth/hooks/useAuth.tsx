"use client";

import { createContext, useContext, useCallback, useState, useEffect, type ReactNode } from "react";
import { useMe } from "./useAuthMutations";
import { TokenManager } from "@/store/token-manager";
import type { User } from "../types/auth.types";
import { useKeycloak } from "@/providers/KeycloakProvider";

type AuthContextType = {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  logout: () => void;
  loginKeycloak: (redirectPath?: string) => Promise<void>;
  /** Roles do realm do Keycloak (identidade OIDC) — UX apenas. */
  roles: string[];
  /**
   * Permissões de negócio (CRM) para UX (menus/botões). Vindas do CurrentUser
   * da aplicação; placeholder até o endpoint público do auth-service (Sprint 4).
   * Autorização real é sempre do backend.
   */
  permissions: string[];
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const keycloakCtx = useKeycloak();

  // `/auth/me` só após o Keycloak inicializar/autenticar (sem race condition).
  const keycloakReady = keycloakCtx.initialized && keycloakCtx.authenticated;
  const { data, isLoading: isMeLoading, isError } = useMe(keycloakReady);

  useEffect(() => {
    if (data) {
      setUser(data);
    } else if (!isMeLoading && (isError || !TokenManager.hasTokens())) {
      setUser(null);
    }
  }, [data, isMeLoading, isError]);

  const logout = useCallback(() => {
    keycloakCtx.logout();
  }, [keycloakCtx]);

  const loginKeycloak = useCallback(
    async (redirectPath?: string) => {
      // Sempre via keycloak-js (PKCE S256) — nunca montar URL OIDC manualmente.
      await keycloakCtx.login(redirectPath);
    },
    [keycloakCtx],
  );

  const roles = TokenManager.getRoles();

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading: isMeLoading,
        logout,
        loginKeycloak,
        roles,
        permissions: [],
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
