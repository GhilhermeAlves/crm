package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.application.BillingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cliente da API v3 do Asaas. Nunca inclua a chave em mensagens de erro ou logs. */
@Component
public class AsaasClient {

    private final RestClient rest;
    private final BillingProperties props;

    @Autowired
    public AsaasClient(BillingProperties props) {
        this(RestClient.builder().requestFactory(timeoutFactory()), props);
    }

    /** Usado nos testes: o builder já vem com o request factory (ex.: MockRestServiceServer). */
    public AsaasClient(RestClient.Builder builder, BillingProperties props) {
        this.rest = builder.build();
        this.props = props;
    }

    private static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5_000);
        rf.setReadTimeout(15_000);
        return rf;
    }

    public static class AsaasException extends RuntimeException {
        public AsaasException(String message) { super(message); }
    }

    public String createCustomer(String name, String cpfCnpj, String email, String externalReference) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("cpfCnpj", cpfCnpj.replaceAll("\\D", ""));
        body.put("email", email);
        body.put("externalReference", externalReference);
        return requireId(post("/customers", body));
    }

    public String createSubscription(String customerId, int valueCents, LocalDate nextDueDate,
                                     String description, String externalReference) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("cycle", "MONTHLY");
        body.put("value", toReais(valueCents));
        body.put("nextDueDate", nextDueDate.toString());
        body.put("description", description);
        body.put("externalReference", externalReference);
        return requireId(post("/subscriptions", body));
    }

    private static String requireId(JsonNode node) {
        String id = node == null ? "" : node.path("id").asText("");
        if (id.isBlank()) throw new AsaasException("Asaas não devolveu o id.");
        return id;
    }

    public record AsaasPayment(String id, String subscription, String externalReference, String status,
                               boolean deleted, LocalDate dueDate, int valueCents, String invoiceUrl,
                               LocalDate paymentDate) {}

    public AsaasPayment getPayment(String paymentId) {
        JsonNode n = get("/payments/{id}", paymentId);
        String id = requireId(n);
        String paid = text(n, "paymentDate");
        if (paid == null) paid = text(n, "clientPaymentDate");
        return new AsaasPayment(
                id,
                text(n, "subscription"),
                text(n, "externalReference"),
                text(n, "status"),
                n.path("deleted").asBoolean(false),
                LocalDate.parse(n.path("dueDate").asText()),
                n.path("value").decimalValue().movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).intValueExact(),
                text(n, "invoiceUrl"),
                paid == null ? null : LocalDate.parse(paid));
    }

    public String getSubscriptionReference(String subscriptionId) {
        JsonNode n = get("/subscriptions/{id}", subscriptionId);
        requireId(n);
        return text(n, "externalReference");
    }

    private JsonNode get(String path, String id) {
        return call(() -> rest.get().uri(props.baseUrl() + path, id)
                .headers(h -> { h.set("access_token", props.apiKey()); h.set("User-Agent", "clin3dpro-crm"); })
                .retrieve().body(JsonNode.class));
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    public void updateSubscriptionValue(String subscriptionId, int valueCents, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("value", toReais(valueCents));
        body.put("description", description);
        body.put("updatePendingPayments", false);
        call(() -> rest.put().uri(props.baseUrl() + "/subscriptions/{id}", subscriptionId)
                .headers(h -> { h.set("access_token", props.apiKey()); h.set("User-Agent", "clin3dpro-crm"); })
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class));
    }

    /**
     * Remove a assinatura no Asaas (ele também remove as cobranças pendentes dela).
     * Assinatura já inexistente (404) conta como removida.
     */
    public void deleteSubscription(String subscriptionId) {
        try {
            rest.delete().uri(props.baseUrl() + "/subscriptions/{id}", subscriptionId)
                    .headers(h -> { h.set("access_token", props.apiKey()); h.set("User-Agent", "clin3dpro-crm"); })
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) return;
            throw new AsaasException("Asaas recusou (" + e.getStatusCode().value() + "): " + describe(e));
        } catch (RestClientException e) {
            throw new AsaasException("Asaas indisponível.");
        }
    }

    private JsonNode post(String path, Map<String, Object> body) {
        return call(() -> rest.post().uri(props.baseUrl() + path)
                .headers(h -> { h.set("access_token", props.apiKey()); h.set("User-Agent", "clin3dpro-crm"); })
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class));
    }

    private JsonNode call(java.util.function.Supplier<JsonNode> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new AsaasException("Asaas recusou (" + e.getStatusCode().value() + "): " + describe(e));
        } catch (RestClientException e) {
            throw new AsaasException("Asaas indisponível.");
        }
    }

    private static String describe(RestClientResponseException e) {
        try {
            JsonNode errors = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(e.getResponseBodyAsString()).path("errors");
            if (errors.isArray() && !errors.isEmpty()) return errors.get(0).path("description").asText();
        } catch (Exception ignored) {
            // corpo não-JSON
        }
        return "erro sem descrição";
    }

    static BigDecimal toReais(int cents) { return BigDecimal.valueOf(cents, 2); }
}
