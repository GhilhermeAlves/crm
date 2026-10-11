package com.becommerce.crm.finance.report.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Visão geral da agenda + financeiro do período:
 * <ul>
 *   <li>professionals: profissionais considerados (hostes com agenda no período ou
 *       os selecionados em professionalUserIds);</li>
 *   <li>totals: agendamentos (sem CANCELADOS), taxas de confirmação e de
 *       atendimento em % (0-100, 1 casa decimal), procedimentos concluídos e
 *       receita total;</li>
 *   <li>appointmentsByAttendance: por período — attended = COMPLETED,
 *       notAttended = NO_SHOW, pending = SCHEDULED + CONFIRMED;</li>
 *   <li>confirmed/attended: taxas por período;</li>
 *   <li>procedures: lançamentos INCOME com catalog_item_id (done = PAID,
 *       pending = PENDING);</li>
 *   <li>revenue: receitas PAID por período (paid_at).</li>
 * </ul>
 */
public record OverviewResponse(
        List<ProfessionalItem> professionals,
        OverviewTotals totals,
        List<AttendanceItem> appointmentsByAttendance,
        List<RateItem> confirmed,
        List<RateItem> attended,
        List<ProceduresItem> procedures,
        List<RevenueItem> revenue) {

    public record ProfessionalItem(UUID id, String name) {}

    public record OverviewTotals(long appointments, double confirmationRate,
                                 double attendanceRate, long proceduresDone, long revenueCents) {}

    public record AttendanceItem(String period, long attended, long notAttended, long pending) {}

    public record RateItem(String period, double rate) {}

    public record ProceduresItem(String period, long done, long pending) {}

    public record RevenueItem(String period, long amountCents) {}
}