"use client";

import {
  createContext,
  useContext,
  useCallback,
  useState,
  useEffect,
  type ReactNode,
} from "react";
import { usePathname } from "next/navigation";
import { useMe } from "./useAuthMutations";
import { loginWithGateway, logoutWithGateway } from "@/lib/gateway-auth";
import { isPublicPathname } from "@/lib/middleware-auth";
import type { User } from "../types/auth.types";

type AuthContextType = {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  logout: () => void;
  loginKeycloak: (redirectPath?: string) => Promise<void>;
  /** Roles OIDC (identidade) — não carregadas no browser (BFF). UX apenas. */
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
  const pathname = usePathname();

  // `/auth/me` apenas em páginas não-públicas — evita chamada/redirecionamento
  // desnecessário na tela de login (a sessão é validada pelo gateway).
  const enabled = !isPublicPathname(pathname);
  const { data, isLoading: isMeLoading, isError } = useMe(enabled);

  useEffect(() => {
    if (data) {
      setUser(data);
    } else if (!isMeLoading && isError) {
      setUser(null);
    }
  }, [data, isMeLoading, isError]);

  const logout = useCallback(() => {
    logoutWithGateway();
  }, []);

  const loginKeycloak = useCallback(async (redirectPath?: string) => {
    // Sempre via Access Gateway (`/auth/authorize`) — o fluxo OIDC é 100% no
    // servidor; nunca montar URL de autorização manualmente no browser.
    loginWithGateway(redirectPath);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading: isMeLoading,
        logout,
        loginKeycloak,
        roles: [],
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
