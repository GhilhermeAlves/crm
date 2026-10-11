package com.becommerce.crm.finance.accounting.application.service;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Rótulos pt-BR dos enums financeiros (categorias, formas de pagamento e status)
 * e formatos de exibição usados pelos relatórios contábeis (Etapa 3). Também
 * resolve "código do enum OU rótulo pt-BR" no CSV de importação, ignorando
 * caixa e acentos.
 */
public final class FinanceLabels {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_TIME =
            DateTimeFormatter.ofPattern("MMMM 'de' uuuu", PT_BR);

    private static final Map<TransactionCategory, String> CATEGORY_LABELS = buildCategoryLabels();
    private static final Map<PaymentMethod, String> PAYMENT_LABELS = buildPaymentLabels();
    private static final Map<String, TransactionCategory> CATEGORY_BY_KEY = buildCategoryKeys();
    private static final Map<String, PaymentMethod> PAYMENT_BY_KEY = buildPaymentKeys();

    private FinanceLabels() {
    }

    public static String categoryLabel(TransactionCategory category) {
        return CATEGORY_LABELS.get(category);
    }

    public static String paymentLabel(PaymentMethod paymentMethod) {
        return PAYMENT_LABELS.get(paymentMethod);
    }

    public static String statusLabel(TransactionStatus status, boolean overdue) {
        if (status == TransactionStatus.PENDING && overdue) {
            return "Atrasado";
        }
        return switch (status) {
            case PAID -> "Pago";
            case PENDING -> "Pendente";
            case CANCELLED -> "Cancelado";
        };
    }

    /**
     * Resolve categoria por código do enum (ex.: {@code PROCEDURE}) OU rótulo
     * pt-BR (ex.: {@code Procedimento}), ignorando caixa e acentos.
     */
    public static TransactionCategory categoryByKey(String value) {
        return CATEGORY_BY_KEY.get(normalize(value));
    }

    /**
     * Resolve forma de pagamento por código do enum (ex.: {@code PIX}) OU rótulo
     * pt-BR (ex.: {@code Pix}), ignorando caixa e acentos.
     */
    public static PaymentMethod paymentByKey(String value) {
        return PAYMENT_BY_KEY.get(normalize(value));
    }

    /** Minúsculas e sem acentos (NFD + remoção de diacríticos). */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    /** Formata em reais (pt-BR): {@code R$ 1.234,56}. */
    public static String formatReais(long cents) {
        return java.text.NumberFormat.getCurrencyInstance(PT_BR)
                .format(BigDecimal.valueOf(cents, 2));
    }

    /** Formata em centavos puro para CSV: {@code 1234,56} (negativo sai com sinal). */
    public static String formatCents(long cents) {
        String sign = cents < 0 ? "-" : "";
        return sign + String.format(Locale.ROOT, "%d,%02d", Math.abs(cents / 100),
                Math.abs(cents % 100));
    }

    /** Mês por extenso em pt-BR: {@code Setembro de 2026}. */
    public static String monthLabel(YearMonth month) {
        String label = MONTH_TIME.format(month);
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    /** Nome do mês por extenso em pt-BR (capitalizado): {@code Setembro}. */
    public static String monthNameLabel(YearMonth month) {
        String name = month.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** Data {@code dd/MM/yyyy}. */
    public static String dateLabel(LocalDate date) {
        return date == null ? "" : DATE_TIME.format(date);
    }

    private static Map<TransactionCategory, String> buildCategoryLabels() {
        Map<TransactionCategory, String> labels = new EnumMap<>(TransactionCategory.class);
        labels.put(TransactionCategory.PROCEDURE, "Procedimento");
        labels.put(TransactionCategory.CONSULTATION, "Consulta");
        labels.put(TransactionCategory.PRODUCT_SALE, "Venda de produto");
        labels.put(TransactionCategory.OTHER_INCOME, "Outras receitas");
        labels.put(TransactionCategory.RENT, "Aluguel");
        labels.put(TransactionCategory.PAYROLL, "Folha de pagamento");
        labels.put(TransactionCategory.SUPPLIES, "Materiais");
        labels.put(TransactionCategory.LAB, "Laboratório");
        labels.put(TransactionCategory.TAXES, "Impostos");
        labels.put(TransactionCategory.MARKETING, "Marketing");
        labels.put(TransactionCategory.UTILITIES, "Contas de consumo");
        labels.put(TransactionCategory.EQUIPMENT, "Equipamentos");
        labels.put(TransactionCategory.OTHER_EXPENSE, "Outras despesas");
        return Map.copyOf(labels);
    }

    private static Map<PaymentMethod, String> buildPaymentLabels() {
        Map<PaymentMethod, String> labels = new EnumMap<>(PaymentMethod.class);
        labels.put(PaymentMethod.BOLETO, "Boleto");
        labels.put(PaymentMethod.CREDIT_CARD, "Cartão de crédito");
        labels.put(PaymentMethod.DEBIT_CARD, "Cartão de débito");
        labels.put(PaymentMethod.PIX, "Pix");
        labels.put(PaymentMethod.CASH, "Dinheiro");
        labels.put(PaymentMethod.TRANSFER, "Transferência");
        labels.put(PaymentMethod.CHECK, "Cheque");
        labels.put(PaymentMethod.OTHER, "Outro");
        return Map.copyOf(labels);
    }

    private static Map<String, TransactionCategory> buildCategoryKeys() {
        Map<String, TransactionCategory> keys = new HashMap<>();
        for (TransactionCategory category : TransactionCategory.values()) {
            keys.put(normalize(category.name()), category);
            keys.put(normalize(CATEGORY_LABELS.get(category)), category);
        }
        return Map.copyOf(keys);
    }

    private static Map<String, PaymentMethod> buildPaymentKeys() {
        Map<String, PaymentMethod> keys = new HashMap<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            keys.put(normalize(method.name()), method);
            keys.put(normalize(PAYMENT_LABELS.get(method)), method);
        }
        return Map.copyOf(keys);
    }
}