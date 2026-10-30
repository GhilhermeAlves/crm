-- V121__finance_detailed_categories.sql
-- Categorias detalhadas de lançamento (impostos, financeiras, pessoal,
-- administrativas, clínicas), para a DRE sair mais precisa. Os códigos antigos
-- continuam válidos; só o CHECK é ampliado.

ALTER TABLE financial_transactions DROP CONSTRAINT chk_financial_transactions_category;
ALTER TABLE financial_transactions ADD CONSTRAINT chk_financial_transactions_category CHECK (category IN
        ('PROCEDURE', 'CONSULTATION', 'PRODUCT_SALE', 'INSURANCE_TRANSFER', 'FINANCIAL_INCOME',
         'OTHER_INCOME', 'TAX_DAS', 'TAX_ISS', 'TAX_IRPJ', 'TAX_CSLL',
         'TAX_INSS', 'TAX_PIS_COFINS', 'TAXES', 'BANK_FEES', 'INTEREST_FINES',
         'CARD_FEES', 'CREDIT_CHECK', 'PAYROLL', 'PRO_LABORE', 'COMMISSION',
         'LABOR_CHARGES', 'MEAL_VOUCHER', 'TRANSPORT_VOUCHER', 'THIRTEENTH_SALARY', 'VACATION',
         'ACCOUNTING', 'RENT', 'CONDO_FEE', 'PROPERTY_TAX', 'ELECTRICITY',
         'WATER', 'GAS', 'INTERNET', 'POSTAGE', 'SECURITY',
         'INSURANCE', 'OFFICE_SUPPLIES', 'KITCHEN_SUPPLIES', 'CLEANING_SUPPLIES', 'EQUIPMENT',
         'PROFESSIONAL_COUNCIL', 'UTILITIES', 'MARKETING', 'SUPPLIES', 'LAB',
         'PROSTHETIST', 'OTHER_EXPENSE'));
