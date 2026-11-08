package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.application.BillingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AsaasClientTest {
    private static final String BASE = "https://api-sandbox.asaas.com/v3";
    private MockRestServiceServer server;
    private AsaasClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AsaasClient(builder, new BillingProperties(true, BASE, "KEY123", "WH"));
    }

    @Test
    void createCustomerSendsKeyAndReturnsId() {
        server.expect(requestTo(BASE + "/customers"))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header("access_token", "KEY123"))
              .andExpect(jsonPath("$.cpfCnpj").value("12345678909"))
              .andExpect(jsonPath("$.externalReference").value("co-1"))
              .andRespond(withSuccess("{\"id\":\"cus_1\"}", MediaType.APPLICATION_JSON));
        assertEquals("cus_1", client.createCustomer("Clínica", "123.456.789-09", "a@b.com", "co-1"));
    }

    @Test
    void missingIdInResponseIsRejected() {
        server.expect(requestTo(BASE + "/customers"))
              .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(AsaasClient.AsaasException.class,
                () -> client.createCustomer("Clínica", "123.456.789-09", "a@b.com", "co-1"));
    }

    @Test
    void createSubscriptionIsMonthlyPixWithDecimalValue() {
        server.expect(requestTo(BASE + "/subscriptions"))
              .andExpect(jsonPath("$.customer").value("cus_1"))
              .andExpect(jsonPath("$.billingType").value("PIX"))
              .andExpect(jsonPath("$.cycle").value("MONTHLY"))
              .andExpect(jsonPath("$.value").value(197.00))
              .andExpect(jsonPath("$.nextDueDate").value("2026-11-15"))
              .andRespond(withSuccess("{\"id\":\"sub_1\"}", MediaType.APPLICATION_JSON));
        assertEquals("sub_1", client.createSubscription("cus_1", 19700, LocalDate.of(2026, 11, 15), "Plano Starter", "co-1"));
    }

    @Test
    void updateSubscriptionDoesNotTouchPendingPayments() {
        server.expect(requestTo(BASE + "/subscriptions/sub_1"))
              .andExpect(method(HttpMethod.PUT))
              .andExpect(jsonPath("$.value").value(49.90))
              .andExpect(jsonPath("$.updatePendingPayments").value(false))
              .andRespond(withSuccess("{\"id\":\"sub_1\"}", MediaType.APPLICATION_JSON));
        client.updateSubscriptionValue("sub_1", 4990, "Plano Essencial");
        server.verify();
    }

    @Test
    void getPaymentMapsFieldsAndCents() {
        server.expect(requestTo(BASE + "/payments/pay_1"))
              .andExpect(method(HttpMethod.GET))
              .andExpect(header("access_token", "KEY123"))
              .andRespond(withSuccess("""
                      {"id":"pay_1","subscription":"sub_1","externalReference":"co-1","status":"RECEIVED",
                       "dueDate":"2026-11-15","value":197.00,"invoiceUrl":"https://pix/1","paymentDate":"2026-11-14"}
                      """, MediaType.APPLICATION_JSON));
        AsaasClient.AsaasPayment p = client.getPayment("pay_1");
        assertEquals("sub_1", p.subscription());
        assertEquals("co-1", p.externalReference());
        assertEquals("RECEIVED", p.status());
        assertFalse(p.deleted());
        assertEquals(LocalDate.of(2026, 11, 15), p.dueDate());
        assertEquals(19700, p.valueCents());
        assertEquals("https://pix/1", p.invoiceUrl());
        assertEquals(LocalDate.of(2026, 11, 14), p.paymentDate());
    }

    @Test
    void getPaymentWithoutBodyIsAsaasError() {
        server.expect(requestTo(BASE + "/payments/pay_1"))
              .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThrows(AsaasClient.AsaasException.class, () -> client.getPayment("pay_1"));
    }

    @Test
    void subscriptionReference() {
        server.expect(requestTo(BASE + "/subscriptions/sub_1"))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess("{\"id\":\"sub_1\",\"externalReference\":\"co-1\"}", MediaType.APPLICATION_JSON));
        assertEquals("co-1", client.getSubscriptionReference("sub_1"));
    }

    @Test
    void errorNeverLeaksApiKey() {
        server.expect(requestTo(BASE + "/customers"))
              .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                      .body("{\"errors\":[{\"description\":\"CPF inválido\"}]}")
                      .contentType(MediaType.APPLICATION_JSON));
        var ex = assertThrows(AsaasClient.AsaasException.class,
                () -> client.createCustomer("C", "1", "a@b.com", "co-1"));
        assertTrue(ex.getMessage().contains("CPF inválido"));
        assertFalse(ex.toString().contains("KEY123"));
    }
}
