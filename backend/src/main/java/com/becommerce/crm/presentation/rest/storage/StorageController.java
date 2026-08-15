package com.becommerce.crm.presentation.rest.storage;

import com.becommerce.crm.application.storage.dto.StorageDownload;
import com.becommerce.crm.application.storage.dto.StorageResponse;
import com.becommerce.crm.application.storage.port.input.StorageUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Armazenamento (Sprint 8.6). Upload, listagem, download e exclusão restritos à
 * própria empresa; a quota {@code max_storage_mb} é validada antes de gravar
 * (422 QUOTA_EXCEEDED). O isolamento cross-tenant é garantido por
 * {@link #requireCompanyAccess} + RLS FORCE na tabela {@code storage_objects}.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/storage")
public class StorageController {

    private final StorageUseCase storageUseCase;

    public StorageController(StorageUseCase storageUseCase) {
        this.storageUseCase = storageUseCase;
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StorageResponse> upload(@PathVariable UUID companyId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @AuthenticationPrincipal CurrentUser principal) throws IOException {
        requireCompanyAccess(companyId, principal);
        StorageResponse response = storageUseCase.upload(
                companyId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes(),
                principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StorageResponse>> list(@PathVariable UUID companyId,
                                                      @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(storageUseCase.list(companyId));
    }

    @GetMapping("/{objectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> download(@PathVariable UUID companyId,
                                           @PathVariable UUID objectId,
                                           @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        StorageDownload download = storageUseCase.download(companyId, objectId);
        String contentType = download.contentType() == null || download.contentType().isBlank()
                ? "application/octet-stream"
                : download.contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.fileName() + "\"")
                .body(download.data());
    }

    @DeleteMapping("/{objectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID companyId,
                                       @PathVariable UUID objectId,
                                       @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        storageUseCase.delete(companyId, objectId);
        return ResponseEntity.noContent().build();
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode enviar arquivos para a sua própria empresa.");
        }
    }
}