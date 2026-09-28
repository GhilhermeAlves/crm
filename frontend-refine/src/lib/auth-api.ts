export interface LoginResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
}

/**
 * Login with email and password via auth-service endpoint.
 * Returns JWT token from Keycloak.
 */
export async function loginWithCredentials(
  email: string,
  password: string,
): Promise<LoginResponse> {
  const response = await fetch("/auth/login", {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) {
    const error = await response.json();
    if (response.status === 401) {
      throw new Error("Email ou senha inválidos");
    }
    throw new Error(error.message || "Erro ao fazer login");
  }

  return response.json();
}
