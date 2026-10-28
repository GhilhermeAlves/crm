package com.becommerce.crm.masterdata.storage.web;

import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.masterdata.storage.application.service.PatientFileNotifier;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Arquivos do paciente (ficha do paciente, Fase 1). Lista e upload escopados ao
 * contato da própria empresa; download e exclusão continuam em
 * {@code /api/v1/companies/{companyId}/storage/{objectId}}.
 */
@RestController
@RequestMapping({"/api/v1/companies/{companyId}/contacts/{contactId}/files",
        "/api/v1/companies/{companyId}/patients/{contactId}/files"})
public class ContactFileController {

    private final StorageUseCase storageUseCase;
    private final PatientFileNotifier fileNotifier;

    public ContactFileController(StorageUseCase storageUseCase, PatientFileNotifier fileNotifier) {
        this.storageUseCase = storageUseCase;
        this.fileNotifier = fileNotifier;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<StorageResponse>> list(
            @CurrentCompanyId("Você só pode acessar arquivos da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId) {
        return ResponseEntity.ok(storageUseCase.listForContact(companyId, contactId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<StorageResponse> upload(
            @CurrentCompanyId("Você só pode enviar arquivos para a sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CurrentUser principal) throws IOException {
        StorageResponse response = storageUseCase.uploadForContact(
                companyId, contactId, file.getOriginalFilename(), file.getContentType(),
                file.getBytes(), principal.userId());
        fileNotifier.fileUploaded(companyId, contactId, response, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Pedido de download: o link vai para o sino de notificações de quem pediu. */
    @PostMapping("/{objectId}/notify")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<Void> notifyDownload(
            @CurrentCompanyId("Você só pode acessar arquivos da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID objectId,
            @AuthenticationPrincipal CurrentUser principal) {
        StorageResponse file = storageUseCase.listForContact(companyId, contactId).stream()
                .filter(f -> f.id().equals(objectId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado."));
        fileNotifier.downloadRequested(companyId, contactId, file, principal.userId());
        return ResponseEntity.accepted().build();
    }
}
