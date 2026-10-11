package com.becommerce.crm.finance.accounting.application.service;

import com.becommerce.crm.finance.accounting.application.dto.CsvImportRow;
import com.becommerce.crm.finance.accounting.application.dto.CsvLineError;
import com.becommerce.crm.finance.accounting.application.dto.ParsedCsv;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionCsvParserTest {

    private static final String HEADER =
            "Tipo;Descrição;Categoria;Valor;Forma de pagamento;Vencimento;Pago em;Paciente;Profissional;"
                    + "Observações";

    private final TransactionCsvParser parser = new TransactionCsvParser();

    @Test
    void parsesSemicolonSeparatedFileWithAllColumns() {
        String content = HEADER + "\n"
                + "Receita;Consulta de retorno;Procedimento;1.234,56;Pix;05/01/2026;05/01/2026;Ana Souza;"
                + "Dra. Maria;Retorno\n"
                + "Despesa;Aluguel da sala;Aluguel;5.000,00;Boleto;10/01/2026;;;;\n";

        ParsedCsv result = parser.parse(content);

        assertEquals(2, result.rows().size());
        assertEquals(0, result.errors().size());
        assertEquals(2, result.dataLineCount());
        CsvImportRow income = result.rows().get(0);
        assertEquals(TransactionType.INCOME, income.type());
        assertEquals(TransactionCategory.PROCEDURE, income.category());
        assertEquals(123456L, income.amountCents());
        assertEquals(PaymentMethod.PIX, income.paymentMethod());
        assertEquals(LocalDate.of(2026, 1, 5), income.dueDate());
        assertEquals(LocalDate.of(2026, 1, 5), income.paidAt());
        assertEquals("Ana Souza", income.patientName());
        assertEquals("Dra. Maria", income.professional());
        assertEquals("Retorno", income.notes());
        assertNotNull(income.paidAt());
        CsvImportRow expense = result.rows().get(1);
        assertEquals(TransactionType.EXPENSE, expense.type());
        assertEquals(500000L, expense.amountCents());
        assertEquals(PaymentMethod.BOLETO, expense.paymentMethod());
        assertNull(expense.paidAt());
    }

    @Test
    void stripsBomAndSupportsCommaSeparator() {
        String content = "\uFEFFtipo,descricao,categoria,valor,forma de pagamento,vencimento\n"
                + "Despesa,Aluguel da sala,Aluguel,\"1.234,56\",Boleto,10/01/2026\n";

        ParsedCsv result = parser.parse(content);

        assertEquals(1, result.rows().size());
        assertEquals(123456L, result.rows().get(0).amountCents());
        assertEquals(PaymentMethod.BOLETO, result.rows().get(0).paymentMethod());
    }

    @Test
    void parsesUsDecimalAndLiteralThousands() {
        assertEquals(123456L, parseAmountOnly("1234.56"));
        assertEquals(123400L, parseAmountOnly("1.234"));
        assertEquals(123456L, parseAmountOnly("1.234,56"));
        assertEquals(150L, parseAmountOnly("1,50"));
    }

    @Test
    void acceptsEnumCodeAndLabelForCategoryAndPayment() {
        String content = "Tipo;Descrição;Categoria;Valor;Forma de pagamento;Vencimento\n"
                + "INCOME;Rx;PRODUCT_SALE;99,90;CREDIT_CARD;05/02/2026\n"
                + "EXPENSE;Folha;Folha de pagamento;8.000,00;PIX;05/02/2026\n";

        ParsedCsv result = parser.parse(content);

        assertEquals(2, result.rows().size());
        assertEquals(TransactionCategory.PRODUCT_SALE, result.rows().get(0).category());
        assertEquals(TransactionCategory.PAYROLL, result.rows().get(1).category());
        assertEquals(PaymentMethod.CREDIT_CARD, result.rows().get(0).paymentMethod());
    }

    @Test
    void missingRequiredColumnInHeaderIsFatal() {
        String content = "Tipo;Descrição;Valor;Vencimento\nDespesa;Aluguel;5000;10/01/2026\n";

        FinancialTransactionValidationException ex = assertThrows(
                FinancialTransactionValidationException.class, () -> parser.parse(content));

        assertTrue(ex.getMessage().contains("categoria"));
        assertTrue(ex.getMessage().contains("forma de pagamento"));
    }

    @Test
    void invalidDateBecomesLineErrorButOthersParse() {
        String content = HEADER + "\n"
                + "Receita;Boa consulta;Consulta;200,00;Pix;99/99/2026;;;;\n"
                + "Despesa;Aluguel;Aluguel;5.000,00;Boleto;10/01/2026;;;;\n";

        ParsedCsv result = parser.parse(content);

        assertEquals(1, result.rows().size());
        assertEquals(1, result.errors().size());
        assertEquals(2, result.errors().get(0).line());
    }

    @Test
    void badLineDoesNotKillProcessing() {
        String content = HEADER + "\n"
                + "Receita;Consulta;Procedimento;200,00;Pix;05/01/2026;;;;\n"
                + "Despesa;;Aluguel errado;5.000,00;Boleto;10/01/2026;;;;\n"
                + "Despesa;Aluguel;Aluguel;5.000,00;Boleto;10/01/2026;;;;\n";

        ParsedCsv result = parser.parse(content);

        assertEquals(2, result.rows().size());
        assertEquals(1, result.errors().size());
        CsvLineError error = result.errors().get(0);
        assertEquals(3, error.line());
        assertEquals("Campo obrigatório ausente: descricao.", error.message());
    }

    @Test
    void columnCountMismatchBecomesLineError() {
        String content = HEADER + "\n"
                + "Despesa;Aluguel;Aluguel;5.000,00;Boleto;10/01/2026\n";

        ParsedCsv result = parser.parse(content);

        assertEquals(0, result.rows().size());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).message().contains("Número de colunas"));
    }

    @Test
    void categoryIncompatibleWithTypeBecomesLineError() {
        String content = HEADER + "\n"
                + "Receita;Aluguel recebido;Aluguel;5.000,00;Pix;10/01/2026;;;;\n";

        ParsedCsv result = parser.parse(content);

        assertEquals(0, result.rows().size());
        assertTrue(result.errors().get(0).message().contains("compatível"));
    }

    @Test
    void quotedDescriptionWithSeparatorIsKeptWhole() {
        String content = HEADER + "\n"
                + "Despesa;\"Taxa de cartao, maquininha\";Outras despesas;50,00;Pix;10/01/2026;;;;\n";

        ParsedCsv result = parser.parse(content);

        assertEquals(1, result.rows().size());
        assertEquals("Taxa de cartao, maquininha", result.rows().get(0).description());
    }

    @Test
    void moreThanTwoThousandDataLinesIsFatal() {
        StringBuilder csv = new StringBuilder(HEADER).append("\n");
        for (int i = 0; i < 2001; i++) {
            csv.append("Despesa;Item ").append(i).append(";Outras despesas;1,00;Pix;10/01/2026;;;\n");
        }

        assertThrows(FinancialTransactionValidationException.class, () -> parser.parse(csv.toString()));
    }

    @Test
    void zeroOrNegativeValueBecomesLineError() {
        String content = HEADER + "\n"
                + "Despesa;Aluguel;Aluguel;0;Boleto;10/01/2026;;;;\n";

        ParsedCsv result = parser.parse(content);

        assertEquals(0, result.rows().size());
        assertTrue(result.errors().get(0).message().contains("maior que zero"));
    }

    private long parseAmountOnly(String raw) {
        String content = HEADER + "\n"
                + "Despesa;Teste;Outras despesas;" + raw + ";Pix;10/01/2026;;;;\n";
        ParsedCsv result = parser.parse(content);
        assertEquals(1, result.rows().size());
        return result.rows().get(0).amountCents();
    }
}