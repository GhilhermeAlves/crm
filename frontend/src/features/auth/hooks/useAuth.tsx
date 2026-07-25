"use client";

import { createContext, useContext, useCallback, useState, useEffect, type ReactNode } from "react";
import { useMe, useLogout } from "./useAuthMutations";
import { TokenManager } from "@/store/token-manager";
import type { User } from "../types/auth.types";
import { useKeycloak } from "@/providers/KeycloakProvider";

type AuthContextType = {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  logout: () => void;
  loginKeycloak: (redirectPath?: string) => Promise<void>;
  roles: string[];
  permissions: string[];
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const { data, isLoading: isMeLoading, isError } = useMe();
  const logoutMutation = useLogout();
  const keycloakCtx = useKeycloak();

  useEffect(() => {
    if (data) {
      setUser(data);
    } else if (!isMeLoading && (isError || !TokenManager.hasTokens())) {
      if (!TokenManager.isKeycloakAuth()) {
        TokenManager.clearTokens();
      }
      setUser(null);
    }
  }, [data, isMeLoading, isError]);

  const logout = useCallback(() => {
    if (TokenManager.isKeycloakAuth()) {
      keycloakCtx.logout();
    } else {
      logoutMutation.mutate();
    }
  }, [keycloakCtx, logoutMutation]);

  const loginKeycloak = useCallback(async (redirectPath?: string) => {
    if (keycloakCtx.initialized) {
      await keycloakCtx.login(redirectPath);
    } else {
      const target = redirectPath || "/dashboard";
      const keycloakUrl = process.env.NEXT_PUBLIC_KEYCLOAK_URL || "https://76.13.237.238/auth";
      const realm = process.env.NEXT_PUBLIC_KEYCLOAK_REALM || "CRM";
      const clientId = process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || "crm-frontend";
      const redirectUri = `${window.location.origin}/auth/callback?redirect=${encodeURIComponent(target)}`;
      window.location.href = `${keycloakUrl}/realms/${realm}/protocol/openid-connect/auth?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=openid`;
    }
  }, [keycloakCtx]);

  const roles = TokenManager.getRoles();
  const permissions = TokenManager.getPermissions();

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading: isMeLoading,
        logout,
        loginKeycloak,
        roles,
        permissions,
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
