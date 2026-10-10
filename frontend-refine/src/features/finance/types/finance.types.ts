export type TransactionType = "INCOME" | "EXPENSE";

export type TransactionStatus = "PENDING" | "PAID" | "CANCELLED";

/** O filtro de status aceita também o valor calculado "OVERDUE" (pendente vencido). */
export type TransactionStatusFilter = TransactionStatus | "OVERDUE";

export type PaymentMethod =
  "BOLETO" | "CREDIT_CARD" | "DEBIT_CARD" | "PIX" | "CASH" | "TRANSFER" | "CHECK" | "OTHER";

export type TransactionCategory =
  | "PROCEDURE"
  | "CONSULTATION"
  | "PRODUCT_SALE"
  | "OTHER_INCOME"
  | "RENT"
  | "PAYROLL"
  | "SUPPLIES"
  | "LAB"
  | "TAXES"
  | "MARKETING"
  | "UTILITIES"
  | "EQUIPMENT"
  | "OTHER_EXPENSE";

export type ReportGranularity = "DAY" | "WEEK" | "MONTH";

export type AccountingReportType = "MONTHLY_ACCOUNTING" | "STATEMENT" | "TRANSACTIONS";

export type ReportFormat = "PDF" | "CSV";

export type PageResponse<T> = {
  content: T[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type FinancialTransaction = {
  id: string;
  companyId: string;
  type: TransactionType;
  description: string;
  category: TransactionCategory;
  amountCents: number;
  feeCents: number;
  paymentMethod: PaymentMethod;
  dueDate: string;
  paidAt: string | null;
  status: TransactionStatus;
  contactId: string | null;
  contactName: string | null;
  professionalUserId: string | null;
  professionalName: string | null;
  catalogItemId: string | null;
  catalogItemName: string | null;
  installmentGroupId: string | null;
  installmentNumber: number | null;
  installmentTotal: number | null;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
  overdue: boolean;
};

export type CreateFinancialTransactionRequest = {
  type: TransactionType;
  description: string;
  category: TransactionCategory;
  amountCents: number;
  feeCents?: number;
  paymentMethod: PaymentMethod;
  dueDate: string;
  contactId?: string | null;
  professionalUserId?: string | null;
  catalogItemId?: string | null;
  notes?: string | null;
  paidAt?: string | null;
  installments?: number;
};

export type UpdateFinancialTransactionRequest = {
  type: TransactionType;
  description: string;
  category: TransactionCategory;
  amountCents: number;
  feeCents?: number;
  paymentMethod: PaymentMethod;
  dueDate: string;
  contactId?: string | null;
  professionalUserId?: string | null;
  catalogItemId?: string | null;
  notes?: string | null;
  paidAt?: string | null;
};

export type TransactionFilters = {
  type?: TransactionType;
  status?: TransactionStatusFilter;
  from?: string;
  to?: string;
  paymentMethod?: PaymentMethod;
  category?: TransactionCategory;
  professionalUserId?: string;
  contactId?: string;
  q?: string;
};

export type ListTransactionsFilters = TransactionFilters & {
  page?: number;
  pageSize?: number;
};

export type SummaryResponse = {
  incomeTotalCents: number;
  expenseTotalCents: number;
  incomeForecastCents: number;
  expenseForecastCents: number;
  balanceCents: number;
};

export type MonthHealth = {
  month: string;
  incomeCents: number;
  expenseCents: number;
  balanceCents: number;
};

export type HealthResponse = {
  months: MonthHealth[];
};

export type CashFlowSeriesItem = {
  period: string;
  incomeCents: number;
  openReceivablesCents: number;
  expenseCents: number;
  openPayablesCents: number;
  periodBalanceCents: number;
  accumulatedBalanceCents: number;
};

export type IncomeByMethod = {
  boletoCents: number;
  creditCardCents: number;
  debitCardCents: number;
  pixCents: number;
  cashCents: number;
  transferCents: number;
  checkCents: number;
  otherCents: number;
};

export type CashFlowHistoryItem = {
  period: string;
  forecastCents: number;
  incomeCents: number;
  defaultCents: number;
  openCents: number;
};

export type ExpenseHistoryItem = {
  period: string;
  expenseCents: number;
  openCents: number;
  defaultCents: number;
};

export type MonthAmounts = {
  paidCents: number;
  openCents: number;
  defaultCents: number;
};

export type CashFlowCurrentMonth = {
  income: MonthAmounts;
  expense: MonthAmounts;
};

export type CashFlowResponse = {
  series: CashFlowSeriesItem[];
  incomeByMethod: IncomeByMethod;
  incomeHistory: CashFlowHistoryItem[];
  expenseHistory: ExpenseHistoryItem[];
  currentMonth: CashFlowCurrentMonth;
};

export type RevenueByProcedureItem = {
  period: string;
  catalogItemId: string | null;
  name: string | null;
  amountCents: number;
};

export type RevenueAverageItem = {
  catalogItemId: string | null;
  name: string | null;
  averageCents: number;
  averageNetCents: number;
};

export type RevenueByProfessionalItem = {
  period: string;
  professionalUserId: string | null;
  name: string | null;
  amountCents: number;
};

export type RevenueDistributionResponse = {
  byProcedure: RevenueByProcedureItem[];
  averageByProcedure: RevenueAverageItem[];
  byProfessional: RevenueByProfessionalItem[];
};

export type OverviewProfessionalItem = {
  id: string;
  name: string;
};

export type OverviewTotals = {
  appointments: number;
  confirmationRate: number;
  attendanceRate: number;
  proceduresDone: number;
  revenueCents: number;
};

export type OverviewAttendanceItem = {
  period: string;
  attended: number;
  notAttended: number;
  pending: number;
};

export type OverviewRateItem = {
  period: string;
  rate: number;
};

export type OverviewProceduresItem = {
  period: string;
  done: number;
  pending: number;
};

export type OverviewRevenueItem = {
  period: string;
  amountCents: number;
};

export type OverviewResponse = {
  professionals: OverviewProfessionalItem[];
  totals: OverviewTotals;
  appointmentsByAttendance: OverviewAttendanceItem[];
  confirmed: OverviewRateItem[];
  attended: OverviewRateItem[];
  procedures: OverviewProceduresItem[];
  revenue: OverviewRevenueItem[];
};

export type AgendaReportStatusCountItem = {
  status: string;
  count: number;
};

export type AgendaReportResponse = {
  total: number;
  attended: number;
  notAttended: number;
  attendanceRate: number;
  byStatus: AgendaReportStatusCountItem[];
};

export type HeadlineResponse = {
  revenueThisMonthCents: number;
  attendancesThisWeek: number;
  budgetsCreatedThisMonth: number;
};

export type CsvLineError = {
  line: number;
  message: string;
};

export type CsvImportResult = {
  imported: number;
  errors: CsvLineError[];
};

export type AccountingReportRequest = {
  type: AccountingReportType;
  month: string;
  format: ReportFormat;
};

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  INCOME: "Receita",
  EXPENSE: "Despesa",
};

export const INCOME_CATEGORY_LABELS: Record<
  Extract<TransactionCategory, "PROCEDURE" | "CONSULTATION" | "PRODUCT_SALE" | "OTHER_INCOME">,
  string
> = {
  PROCEDURE: "Procedimento",
  CONSULTATION: "Consulta",
  PRODUCT_SALE: "Venda de produto",
  OTHER_INCOME: "Outras receitas",
};

export const EXPENSE_CATEGORY_LABELS: Record<
  Extract<
    TransactionCategory,
    | "RENT"
    | "PAYROLL"
    | "SUPPLIES"
    | "LAB"
    | "TAXES"
    | "MARKETING"
    | "UTILITIES"
    | "EQUIPMENT"
    | "OTHER_EXPENSE"
  >,
  string
> = {
  RENT: "Aluguel",
  PAYROLL: "Folha de pagamento",
  SUPPLIES: "Materiais",
  LAB: "Laboratório",
  TAXES: "Taxas",
  MARKETING: "Marketing",
  UTILITIES: "Utilidades",
  EQUIPMENT: "Equipamento",
  OTHER_EXPENSE: "Outras despesas",
};

export const TRANSACTION_CATEGORY_LABELS: Record<TransactionCategory, string> = {
  ...INCOME_CATEGORY_LABELS,
  ...EXPENSE_CATEGORY_LABELS,
};

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  BOLETO: "Boleto",
  CREDIT_CARD: "Cartão de crédito",
  DEBIT_CARD: "Cartão de débito",
  PIX: "Pix",
  CASH: "Dinheiro",
  TRANSFER: "Transferência",
  CHECK: "Cheque",
  OTHER: "Outro",
};

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatusFilter, string> = {
  PENDING: "Pendente",
  PAID: "Pago",
  CANCELLED: "Cancelado",
  OVERDUE: "Atrasado",
};

export const CATEGORIES_BY_TYPE: Record<TransactionType, TransactionCategory[]> = {
  INCOME: ["PROCEDURE", "CONSULTATION", "PRODUCT_SALE", "OTHER_INCOME"],
  EXPENSE: [
    "RENT",
    "PAYROLL",
    "SUPPLIES",
    "LAB",
    "TAXES",
    "MARKETING",
    "UTILITIES",
    "EQUIPMENT",
    "OTHER_EXPENSE",
  ],
};
