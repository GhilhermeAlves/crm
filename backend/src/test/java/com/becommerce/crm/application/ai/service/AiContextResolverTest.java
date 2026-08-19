package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.context.AiRecordContext;
import com.becommerce.crm.application.ai.context.AiRecordContextResolver;
import com.becommerce.crm.application.ai.context.ContactContextResolver;
import com.becommerce.crm.application.ai.context.CustomerContextResolver;
import com.becommerce.crm.application.ai.context.Customer360ContextBuilder;
import com.becommerce.crm.application.ai.context.ResolvedAiContext;
import com.becommerce.crm.application.ai.dto.AiContextPayload;
import com.becommerce.crm.domain.ai.AiRecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiContextResolverTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();

    private Customer360ContextBuilder customer360Builder;
    private AiContextResolver resolver;
    private final List<String> readPermissions = List.of(
            "contact:read", "opportunity:read", "activity:read", "task:read");

    @BeforeEach
    void setUp() {
        customer360Builder = mock(Customer360ContextBuilder.class);
        when(customer360Builder.build(any(), any())).thenReturn("Cliente: João\nOportunidades abertas: 2");

        AiRecordContextResolverStub opportunity = stub(AiRecordType.OPPORTUNITY, "opportunity:read", "Oportunidade: Proposta A");
        AiRecordContextResolverStub activity = stub(AiRecordType.ACTIVITY, "activity:read", "Atividade (CALL): Ligar");
        AiRecordContextResolverStub task = stub(AiRecordType.TASK, "task:read", "Tarefa: Follow-up");

        resolver = new AiContextResolver(List.of(
                new CustomerContextResolver(customer360Builder),
                new ContactContextResolver(customer360Builder),
                opportunity, activity, task));
    }

    private static AiRecordContextResolverStub stub(AiRecordType type, String perm, String context) {
        AiRecordContextResolverStub s = new AiRecordContextResolverStub();
        s.type = type;
        s.permission = perm;
        s.context = context;
        return s;
    }

    private ResolvedAiContext resolve(AiContextPayload payload, List<String> permissions) {
        return resolver.resolve(companyId, userId, permissions, payload);
    }

    @Test
    void shouldResolveUserContextFromCurrentUser() {
        ResolvedAiContext c = resolve(new AiContextPayload("customer360", recordId), readPermissions);
        assertNotNull(c.user());
        assertEquals(userId, c.user().userId());
    }

    @Test
    void shouldResolveCompanyContextFromCurrentUser() {
        ResolvedAiContext c = resolve(new AiContextPayload("customer360", recordId), readPermissions);
        assertNotNull(c.company());
        assertEquals(companyId, c.company().companyId());
    }

    @Test
    void shouldResolvePermissionContextFromCurrentUser() {
        ResolvedAiContext c = resolve(new AiContextPayload("customer360", recordId), readPermissions);
        assertNotNull(c.permissions());
        assertTrue(c.permissions().has("contact:read"));
        assertFalse(c.permissions().has("settings:update"));
    }

    @Test
    void shouldResolveApplicationContextFromRoute() {
        ResolvedAiContext c = resolve(new AiContextPayload("customer360", "/contacts/xyz", "CUSTOMER", recordId),
                readPermissions);
        assertNotNull(c.application());
        assertEquals("/contacts/xyz", c.application().route());
        assertEquals("customer360", c.application().screen());
        assertEquals("CUSTOMER", c.application().module());
    }

    @Test
    void shouldResolveCustomerContext() {
        ResolvedAiContext c = resolve(
                new AiContextPayload("customer360", "/customers/xyz", "CUSTOMER", recordId),
                readPermissions);
        assertNotNull(c.record());
        assertEquals(AiRecordType.CUSTOMER, c.record().type());
        assertEquals(recordId, c.record().recordId());
        assertTrue(c.crmContext().contains("João"));
        verify(customer360Builder).build(companyId, recordId);
    }

    @Test
    void shouldResolveContactContext() {
        ResolvedAiContext c = resolve(
                new AiContextPayload("contact", "/contacts/xyz", "CONTACT", recordId),
                readPermissions);
        assertNotNull(c.record());
        assertEquals(AiRecordType.CONTACT, c.record().type());
        assertTrue(c.crmContext().contains("João"));
        verify(customer360Builder).build(companyId, recordId);
    }

    @Test
    void shouldResolveOpportunityContext() {
        ResolvedAiContext c = resolve(
                new AiContextPayload("opportunity", "/opportunities/xyz", "OPPORTUNITY", recordId),
                readPermissions);
        assertNotNull(c.record());
        assertEquals(AiRecordType.OPPORTUNITY, c.record().type());
        assertTrue(c.crmContext().contains("Proposta A"));
    }

    @Test
    void shouldResolveActivityContext() {
        ResolvedAiContext c = resolve(
                new AiContextPayload("activity", "/activities/xyz", "ACTIVITY", recordId),
                readPermissions);
        assertEquals(AiRecordType.ACTIVITY, c.record().type());
        assertTrue(c.crmContext().contains("Ligar"));
    }

    @Test
    void shouldResolveTaskContext() {
        ResolvedAiContext c = resolve(
                new AiContextPayload("task", "/tasks/xyz", "TASK", recordId),
                readPermissions);
        assertEquals(AiRecordType.TASK, c.record().type());
        assertTrue(c.crmContext().contains("Follow-up"));
    }

    @Test
    void shouldOmitRecordContextWhenPermissionMissing() {
        ResolvedAiContext c = resolve(
                new AiContextPayload("task", "/tasks/xyz", "TASK", recordId),
                List.of("contact:read"));
        assertNull(c.record());
        assertNull(c.crmContext());
        assertNotNull(c.application());
    }

    @Test
    void shouldNotResolveWhenNoRecordId() {
        ResolvedAiContext c = resolve(new AiContextPayload("customer360", "/customers/xyz", "CUSTOMER", null),
                readPermissions);
        assertNull(c.record());
        assertNull(c.crmContext());
        verify(customer360Builder, never()).build(any(), any());
    }

    @Test
    void shouldNotResolveWhenRecordTypeUnknown() {
        ResolvedAiContext c = resolve(
                new AiContextPayload("something", "/x", "UNKNOWN_TYPE", recordId),
                readPermissions);
        assertNull(c.record());
        assertNull(c.crmContext());
        verify(customer360Builder, never()).build(any(), any());
    }

    @Test
    void shouldNotResolveWhenNullContext() {
        ResolvedAiContext c = resolve(null, readPermissions);
        assertNotNull(c.user());
        assertNotNull(c.company());
        assertNotNull(c.permissions());
        assertNotNull(c.application());
        assertNull(c.record());
        assertNull(c.crmContext());
        verify(customer360Builder, never()).build(any(), any());
    }

    @Test
    void shouldNotExposeWhenResolverReturnsNull() {
        AiRecordContextResolverStub miss = new AiRecordContextResolverStub();
        miss.type = AiRecordType.ACTIVITY;
        miss.permission = "activity:read";
        miss.context = null;
        AiContextResolver local = new AiContextResolver(List.of(miss));

        ResolvedAiContext c = local.resolve(companyId, userId, readPermissions,
                new AiContextPayload("activity", "/activities/x", "ACTIVITY", recordId));
        assertNull(c.record());
        assertNull(c.crmContext());
    }

    private static class AiRecordContextResolverStub implements AiRecordContextResolver {
        private AiRecordType type;
        private String permission;
        private String context;

        @Override
        public AiRecordType type() {
            return type;
        }

        @Override
        public String requiredPermission() {
            return permission;
        }

        @Override
        public String resolve(UUID companyId, UUID recordId) {
            return context;
        }
    }
}