package com.becommerce.crm.finance.patientbilling;

import com.becommerce.crm.billing.application.BillingProperties;
import com.becommerce.crm.billing.application.BillingTenant;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.communication.omnichannel.application.service.WhatsAppConnectionService;
import com.becommerce.crm.finance.patientbilling.PatientChargeStore.PatientCharge;
import com.becommerce.crm.finance.transaction.application.dto.PayTransactionRequest;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cobrança de pacientes pelo Asaas (spec 2026-11-18). Sem @Transactional: as chamadas HTTP
 * ao Asaas não seguram transação; cada passo é gravado logo após dar certo.
 */
@Service
public class PatientBillingService {

    private static final Logger log = LoggerFactory.getLogger(PatientBillingService.class);
    static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    static final String REF_PREFIX = "tx:";

    public static final String PENDING = "PENDING";
    public static final String RECEIVED = "RECEIVED";
    public static final String RECEIVED_IN_CASH = "RECEIVED_IN_CASH";
    public static final String CANCELLED = "CANCELLED";
    public static final String FAILED = "FAILED";

    public record ChargeView(UUID transactionId, String status, String invoiceUrl, String error,
                             boolean whatsappSent, String whatsappError) {
        static ChargeView of(PatientCharge c, boolean sent, String waError) {
            return new ChargeView(c.transactionId(), c.status(), c.invoiceUrl(), c.error(), sent, waError);
        }
    }

    private final PatientChargeStore store;
    private final AsaasClient asaas;
    private final BillingProperties props;
    private final FinancialTransactionRepository transactions;
    private final FinancialTransactionService transactionService;
    private final ContactRepository contacts;
    private final WhatsAppConnectionService whatsapp;
    private final Clock clock;

    public PatientBillingService(PatientChargeStore store, AsaasClient asaas, BillingProperties props,
                                 FinancialTransactionRepository transactions,
                                 FinancialTransactionService transactionService, ContactRepository contacts,
                                 WhatsAppConnectionService whatsapp, Clock clock) {
        this.store = store;
        this.asaas = asaas;
        this.props = props;
        this.transactions = transactions;
        this.transactionService = transactionService;
        this.contacts = contacts;
        this.whatsapp = whatsapp;
        this.clock = clock;
    }

    public static String ref(UUID companyId, UUID transactionId) {
        return REF_PREFIX + companyId + ":" + transactionId;
    }

    public boolean enabled(UUID companyId) {
        return props.configured() && store.enabled(companyId);
    }

    public void setEnabled(UUID companyId, boolean on) {
        store.setEnabled(companyId, on);
    }

    public List<ChargeView> charges(UUID companyId, Collection<UUID> transactionIds) {
        return store.findAll(companyId, transactionIds).stream().map(c -> ChargeView.of(c, false, null)).toList();
    }

    /** Cria (ou devolve a já ativa) cobrança do lançamento e envia o link pelo WhatsApp. */
    public ChargeView charge(UUID companyId, UUID transactionId) {
        if (!enabled(companyId)) {
            throw new PatientBillingException("Cobrança de pacientes pelo Asaas não está ligada nesta empresa.");
        }
        FinancialTransaction tx = requireTransaction(companyId, transactionId);
        if (tx.getType() != TransactionType.INCOME || tx.getStatus() != TransactionStatus.PENDING) {
            throw new PatientBillingException("Só receitas pendentes podem ser cobradas pelo Asaas.");
        }
        if (tx.getContactId() == null) {
            throw new PatientBillingException("Vincule um paciente ao lançamento para cobrar pelo Asaas.");
        }
        Optional<PatientCharge> existing = store.find(companyId, transactionId);
        if (existing.isPresent() && PENDING.equals(existing.get().status())) {
            return ChargeView.of(existing.get(), false, null);
        }
        Contact contact = BillingTenant.run(companyId, () -> contacts.findById(tx.getContactId()))
                .orElseThrow(() -> new PatientBillingException("Paciente do lançamento não encontrado."));
        if (contact.getCpf() == null || contact.getCpf().isBlank()) {
            throw new PatientBillingException("Cadastre o CPF do paciente para cobrar pelo Asaas.");
        }

        PatientCharge charge;
        try {
            String customerId = store.customerId(companyId, contact.getId()).orElse(null);
            if (customerId == null) {
                customerId = asaas.createPatientCustomer(fullName(contact), contact.getCpf(), contact.getEmail(),
                        contact.getMobile(), contact.getId().toString());
                store.saveCustomer(companyId, contact.getId(), customerId);
            }
            LocalDate today = LocalDate.now(clock.withZone(ZONE));
            LocalDate due = tx.getDueDate() == null || tx.getDueDate().isBefore(today) ? today : tx.getDueDate();
            AsaasClient.CreatedPayment p = asaas.createPayment(customerId, tx.getAmountCents(), due,
                    tx.getDescription(), ref(companyId, transactionId));
            charge = new PatientCharge(transactionId, companyId, p.id(), p.invoiceUrl(), PENDING, null);
        } catch (AsaasClient.AsaasException e) {
            log.warn("[PATIENT-BILLING] cobrança do lançamento {} não criada: {}", transactionId, e.getMessage());
            charge = new PatientCharge(transactionId, companyId, null, null, FAILED, e.getMessage());
        }
        store.upsert(charge);
        if (!PENDING.equals(charge.status())) {
            return ChargeView.of(charge, false, null);
        }
        String waError = sendWhatsApp(companyId, contact, charge);
        return ChargeView.of(charge, waError == null, waError);
    }

    public ChargeView resendWhatsApp(UUID companyId, UUID transactionId) {
        PatientCharge charge = store.find(companyId, transactionId)
                .filter(c -> PENDING.equals(c.status()))
                .orElseThrow(() -> new PatientBillingException("Lançamento sem cobrança em aberto no Asaas."));
        FinancialTransaction tx = requireTransaction(companyId, transactionId);
        Contact contact = BillingTenant.run(companyId, () -> contacts.findById(tx.getContactId()))
                .orElseThrow(() -> new PatientBillingException("Paciente do lançamento não encontrado."));
        String waError = sendWhatsApp(companyId, contact, charge);
        return ChargeView.of(charge, waError == null, waError);
    }

    /** Baixa feita no CRM (recepção): o Asaas registra como recebido em dinheiro. */
    public void onPaidInClinic(UUID companyId, UUID transactionId) {
        store.find(companyId, transactionId).filter(c -> PENDING.equals(c.status())).ifPresent(c -> {
            FinancialTransaction tx = BillingTenant.run(companyId, () -> transactions.findById(transactionId)).orElse(null);
            LocalDate paidAt = tx != null && tx.getPaidAt() != null ? tx.getPaidAt() : LocalDate.now(clock.withZone(ZONE));
            long cents = tx != null ? tx.getAmountCents() : 0;
            try {
                asaas.receiveInCash(c.asaasPaymentId(), paidAt, cents);
                store.setStatus(companyId, transactionId, RECEIVED_IN_CASH, null);
            } catch (AsaasClient.AsaasException e) {
                log.warn("[PATIENT-BILLING] receiveInCash de {} falhou: {}", transactionId, e.getMessage());
                store.setStatus(companyId, transactionId, PENDING, "Baixa não enviada ao Asaas: " + e.getMessage());
            }
        });
    }

    /** Lançamento cancelado ou excluído no CRM: remove a cobrança no Asaas. */
    public void onCancelled(UUID companyId, UUID transactionId) {
        store.find(companyId, transactionId).filter(c -> PENDING.equals(c.status())).ifPresent(c -> {
            try {
                asaas.deletePayment(c.asaasPaymentId());
                store.setStatus(companyId, transactionId, CANCELLED, null);
            } catch (AsaasClient.AsaasException e) {
                log.warn("[PATIENT-BILLING] remoção da cobrança de {} falhou: {}", transactionId, e.getMessage());
                store.setStatus(companyId, transactionId, PENDING, "Cancelamento não enviado ao Asaas: " + e.getMessage());
            }
        });
    }

    /**
     * Pagamento vindo do webhook. Devolve false se não é cobrança de paciente (segue o fluxo da licença).
     * Marca a cobrança antes de baixar o lançamento, para o evento de baixa não chamar receiveInCash.
     */
    public boolean applyWebhook(AsaasClient.AsaasPayment p) {
        String ref = p.externalReference();
        if (ref == null || !ref.startsWith(REF_PREFIX)) return false;
        String[] parts = ref.substring(REF_PREFIX.length()).split(":");
        if (parts.length != 2) return true;
        UUID companyId;
        UUID transactionId;
        try {
            companyId = UUID.fromString(parts[0]);
            transactionId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            return true;
        }
        Optional<PatientCharge> charge = store.find(companyId, transactionId);
        if (charge.isEmpty() || !p.id().equals(charge.get().asaasPaymentId())) return true;
        String status = p.status() == null ? "" : p.status();
        if (p.deleted()) {
            store.setStatus(companyId, transactionId, CANCELLED, null);
            return true;
        }
        if (status.equals("RECEIVED") || status.equals("CONFIRMED")) {
            if (RECEIVED.equals(charge.get().status()) || RECEIVED_IN_CASH.equals(charge.get().status())) return true;
            store.setStatus(companyId, transactionId, RECEIVED, null);
            FinancialTransaction tx = BillingTenant.run(companyId, () -> transactions.findById(transactionId)).orElse(null);
            if (tx != null && tx.getStatus() == TransactionStatus.PENDING) {
                LocalDate paid = p.paymentDate() != null ? p.paymentDate() : LocalDate.now(clock.withZone(ZONE));
                transactionService.pay(companyId, transactionId, new PayTransactionRequest(paid), null);
            }
        }
        return true;
    }

    private String sendWhatsApp(UUID companyId, Contact contact, PatientCharge charge) {
        String phone = contact.getMobile() != null ? contact.getMobile() : contact.getPhone();
        String text = "Olá, " + contact.getFirstName() + "! Segue o link para pagamento: " + charge.invoiceUrl()
                + "\nVocê pode pagar com Pix, boleto ou cartão.";
        try {
            whatsapp.sendText(companyId, phone, text);
            return null;
        } catch (RuntimeException e) {
            log.info("[PATIENT-BILLING] WhatsApp do lançamento {} não enviado: {}", charge.transactionId(),
                    e.getMessage());
            return e.getMessage() == null ? "WhatsApp não enviado." : e.getMessage();
        }
    }

    private FinancialTransaction requireTransaction(UUID companyId, UUID transactionId) {
        FinancialTransaction tx = BillingTenant.run(companyId, () -> transactions.findById(transactionId))
                .orElseThrow(() -> new PatientBillingException("Lançamento não encontrado."));
        if (!tx.getCompanyId().equals(companyId)) throw new PatientBillingException("Lançamento não encontrado.");
        return tx;
    }

    private static String fullName(Contact c) {
        String last = c.getLastName();
        return last == null || last.isBlank() ? c.getFirstName() : c.getFirstName() + " " + last;
    }
}
