package com.becommerce.crm.finance.accounting.web;

import com.becommerce.crm.finance.accounting.application.dto.CsvImportResult;
import com.becommerce.crm.finance.accounting.application.service.TransactionImportService;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Importação de lançamentos via CSV (Etapa 3). Rotas adicionais de
 * {@code /finance/transactions}: {@code POST /import} (exige
 * {@code finance:manage}) e {@code GET /import/template} (modelo de download).
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/finance/transactions")
public class TransactionCsvImportController {

    private static final String OWN_COMPANY =
            "Você só pode acessar os lançamentos financeiros da sua própria empresa.";
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final String TEMPLATE_NAME = "modelo-lancamentos.csv";
    private static final String TEMPLATE_HEADER =
            "Tipo;Descrição;Categoria;Valor;Forma de pagamento;Vencimento;Pago em;Paciente;Profissional;Observações";

    private final TransactionImportService service;

    public TransactionCsvImportController(TransactionImportService service) {
        this.service = service;
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('finance:manage')")
    public ResponseEntity<CsvImportResult> importCsv(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CurrentUser principal) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new FinancialTransactionValidationException("Selecione um arquivo CSV.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new FinancialTransactionValidationException("O arquivo deve ter no máximo 5 MB.");
        }
        CsvImportResult result = service.importTransactions(companyId, file.getBytes(),
                principal.userId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<byte[]> template(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        byte[] body = ("\uFEFF" + TEMPLATE_HEADER).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + TEMPLATE_NAME + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }
}