package com.becommerce.crm.reports.export.application.port.out;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Leituras (somente SELECT) que alimentam os relatórios exportados. Filtros
 * sempre preenchidos — nada de {@code :x IS NULL OR ...}, que quebra no
 * PostgreSQL por falta de tipo do parâmetro.
 */
public interface ReportDataGateway {

    /**
     * Teto de linhas por exportação. As consultas de detalhe trazem no máximo
     * {@code MAX_ROWS + 1} linhas, só para saber que o limite foi passado sem
     * carregar tudo na memória.
     */
    int MAX_ROWS = 50_000;

    /** Nome fantasia (ou razão social), CNPJ e logo da empresa, para o cabeçalho. */
    CompanyHeader companyHeader(UUID companyId);

    /** Lançamentos PAID com {@code paid_at} no período, somados por categoria. */
    List<CategoryTotalRow> paidByCategory(UUID companyId, LocalDate from, LocalDate to);

    /** Lançamentos PAID com {@code paid_at} no período, somados por dia e tipo. */
    List<DailyTotalRow> paidByDay(UUID companyId, LocalDate from, LocalDate to);

    /** Lançamentos não cancelados com vencimento no período. */
    List<TransactionRow> transactions(UUID companyId, LocalDate from, LocalDate to);

    /** Consultas com início (horário de Brasília) no período. */
    List<AppointmentRow> appointments(UUID companyId, LocalDate from, LocalDate to);

    /** Orçamentos criados (horário de Brasília) no período, com itens somados. */
    List<BudgetRow> budgets(UUID companyId, LocalDate from, LocalDate to);

    /** Leads criados (horário de Brasília) no período, cujo contato não foi excluído. */
    List<LeadRow> leads(UUID companyId, LocalDate from, LocalDate to);

    /** Contatos cadastrados (horário de Brasília) no período, sem os excluídos. */
    List<ContactRow> contacts(UUID companyId, LocalDate from, LocalDate to);

    /** Atividades com data (horário de Brasília) no período; sem descrição livre. */
    List<ActivityRow> activities(UUID companyId, LocalDate from, LocalDate to);

    /**
     * @param logoUrl logo de Configurações → Empresa: data URL (PNG/JPG embutido)
     *                ou link; só o data URL entra no PDF.
     */
    record CompanyHeader(String name, String cnpj, String logoUrl) {

        public CompanyHeader(String name, String cnpj) {
            this(name, cnpj, null);
        }
    }

    record CategoryTotalRow(TransactionCategory category, long count, long totalCents, long feeCents,
                            long feeCount) {}

    record DailyTotalRow(LocalDate date, TransactionType type, long totalCents) {}

    record TransactionRow(LocalDate dueDate, LocalDate paidAt, String description, String contactName,
                          TransactionCategory category, PaymentMethod paymentMethod, TransactionType type,
                          long amountCents, TransactionStatus status) {}

    record BudgetRow(LocalDate createdOn, String contactName, String title, String status, long items,
                     long subtotalCents, long discountCents, PaymentMethod paymentMethod, Integer installments,
                     LocalDate approvedOn) {

        public long totalCents() {
            return subtotalCents - discountCents;
        }
    }

    /** Status e origem chegam como texto do banco (LeadStatus/LeadSource). */
    record LeadRow(LocalDate createdOn, String name, String email, String phone, String source,
                   String status, String assignedName, int score) {}

    /** Só colunas em claro de contacts (nada do cadastro criptografado de persons). */
    /** Tipo chega como texto do banco (ActivityType); a descrição livre não entra (pode ter dado clínico). */
    record ActivityRow(LocalDate activityOn, String type, String subject, String contactName,
                       String createdByName) {}

    record ContactRow(LocalDate createdOn, String name, String document, String email, String phone,
                      String city, String state, LocalDate birthDate, String source, String tags) {}

    record AppointmentRow(Instant startAt, String contactName, String typeName, String hostName,
                          String status, String source) {}
}
