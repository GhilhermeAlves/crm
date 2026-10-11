package com.becommerce.crm.finance.accounting.application.service;

import com.becommerce.crm.finance.accounting.application.dto.CsvImportRow;
import com.becommerce.crm.finance.accounting.application.dto.CsvLineError;
import com.becommerce.crm.finance.accounting.application.dto.ParsedCsv;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser do CSV de importação de lançamentos (Etapa 3), escrito à mão (sem
 * biblioteca externa) com suporte a aspas, BOM e separador {@code ;} (preferido)
 * ou {@code ,}. Uma linha inválida não derruba as demais: ela vira um
 * {@link CsvLineError} e o processamento continua. Cabeçalho ausente/inválido
 * (faltando coluna obrigatória) é erro fatal (400).
 */
public final class TransactionCsvParser {

    private static final int MAX_DATA_LINES = 2000;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int HEADER_LINE = 1;

    private static final List<String> REQUIRED_HEADERS = List.of(
            "tipo", "descricao", "categoria", "valor", "forma de pagamento", "vencimento");

    /**
     * Parseia o conteúdo UTF-8 do arquivo. {@code content} pode vir sem BOM
     * (que também é aceito e removido).
     */
    public ParsedCsv parse(String content) {
        if (content == null || content.isBlank()) {
            throw new FinancialTransactionValidationException("O arquivo CSV está vazio.");
        }
        String text = stripBom(content).replace("\r\n", "\n");
        String[] lines = text.split("\n", -1);

        Header header = readHeader(lines);
        Map<String, Integer> column = header.column();
        char separator = header.separator();

        List<CsvImportRow> rows = new ArrayList<>();
        List<CsvLineError> errors = new ArrayList<>();
        int dataLineCount = 0;
        int headerIndex = header.lineIndex();
        for (int i = headerIndex + 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            if (++dataLineCount > MAX_DATA_LINES) {
                throw new FinancialTransactionValidationException(
                        "O arquivo pode ter no máximo " + MAX_DATA_LINES + " linhas de lançamento.");
            }
            int line = i + 1;
            try {
                rows.add(parseRow(lines[i], separator, column, line));
            } catch (FinancialTransactionValidationException ex) {
                errors.add(new CsvLineError(line, ex.getMessage()));
            }
        }
        return new ParsedCsv(rows, errors, dataLineCount);
    }

    private Header readHeader(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String headerLine = lines[i];
            char separator = detectSeparator(headerLine);
            String[] fields = split(headerLine, separator);
            Map<String, Integer> column = new HashMap<>();
            for (int c = 0; c < fields.length; c++) {
                column.putIfAbsent(FinanceLabels.normalize(fields[c]), c);
            }
            List<String> missing = new ArrayList<>();
            for (String required : REQUIRED_HEADERS) {
                if (!column.containsKey(required)) {
                    missing.add(required);
                }
            }
            if (!missing.isEmpty()) {
                throw new FinancialTransactionValidationException(
                        "Cabeçalho inválido. Colunas obrigatórias ausentes: " + String.join(", ", missing) + ".");
            }
            return new Header(column, separator, i);
        }
        throw new FinancialTransactionValidationException("O arquivo CSV está vazio.");
    }

    private static char detectSeparator(String headerLine) {
        int semicolons = countChar(headerLine, ';');
        int commas = countChar(headerLine, ',');
        if (semicolons > 0) {
            return ';';
        }
        if (commas > 0) {
            return ',';
        }
        throw new FinancialTransactionValidationException(
                "Cabeçalho inválido: não foi possível identificar o separador de colunas (use ';' ou ',').");
    }

    private static int countChar(String value, char target) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    private CsvImportRow parseRow(String line, char separator, Map<String, Integer> column, int lineNumber) {
        String[] fields = split(line, separator);
        int expected = column.size();
        if (fields.length != expected) {
            throw new FinancialTransactionValidationException(
                    "Número de colunas não confere (esperado " + expected + ", informado " + fields.length + ").");
        }
        String typeValue = required(fields, column, "tipo", lineNumber);
        String description = required(fields, column, "descricao", lineNumber);
        if (description.length() > 255) {
            throw new FinancialTransactionValidationException("A descrição deve ter no máximo 255 caracteres.");
        }
        String categoryValue = required(fields, column, "categoria", lineNumber);
        String amountValue = required(fields, column, "valor", lineNumber);
        String paymentValue = required(fields, column, "forma de pagamento", lineNumber);
        String dueValue = required(fields, column, "vencimento", lineNumber);

        TransactionType type = parseType(typeValue);
        TransactionCategory category = FinanceLabels.categoryByKey(categoryValue);
        if (category == null) {
            throw new FinancialTransactionValidationException("Categoria inválida: " + categoryValue + ".");
        }
        if (category.type() != type) {
            throw new FinancialTransactionValidationException(
                    "A categoria " + category + " não é compatível com o tipo " + type + ".");
        }
        PaymentMethod paymentMethod = FinanceLabels.paymentByKey(paymentValue);
        if (paymentMethod == null) {
            throw new FinancialTransactionValidationException(
                    "Forma de pagamento inválida: " + paymentValue + ".");
        }
        long amountCents = parseAmount(amountValue);
        LocalDate dueDate = parseDate(dueValue, "Vencimento");
        LocalDate paidAt = parseOptionalDate(fields, column, "pago em");
        String patient = value(fields, column, "paciente");
        String professional = value(fields, column, "profissional");
        String notes = value(fields, column, "observacoes");

        return new CsvImportRow(lineNumber, type, description, category, amountCents, paymentMethod, dueDate,
                paidAt, patient, professional, notes);
    }

    private static String required(String[] fields, Map<String, Integer> column, String name, int lineNumber) {
        String value = value(fields, column, name);
        if (value == null) {
            throw new FinancialTransactionValidationException("Campo obrigatório ausente: " + name + ".");
        }
        return value;
    }

    private static String value(String[] fields, Map<String, Integer> column, String name) {
        Integer index = column.get(name);
        if (index == null || index >= fields.length) {
            return null;
        }
        String value = fields[index].trim();
        return value.isEmpty() ? null : value;
    }

    private static LocalDate parseOptionalDate(String[] fields, Map<String, Integer> column, String name) {
        String raw = value(fields, column, name);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw, DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new FinancialTransactionValidationException(
                    "Data de pagamento (pago em) inválida: " + raw + ". Use o formato dd/MM/yyyy.");
        }
    }

    private static TransactionType parseType(String raw) {
        String key = FinanceLabels.normalize(raw);
        switch (key) {
            case "income":
            case "receita":
                return TransactionType.INCOME;
            case "expense":
            case "despesa":
                return TransactionType.EXPENSE;
            default:
                throw new FinancialTransactionValidationException("Tipo inválido: " + raw + ".");
        }
    }

    private static LocalDate parseDate(String raw, String name) {
        try {
            return LocalDate.parse(raw, DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new FinancialTransactionValidationException(
                    "Vencimento inválido: " + raw + ". Use o formato dd/MM/yyyy.");
        }
    }

    /**
     * Aceita {@code 1.234,56}, {@code 1234,56}, {@code 1234.56} (moeda US) e
     * {@code 1234} — sempre convertido para centavos ({@code long}).
     */
    private static long parseAmount(String raw) {
        String value = raw.replace("R$", "").replace("r$", "").replace(" ", "").trim();
        if (value.isEmpty() || !value.matches("[0-9.,]+")) {
            throw new FinancialTransactionValidationException("Valor inválido: " + raw + ".");
        }
        if (value.contains(",")) {
            return toCents(value.replace(".", "").replace(",", "."), raw);
        }
        if (value.contains(".")) {
            int lastDot = value.lastIndexOf('.');
            String decimals = value.substring(lastDot + 1);
            if (decimals.matches("[0-9]{1,2}") && value.substring(0, lastDot).matches("[0-9]+")) {
                return toCents(value, raw);
            }
            return toCents(value.replace(".", ""), raw);
        }
        return toCents(value, raw);
    }

    private static long toCents(String normalized, String raw) {
        if (!normalized.matches("[0-9]+(\\.[0-9]{1,2})?")) {
            throw new FinancialTransactionValidationException("Valor inválido: " + raw + ".");
        }
        long cents = new BigDecimal(normalized).movePointRight(2).longValueExact();
        if (cents <= 0) {
            throw new FinancialTransactionValidationException(
                    "Valor inválido: " + raw + ". O valor deve ser maior que zero.");
        }
        return cents;
    }

    private static String stripBom(String content) {
        if (content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
    }

    /** Divide respeitando aspas (valores podem conter separador entre aspas). */
    private static String[] split(String line, char separator) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == separator && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(String[]::new);
    }

    private record Header(Map<String, Integer> column, char separator, int lineIndex) {
    }
}