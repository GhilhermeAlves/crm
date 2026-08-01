import Keycloak from "keycloak-js";
import { TokenManager } from "@/store/token-manager";

const keycloakConfig = {
  url: process.env.NEXT_PUBLIC_KEYCLOAK_URL || "http://localhost:8080",
  realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM || "CRM",
  clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || "crm-frontend",
};

let keycloakInstance: Keycloak | null = null;

export function getKeycloakInstance(): Keycloak {
  if (!keycloakInstance) {
    keycloakInstance = new Keycloak(keycloakConfig);
  }
  return keycloakInstance;
}

export async function initKeycloak(): Promise<Keycloak> {
  const kc = getKeycloakInstance();
  if (!kc.authenticated) {
    try {
      await kc.init({
        onLoad: "check-sso",
        silentCheckSsoRedirectUri:
          window.location.origin + "/silent-check-sso.html",
        pkceMethod: "S256",
        checkLoginIframe: false,
      });
    } catch {
      return kc;
    }
  }
  return kc;
}

export async function loginKeycloak(redirectPath?: string): Promise<void> {
  const kc = getKeycloakInstance();
  const target = redirectPath || "/dashboard";
  await kc.login({
    redirectUri: `${window.location.origin}/auth/callback?redirect=${encodeURIComponent(target)}`,
  });
}

export async function logoutKeycloak(): Promise<void> {
  const kc = getKeycloakInstance();
  await kc.logout({
    redirectUri: window.location.origin + "/login",
  });
}

/**
 * Único ponto de renovação de token do frontend. Usa exclusivamente
 * `keycloak.updateToken` (o Keycloak é o único responsável pelo refresh) e
 * sincroniza o resultado no TokenManager via `setTokens` (único escritor).
 * O keycloak-js deduplica refreshes concorrentes internamente.
 */
export async function refreshAccessToken(minValidity: number = 30): Promise<boolean> {
  const kc = getKeycloakInstance();
  if (!kc.authenticated) return false;
  try {
    await kc.updateToken(minValidity);
    if (kc.token) {
      TokenManager.setTokens(kc.token, kc.refreshToken || null);
    }
    return true;
  } catch {
    return false;
  }
}
