package com.becommerce.auth.domain.gateway;

/**
 * Exceção de domínio de rate limiting (Sprint 6.6): o cliente excedeu o limite
 * de requisições do Gateway. Traduzida em {@code 429 Too Many Requests} com o
 * header {@code Retry-After}.
 */
public class RateLimitExceededException extends RuntimeException {

    public static final int STATUS = 429;
    public static final String CODE = "RATE_LIMIT_EXCEEDED";

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Muitas requisições. Tente novamente em instantes.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
