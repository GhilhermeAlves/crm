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
  | "INSURANCE_TRANSFER"
  | "FINANCIAL_INCOME"
  | "OTHER_INCOME"
  | "TAX_DAS"
  | "TAX_ISS"
  | "TAX_IRPJ"
  | "TAX_CSLL"
  | "TAX_INSS"
  | "TAX_PIS_COFINS"
  | "TAXES"
  | "BANK_FEES"
  | "INTEREST_FINES"
  | "CARD_FEES"
  | "CREDIT_CHECK"
  | "PAYROLL"
  | "PRO_LABORE"
  | "COMMISSION"
  | "LABOR_CHARGES"
  | "MEAL_VOUCHER"
  | "TRANSPORT_VOUCHER"
  | "THIRTEENTH_SALARY"
  | "VACATION"
  | "ACCOUNTING"
  | "RENT"
  | "CONDO_FEE"
  | "PROPERTY_TAX"
  | "ELECTRICITY"
  | "WATER"
  | "GAS"
  | "INTERNET"
  | "POSTAGE"
  | "SECURITY"
  | "INSURANCE"
  | "OFFICE_SUPPLIES"
  | "KITCHEN_SUPPLIES"
  | "CLEANING_SUPPLIES"
  | "EQUIPMENT"
  | "PROFESSIONAL_COUNCIL"
  | "UTILITIES"
  | "MARKETING"
  | "SUPPLIES"
  | "LAB"
  | "PROSTHETIST"
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
  /** Repete o valor cheio por `installments` meses (aluguel, salário...). */
  recurring?: boolean;
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

export const TRANSACTION_CATEGORY_LABELS: Record<TransactionCategory, string> = {
  PROCEDURE: "Procedimento",
  CONSULTATION: "Consulta",
  PRODUCT_SALE: "Venda de produto",
  INSURANCE_TRANSFER: "Repasse do convênio",
  FINANCIAL_INCOME: "Rendimento financeiro",
  OTHER_INCOME: "Outras receitas",
  TAX_DAS: "Simples Nacional (DAS)",
  TAX_ISS: "ISS",
  TAX_IRPJ: "IRPJ",
  TAX_CSLL: "CSLL",
  TAX_INSS: "INSS / Previdência",
  TAX_PIS_COFINS: "PIS/COFINS",
  TAXES: "Outros impostos",
  BANK_FEES: "Tarifas bancárias",
  INTEREST_FINES: "Juros e multa",
  CARD_FEES: "Taxas de cartão",
  CREDIT_CHECK: "Consulta de crédito",
  PAYROLL: "Salários",
  PRO_LABORE: "Pró-labore",
  COMMISSION: "Comissão",
  LABOR_CHARGES: "Encargos trabalhistas",
  MEAL_VOUCHER: "Vale-refeição (VR)",
  TRANSPORT_VOUCHER: "Vale-transporte (VT)",
  THIRTEENTH_SALARY: "13º salário",
  VACATION: "Férias",
  ACCOUNTING: "Contabilidade",
  RENT: "Aluguel",
  CONDO_FEE: "Condomínio",
  PROPERTY_TAX: "IPTU",
  ELECTRICITY: "Luz",
  WATER: "Água",
  GAS: "Gás",
  INTERNET: "Internet e telefone",
  POSTAGE: "Correio",
  SECURITY: "Segurança",
  INSURANCE: "Seguro",
  OFFICE_SUPPLIES: "Material de escritório",
  KITCHEN_SUPPLIES: "Material de cozinha",
  CLEANING_SUPPLIES: "Material de limpeza",
  EQUIPMENT: "Manutenção de equipamentos",
  PROFESSIONAL_COUNCIL: "Conselho profissional (CRO, CRM...)",
  UTILITIES: "Outras contas de consumo",
  MARKETING: "Divulgação e marketing",
  SUPPLIES: "Materiais clínicos",
  LAB: "Laboratório",
  PROSTHETIST: "Protético",
  OTHER_EXPENSE: "Outras despesas",
};

/** Categorias agrupadas como no plano de contas (formulário e filtros). */
export const CATEGORY_GROUPS: {
  type: TransactionType;
  label: string;
  categories: TransactionCategory[];
}[] = [
  {
    type: "INCOME",
    label: "Receitas",
    categories: [
      "PROCEDURE",
      "CONSULTATION",
      "PRODUCT_SALE",
      "INSURANCE_TRANSFER",
      "FINANCIAL_INCOME",
      "OTHER_INCOME",
    ],
  },
  {
    type: "EXPENSE",
    label: "Impostos e taxas",
    categories: [
      "TAX_DAS",
      "TAX_ISS",
      "TAX_IRPJ",
      "TAX_CSLL",
      "TAX_INSS",
      "TAX_PIS_COFINS",
      "TAXES",
    ],
  },
  {
    type: "EXPENSE",
    label: "Financeiras",
    categories: ["BANK_FEES", "INTEREST_FINES", "CARD_FEES", "CREDIT_CHECK"],
  },
  {
    type: "EXPENSE",
    label: "Pessoal",
    categories: [
      "PAYROLL",
      "PRO_LABORE",
      "COMMISSION",
      "LABOR_CHARGES",
      "MEAL_VOUCHER",
      "TRANSPORT_VOUCHER",
      "THIRTEENTH_SALARY",
      "VACATION",
    ],
  },
  {
    type: "EXPENSE",
    label: "Administrativas",
    categories: [
      "ACCOUNTING",
      "RENT",
      "CONDO_FEE",
      "PROPERTY_TAX",
      "ELECTRICITY",
      "WATER",
      "GAS",
      "INTERNET",
      "POSTAGE",
      "SECURITY",
      "INSURANCE",
      "OFFICE_SUPPLIES",
      "KITCHEN_SUPPLIES",
      "CLEANING_SUPPLIES",
      "EQUIPMENT",
      "PROFESSIONAL_COUNCIL",
      "UTILITIES",
    ],
  },
  {
    type: "EXPENSE",
    label: "Divulgação e marketing",
    categories: ["MARKETING"],
  },
  {
    type: "EXPENSE",
    label: "Despesas clínicas",
    categories: ["SUPPLIES", "LAB", "PROSTHETIST"],
  },
  {
    type: "EXPENSE",
    label: "Outras",
    categories: ["OTHER_EXPENSE"],
  },
];

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
  INCOME: CATEGORY_GROUPS.filter((g) => g.type === "INCOME").flatMap((g) => g.categories),
  EXPENSE: CATEGORY_GROUPS.filter((g) => g.type === "EXPENSE").flatMap((g) => g.categories),
};
