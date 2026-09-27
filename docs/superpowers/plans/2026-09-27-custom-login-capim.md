# Login Customizado (Visual Capim) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar página de login customizada no CRM (visual Capim) com autenticação programática contra Keycloak, sem redirecionar para tela padrão do Keycloak.

**Architecture:** 
1. **Backend (auth-service):** Adicionar endpoint POST `/internal/auth/login` que recebe email/senha, autentica contra Keycloak Admin API, e retorna JWT válido
2. **Frontend:** Criar mutation `useLoginMutation` que chama o novo endpoint, captura o JWT, e configura sessão
3. **Frontend (UI):** Redesenhar `LoginForm` com campos email/senha estilo Capim, validação local, e integração com mutation
4. **Integração:** Manter fluxo de provedores de identidade intacto, apenas adicionar novo caminho de autenticação

**Tech Stack:** 
- Backend: Spring Boot (auth-service), Keycloak Admin Client
- Frontend: React, TanStack Query, TypeScript
- Design: Tokens CSS do Capim, compostos visuais similares

**Spec:** Página de login do Capim: https://dash.capim.com.br/#/sign-in

---

## File Structure

**Backend (auth-service):**
- Modify: `auth-service/pom.xml` — adicionar Keycloak Admin Client dependency
- Create: `auth-service/src/main/java/com/becommerce/auth/application/identity/port/output/KeycloakAuthPort.java` — port de autenticação
- Create: `auth-service/src/main/java/com/becommerce/auth/infrastructure/identity/client/KeycloakAdminClientImpl.java` — implementação cliente Keycloak
- Create: `auth-service/src/main/java/com/becommerce/auth/application/identity/service/CredentialsAuthService.java` — use case de login
- Modify: `auth-service/src/main/java/com/becommerce/auth/presentation/rest/AuthController.java` — adicionar endpoint POST /login
- Create: `auth-service/src/main/java/com/becommerce/auth/application/identity/dto/LoginRequest.java` — DTO de entrada
- Create: `auth-service/src/main/java/com/becommerce/auth/application/identity/dto/LoginResponse.java` — DTO de saída
- Create: `auth-service/src/test/java/com/becommerce/auth/application/identity/service/CredentialsAuthServiceTest.java` — testes

**Frontend (CRM):**
- Create: `src/features/auth/hooks/useLoginMutation.ts` — mutation para login com credenciais
- Modify: `src/features/auth/hooks/useAuth.tsx` — adicionar função `loginWithCredentials`
- Create: `src/features/auth/components/LoginFormCredentials.tsx` — novo formulário com email/senha
- Modify: `src/app/(auth)/login/page.tsx` — usar novo formulário
- Create: `src/lib/auth-api.ts` — cliente API para login
- Create: `src/features/auth/hooks/useLoginMutation.test.ts` — testes

---

## Task 1: Backend Setup - Adicionar Keycloak Admin Client Dependency

**Files:**
- Modify: `auth-service/pom.xml`

**Interfaces:**
- Consumes: Project version (check current pom.xml)
- Produces: Keycloak Admin Client available for import

- [ ] **Step 1: Read current pom.xml**

```bash
cat auth-service/pom.xml | grep -A 5 "<version>"
```

- [ ] **Step 2: Add Keycloak Admin Client dependency**

In `auth-service/pom.xml`, find `<dependencies>` section and add:

```xml
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-admin-client</artifactId>
    <version>24.0.4</version>
</dependency>
```

- [ ] **Step 3: Verify pom.xml syntax**

```bash
cd auth-service && mvn validate
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add auth-service/pom.xml
git commit -m "chore(auth-service): add keycloak-admin-client dependency"
```

---

## Task 2: Backend - Create Keycloak Port Interface

**Files:**
- Create: `auth-service/src/main/java/com/becommerce/auth/application/identity/port/output/KeycloakAuthPort.java`

**Interfaces:**
- Consumes: —
- Produces: `KeycloakAuthPort` interface with method `authenticateWithCredentials(email: String, password: String): String` (returns JWT)

- [ ] **Step 1: Create port interface**

```java
package com.becommerce.auth.application.identity.port.output;

/**
 * Port for authenticating against Keycloak with email/password credentials.
 * Implementação: KeycloakAdminClient (via Keycloak Admin REST API).
 */
public interface KeycloakAuthPort {
    /**
     * Authenticate user with email and password directly against Keycloak.
     * @param email User email
     * @param password User password
     * @return JWT token from Keycloak
     * @throws InvalidCredentialsException if email/password invalid
     * @throws KeycloakUnavailableException if Keycloak unreachable
     */
    String authenticateWithCredentials(String email, String password);
}
```

- [ ] **Step 2: Create exception classes**

Create `auth-service/src/main/java/com/becommerce/auth/domain/identity/exception/InvalidCredentialsException.java`:

```java
package com.becommerce.auth.domain.identity.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
```

Create `auth-service/src/main/java/com/becommerce/auth/domain/identity/exception/KeycloakUnavailableException.java`:

```java
package com.becommerce.auth.domain.identity.exception;

public class KeycloakUnavailableException extends RuntimeException {
    public KeycloakUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add auth-service/src/main/java/com/becommerce/auth/application/identity/port/output/KeycloakAuthPort.java
git add auth-service/src/main/java/com/becommerce/auth/domain/identity/exception/InvalidCredentialsException.java
git add auth-service/src/main/java/com/becommerce/auth/domain/identity/exception/KeycloakUnavailableException.java
git commit -m "feat(auth-service): add KeycloakAuthPort interface and exceptions"
```

---

## Task 3: Backend - Implement Keycloak Admin Client

**Files:**
- Create: `auth-service/src/main/java/com/becommerce/auth/infrastructure/identity/client/KeycloakAdminClientImpl.java`

**Interfaces:**
- Consumes: `KeycloakAuthPort` interface, Keycloak configuration (KEYCLOAK_SERVER_URL, KEYCLOAK_CLIENT_ID, KEYCLOAK_CLIENT_SECRET, KEYCLOAK_REALM)
- Produces: `KeycloakAuthPort` implementation that calls Keycloak Admin REST API

- [ ] **Step 1: Read Keycloak properties from environment**

Check `auth-service/src/main/resources/application.yml` or `.env.example` for Keycloak config:

```bash
cat auth-service/.env.example | grep -i keycloak
```

- [ ] **Step 2: Create Keycloak Admin Client Implementation**

```java
package com.becommerce.auth.infrastructure.identity.client;

import com.becommerce.auth.application.identity.port.output.KeycloakAuthPort;
import com.becommerce.auth.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.auth.domain.identity.exception.KeycloakUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

@Component
public class KeycloakAdminClientImpl implements KeycloakAuthPort {

    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;

    public KeycloakAdminClientImpl(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${app.keycloak.realm:master}") String realm,
            @Value("${app.keycloak.client-id:}") String clientId,
            @Value("${app.keycloak.client-secret:}") String clientSecret,
            RestClient.Builder restClientBuilder) {
        this.serverUrl = issuerUri.replaceAll("/realms/.*$", "");
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = restClientBuilder.baseUrl(this.serverUrl).build();
    }

    @Override
    public String authenticateWithCredentials(String email, String password) {
        try {
            var response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=password" +
                            "&client_id=" + clientId +
                            "&client_secret=" + clientSecret +
                            "&username=" + email +
                            "&password=" + password +
                            "&scope=openid profile email")
                    .retrieve()
                    .onStatus(status -> status.value() == 401,
                            (request, errorResponse) -> {
                                throw new InvalidCredentialsException("Email ou senha inválidos.");
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            (request, errorResponse) -> {
                                throw new KeycloakUnavailableException(
                                    "Keycloak temporariamente indisponível", null);
                            })
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.access_token == null) {
                throw new KeycloakUnavailableException(
                    "Resposta inválida do Keycloak", null);
            }

            return response.access_token;
        } catch (Exception e) {
            if (e instanceof InvalidCredentialsException || 
                e instanceof KeycloakUnavailableException) {
                throw e;
            }
            throw new KeycloakUnavailableException(
                "Erro ao autenticar com Keycloak: " + e.getMessage(), e);
        }
    }

    public static class KeycloakTokenResponse {
        public String access_token;
        public String token_type;
        public int expires_in;
        public String refresh_token;
    }
}
```

- [ ] **Step 3: Add properties to application.yml**

In `auth-service/src/main/resources/application.yml`, add:

```yaml
app:
  keycloak:
    realm: crm
    client-id: ${KEYCLOAK_CLIENT_ID:crm-client}
    client-secret: ${KEYCLOAK_CLIENT_SECRET:}
```

- [ ] **Step 4: Run tests**

```bash
cd auth-service && mvn test
```

- [ ] **Step 5: Commit**

```bash
git add auth-service/src/main/java/com/becommerce/auth/infrastructure/identity/client/KeycloakAdminClientImpl.java
git add auth-service/src/main/resources/application.yml
git commit -m "feat(auth-service): implement Keycloak Admin Client with direct grant flow"
```

---

## Task 4: Backend - Create Credentials Auth Service (Use Case)

**Files:**
- Create: `auth-service/src/main/java/com/becommerce/auth/application/identity/service/CredentialsAuthService.java`

**Interfaces:**
- Consumes: `KeycloakAuthPort`
- Produces: `authenticateWithCredentials(email: String, password: String): String` (returns JWT)

- [ ] **Step 1: Create service class**

```java
package com.becommerce.auth.application.identity.service;

import com.becommerce.auth.application.identity.port.output.KeycloakAuthPort;
import org.springframework.stereotype.Service;

@Service
public class CredentialsAuthService {

    private final KeycloakAuthPort keycloakAuthPort;

    public CredentialsAuthService(KeycloakAuthPort keycloakAuthPort) {
        this.keycloakAuthPort = keycloakAuthPort;
    }

    /**
     * Authenticate user with email and password.
     * Returns JWT token from Keycloak ready to use in subsequent API calls.
     */
    public String authenticateWithCredentials(String email, String password) {
        return keycloakAuthPort.authenticateWithCredentials(email, password);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add auth-service/src/main/java/com/becommerce/auth/application/identity/service/CredentialsAuthService.java
git commit -m "feat(auth-service): add CredentialsAuthService use case"
```

---

## Task 5: Backend - Create DTOs for Login Endpoint

**Files:**
- Create: `auth-service/src/main/java/com/becommerce/auth/application/identity/dto/LoginRequest.java`
- Create: `auth-service/src/main/java/com/becommerce/auth/application/identity/dto/LoginResponse.java`

**Interfaces:**
- Consumes: —
- Produces: `LoginRequest` (email, password), `LoginResponse` (access_token, token_type, expires_in)

- [ ] **Step 1: Create LoginRequest DTO**

```java
package com.becommerce.auth.application.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    String password
) {}
```

- [ ] **Step 2: Create LoginResponse DTO**

```java
package com.becommerce.auth.application.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("expires_in")
    Integer expiresIn
) {
    public static LoginResponse from(String accessToken, String tokenType, Integer expiresIn) {
        return new LoginResponse(accessToken, tokenType, expiresIn);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add auth-service/src/main/java/com/becommerce/auth/application/identity/dto/LoginRequest.java
git add auth-service/src/main/java/com/becommerce/auth/application/identity/dto/LoginResponse.java
git commit -m "feat(auth-service): add LoginRequest and LoginResponse DTOs"
```

---

## Task 6: Backend - Add Login Endpoint to AuthController

**Files:**
- Modify: `auth-service/src/main/java/com/becommerce/auth/presentation/rest/AuthController.java`

**Interfaces:**
- Consumes: `LoginRequest`, `CredentialsAuthService`
- Produces: POST `/internal/auth/login` endpoint returning `LoginResponse`

- [ ] **Step 1: Read current AuthController and add endpoint**

Modify AuthController to add:

```java
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    String jwtToken = credentialsAuthService.authenticateWithCredentials(
        request.email(), 
        request.password()
    );
    
    LoginResponse response = LoginResponse.from(
        jwtToken,
        "Bearer",
        3600
    );
    
    return ResponseEntity.ok(response);
}
```

Inject `CredentialsAuthService credentialsAuthService` in constructor.

- [ ] **Step 2: Add GlobalExceptionHandler**

Create `auth-service/src/main/java/com/becommerce/auth/presentation/rest/GlobalExceptionHandler.java`:

```java
package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.auth.domain.identity.exception.KeycloakUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("INVALID_CREDENTIALS", e.getMessage()));
    }

    @ExceptionHandler(KeycloakUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleKeycloakUnavailable(KeycloakUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse("KEYCLOAK_UNAVAILABLE", e.getMessage()));
    }

    public record ErrorResponse(String code, String message) {}
}
```

- [ ] **Step 3: Commit**

```bash
git add auth-service/src/main/java/com/becommerce/auth/presentation/rest/AuthController.java
git add auth-service/src/main/java/com/becommerce/auth/presentation/rest/GlobalExceptionHandler.java
git commit -m "feat(auth-service): add POST /internal/auth/login endpoint"
```

---

## Task 7: Frontend - Create Auth API Client

**Files:**
- Create: `src/lib/auth-api.ts`

**Interfaces:**
- Consumes: —
- Produces: `loginWithCredentials(email: string, password: string): Promise<{accessToken: string}>`

- [ ] **Step 1: Create auth-api.ts**

```typescript
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
  password: string
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
```

- [ ] **Step 2: Commit**

```bash
git add src/lib/auth-api.ts
git commit -m "feat(auth): add auth-api client for credentials login"
```

---

## Task 8: Frontend - Create useLoginMutation Hook

**Files:**
- Create: `src/features/auth/hooks/useLoginMutation.ts`

**Interfaces:**
- Consumes: `loginWithCredentials()` from auth-api
- Produces: `useLoginMutation()` hook returning `{mutate, isPending, error}`

- [ ] **Step 1: Create useLoginMutation.ts**

```typescript
import { useMutation } from "@tanstack/react-query";
import { loginWithCredentials } from "@/lib/auth-api";

interface LoginMutationVariables {
  email: string;
  password: string;
  redirect?: string;
}

export function useLoginMutation() {
  return useMutation({
    mutationFn: async ({ email, password }: LoginMutationVariables) => {
      return loginWithCredentials(email, password);
    },
    onSuccess: (data) => {
      window.location.href = "/dashboard";
    },
    onError: (error: Error) => {
      console.error("Login error:", error.message);
    },
  });
}
```

- [ ] **Step 2: Commit**

```bash
git add src/features/auth/hooks/useLoginMutation.ts
git commit -m "feat(auth): add useLoginMutation hook for credentials login"
```

---

## Task 9: Frontend - Create Credentials Login Form Component

**Files:**
- Create: `src/features/auth/components/LoginFormCredentials.tsx`

**Interfaces:**
- Consumes: `useLoginMutation()`, form state
- Produces: React component with email/password inputs, similar to Capim design

- [ ] **Step 1: Create LoginFormCredentials.tsx**

```typescript
"use client";

import { useState } from "react";
import { Loader2, LogIn } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useLoginMutation } from "../hooks/useLoginMutation";

export function LoginFormCredentials() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const { mutate, isPending, error } = useLoginMutation();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutate({ email, password });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <label
          htmlFor="email"
          className="text-sm font-medium text-crm-text"
        >
          Email
        </label>
        <input
          id="email"
          type="email"
          placeholder="seu@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          disabled={isPending}
          required
          className="w-full px-3 py-2 border border-crm-border rounded-md bg-crm-background text-crm-text placeholder-crm-text-secondary focus:outline-none focus:ring-2 focus:ring-crm-primary"
        />
      </div>

      <div className="space-y-2">
        <label
          htmlFor="password"
          className="text-sm font-medium text-crm-text"
        >
          Senha
        </label>
        <input
          id="password"
          type="password"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          disabled={isPending}
          required
          className="w-full px-3 py-2 border border-crm-border rounded-md bg-crm-background text-crm-text placeholder-crm-text-secondary focus:outline-none focus:ring-2 focus:ring-crm-primary"
        />
      </div>

      {error && (
        <div className="rounded-md bg-red-50 p-3 text-sm text-red-800">
          {error.message}
        </div>
      )}

      <Button
        type="submit"
        variant="crm"
        className="w-full"
        disabled={isPending}
      >
        {isPending ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : (
          <LogIn className="h-4 w-4" />
        )}
        Entrar com e-mail e senha
      </Button>

      <p className="text-center text-xs text-crm-text-secondary">
        Login seguro — os dados ficam entre você e nossos servidores.
      </p>
    </form>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/features/auth/components/LoginFormCredentials.tsx
git commit -m "feat(auth): add LoginFormCredentials component with email/password inputs"
```

---

## Task 10: Frontend - Update Login Page to Use New Form

**Files:**
- Modify: `src/app/(auth)/login/page.tsx`

**Interfaces:**
- Consumes: `LoginFormCredentials` component
- Produces: Updated login page using new form

- [ ] **Step 1: Replace LoginForm with LoginFormCredentials**

Update the page to import and use LoginFormCredentials instead of LoginForm.

- [ ] **Step 2: Commit**

```bash
git add src/app/\(auth\)/login/page.tsx
git commit -m "feat(auth): update login page to use new credentials form"
```

---

## Task 11: Integration - API Route Proxy

**Files:**
- Create: `src/app/auth/login/route.ts` (optional, if needed for proxying)

**Interfaces:**
- Consumes: Keycloak auth-service endpoint
- Produces: Proxied `/auth/login` endpoint

- [ ] **Step 1: Verify if proxying is needed**

Check if `/auth/*` routes are already proxied to auth-service via dev server.

- [ ] **Step 2: If needed, create route**

```typescript
import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  const body = await request.json();

  const response = await fetch(
    `${process.env.NEXT_PUBLIC_AUTH_SERVICE_URL || "http://localhost:8082"}/internal/auth/login`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }
  );

  if (!response.ok) {
    return NextResponse.json(
      await response.json(),
      { status: response.status }
    );
  }

  return NextResponse.json(await response.json());
}
```

- [ ] **Step 3: Commit**

```bash
git add src/app/auth/login/route.ts
git commit -m "feat(auth): add API route proxy for /auth/login"
```

---

## Task 12: Manual Testing & Verification

**Interfaces:**
- Consumes: Running backend + frontend
- Produces: Verified credentials login flow

- [ ] **Step 1: Start services and test login**

- [ ] **Step 2: Verify OAuth providers still work**

- [ ] **Step 3: Test error handling**

---

## Task 13: Documentation

**Files:**
- Create: `docs/LOGIN_FLOW.md`

- [ ] **Step 1: Document the flow**

- [ ] **Step 2: Update IMPLEMENTATION_REPORT.md**

---

## Rollback Plan

If issues arise, revert the commits created during implementation.
