package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Testes de concorrência do rate limiter distribuído (Sprint 6.8) em nível de
 * integração: múltiplas threads disputando o <b>mesmo bucket Redis</b> com um
 * contador atômico por chave (espelha a semântica do script Lua
 * {@code INCR} + {@code EXPIRE} do {@link GatewayRateLimiter}) e liberação
 * simultânea via {@link CountDownLatch}.
 *
 * <p>O que é validado sob carga real de threads:
 * <ul>
 *   <li><b>sem over-admission</b>: exatamente {@code limit} requisições são
 *       aceitas e todo excedente recebe {@link RateLimitExceededException}
 *       (429) — nenhuma conta além do limite passa;</li>
 *   <li><b>sem contagem perdida nem incremento duplicado</b>: o número de
 *       incrementos atômicos é exatamente igual ao número de chamadas;</li>
 *   <li><b>TTL correto</b>: o tamanho da janela é passado como {@code EXPIRE}
 *       em toda chamada;</li>
 *   <li><b>isolamento usuário A/B</b> e <b>IP fallback</b> sob concorrência —
 *       buckets independentes, sem cross-talk;</li>
 *   <li><b>expiração sob concorrência</b>: fim da janela → nova janela, sem
 *       bloqueio permanente e sem contagem persistindo indevidamente;</li>
 *   <li><b>Redis down durante carga</b>: fail-controlled (permite com warning,
 *       sem 500 em cascata) e recuperação automática.</li>
 * </ul>
 *
 * <p>A atomicidade do {@code INCR} no Redis é propriedade do próprio Redis
 * (validada no E2E da VPS); aqui o contador atômico por chave reproduz a mesma
 * invariante para exercitar a lógica de decisão do limiter sob contenção real.
 */
class GatewayRateLimitConcurrencyTest {

    /** Contador atômico por chave do bucket — espelha o {@code INCR} do Lua. */
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    private final AtomicInteger ttlArgSeen = new AtomicInteger(-1);

    private StringRedisTemplate redis;

    private GatewayRateLimiter limiter;

    @BeforeEach
    void setUp() {
        redis = Mockito.mock(StringRedisTemplate.class);
        configureAtomicCounter();
        limiter = new GatewayRateLimiter(redis);
    }

    /** Cada execução do "script" incrementa atomicamente o contador da chave e
     *  "agenda" o EXPIRE com o TTL da janela (ARGV[1]) — espelha o Lua do limiter. */
    private void configureAtomicCounter() {
        Mockito.reset(redis);
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<String> keys = (List<String>) invocation.getArgument(1);
                    String firstArg = firstVarArg(invocation);
                    if (firstArg != null) {
                        ttlArgSeen.set(Integer.parseInt(firstArg));
                    }
                    return counters.computeIfAbsent(keys.get(0), k -> new AtomicLong()).incrementAndGet();
                });
    }

    /** Recupera o primeiro vararg da invocação (o Mockito pode entregá-lo como
     *  {@code Object[]} ou como valor único — deparando com ambos os casos). */
    private static String firstVarArg(org.mockito.invocation.InvocationOnMock invocation) {
        Object a = invocation.getArguments()[2];
        if (a instanceof Object[] arr) {
            return arr.length > 0 ? String.valueOf(arr[0]) : null;
        }
        return a == null ? null : String.valueOf(a);
    }

    /** Dispara {@code tasks} concorrentes e retorna quantas lançaram exceção. */
    private int runConcurrent(int tasks, int threads, Runnable task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger exceeded = new AtomicInteger();
        try {
            for (int i = 0; i < tasks; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        task.run();
                    } catch (RateLimitExceededException e) {
                        exceeded.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "tasks devem terminar em 30s");
        } finally {
            pool.shutdownNow();
        }
        return exceeded.get();
    }

    @Test
    void shouldAdmitExactlyLimitConcurrentRequestsInSameBucket() throws Exception {
        int limit = 50;
        int total = 200;
        int exceeded = runConcurrent(total, 16, () -> limiter.enforce("api", "user-A", limit, Duration.ofSeconds(60)));

        // Sem over-admission: exatamente `limit` aceitas, exatamente `total-limit` com 429.
        assertEquals(total - limit, exceeded,
                "apenas o excedente do limite deve receber 429 sob concorrência");
        assertEquals(total, counters.values().stream().mapToLong(AtomicLong::get).sum(),
                "contagem total = número de chamadas (sem contagem perdida)");
    }

    @Test
    void shouldNotLoseOrDuplicateCountsUnderConcurrentSameBucketLoad() throws Exception {
        int total = 500;
        runConcurrent(total, 24, () -> limiter.enforce("refresh", "session-shared", 100_000, Duration.ofSeconds(60)));

        assertEquals(total, counters.values().stream().mapToLong(AtomicLong::get).sum(),
                "cada chamada incrementa exatamente 1 (sem duplicar nem perder)");
        assertTrue(counters.size() <= 2,
                "todas as chamadas concorrentes devem cair na mesma janela (mesma chave Redis)");
    }

    @Test
    void shouldPassWindowTtlAsExpireArgumentUnderConcurrency() throws Exception {
        Duration window = Duration.ofSeconds(60);
        runConcurrent(100, 8, () -> limiter.enforce("authorize", "ip-x", 100, window));

        assertEquals(60, ttlArgSeen.get(),
                "a janela configurada deve ser propagada como TTL (EXPIRE) do bucket");
    }

    @Test
    void shouldIsolateConcurrentUsersInSeparateBuckets() throws Exception {
        int limit = 25;
        int total = 100;
        AtomicInteger aExceeded = new AtomicInteger();
        AtomicInteger bExceeded = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        try {
            for (int i = 0; i < total; i++) {
                final boolean isA = i % 2 == 0;
                pool.submit(() -> {
                    try {
                        start.await();
                        if (isA) {
                            limiter.enforce("api", "user-A", limit, Duration.ofSeconds(60));
                        } else {
                            limiter.enforce("api", "user-B", limit, Duration.ofSeconds(60));
                        }
                    } catch (RateLimitExceededException e) {
                        if (isA) {
                            aExceeded.incrementAndGet();
                        } else {
                            bExceeded.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        // 50 chamadas por usuário, limite 25 → exatamente 25 excedentes por usuário.
        assertEquals(25, aExceeded.get(), "usuário A excede exatamente seu limite");
        assertEquals(25, bExceeded.get(), "usuário B excede exatamente seu limite (sem interferência de A)");
        assertEquals(2, counters.size(), "cada usuário usa um bucket próprio");
    }

    @Test
    void shouldIsolateConcurrentIpFallbackBuckets() throws Exception {
        int limit = 20;
        int total = 60;
        AtomicInteger ipAExceeded = new AtomicInteger();
        AtomicInteger ipBExceeded = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        try {
            for (int i = 0; i < total; i++) {
                final boolean isA = i % 2 == 0;
                pool.submit(() -> {
                    try {
                        start.await();
                        if (isA) {
                            limiter.enforce("api", "189.60.1.2", limit, Duration.ofSeconds(60));
                        } else {
                            limiter.enforce("api", "200.147.3.9", limit, Duration.ofSeconds(60));
                        }
                    } catch (RateLimitExceededException e) {
                        if (isA) {
                            ipAExceeded.incrementAndGet();
                        } else {
                            ipBExceeded.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        // 30 chamadas por IP, limite 20 → exatamente 10 excedentes por IP.
        assertEquals(10, ipAExceeded.get(), "IP A bloqueia apenas o excedente do seu bucket");
        assertEquals(10, ipBExceeded.get(), "IP B bloqueia apenas o excedente do seu bucket");
        assertEquals(2, counters.size(), "IPs distintos usam buckets independentes");
    }

    @Test
    void shouldStartFreshWindowAfterExpirationWithoutPermanentBlock() {
        int limit = 1;
        Duration window = Duration.ofSeconds(1);

        // Consome o bucket: 1ª aceita, 2ª excede.
        limiter.enforce("api", "user-window", limit, window);
        assertThrowsRateLimited(() -> limiter.enforce("api", "user-window", limit, window));

        // Próximo segundo = nova janela → o bucket volta a permitir (sem bloqueio
        // permanente e sem contagem persistindo indevidamente).
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        boolean recovered = false;
        while (System.nanoTime() < deadline) {
            try {
                limiter.enforce("api", "user-window", limit, window);
                recovered = true;
                break;
            } catch (RateLimitExceededException e) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        assertTrue(recovered, "nova janela deve permitir a requisição (expiração sob concorrência)");
    }

    @Test
    void shouldAllowAllRequestsWhenRedisDownDuringConcurrentLoadAndRecover() throws Exception {
        // Redis fora durante toda a carga: fail-controlled — nenhum 429, nenhum 500,
        // e o limiter não derruba (sem loop de erro nem avalanche de 429).
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenThrow(new QueryTimeoutException("connection timed out"));
        int downExceeded = runConcurrent(100, 16,
                () -> limiter.enforce("api", "user-A", 10, Duration.ofSeconds(60)));
        assertEquals(0, downExceeded, "com Redis fora o limiter permite (fail-controlled), sem avalanche de 429");

        // Recuperação automática: Redis volta a contar atomicamente e o limite
        // volta a valer — 30 chamadas, limite 10 → exatamente 20 excedentes.
        configureAtomicCounter();
        int exceeded = runConcurrent(30, 8,
                () -> limiter.enforce("api", "user-A", 10, Duration.ofSeconds(60)));
        assertEquals(20, exceeded, "após a recuperação o limite volta a valer");
    }

    @Test
    void shouldNotLoopOnRedisConnectionFailureDuringLoad() throws Exception {
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));
        int exceeded = runConcurrent(50, 8,
                () -> limiter.enforce("authorize", "ip-y", 5, Duration.ofSeconds(60)));
        assertEquals(0, exceeded, "falha de conexão contínua: permite com warning, sem loop de 429");
    }

    private void assertThrowsRateLimited(Runnable runnable) {
        try {
            runnable.run();
        } catch (RateLimitExceededException e) {
            assertFalse(e.getRetryAfterSeconds() < 1, "Retry-After deve ser >= 1s");
            return;
        }
        throw new AssertionError("esperado RateLimitExceededException");
    }
}
