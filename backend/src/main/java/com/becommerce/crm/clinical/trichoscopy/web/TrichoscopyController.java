package com.becommerce.crm.clinical.trichoscopy.web;

import com.becommerce.crm.clinical.trichoscopy.application.dto.TrichoscopyImageResponse;
import com.becommerce.crm.clinical.trichoscopy.application.port.in.TrichoscopyUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/patients/{contactId}/trichoscopy")
public class TrichoscopyController {

    private static final String OWN_COMPANY = "Você só pode acessar tricoscopia da sua própria empresa.";

    private final TrichoscopyUseCase useCase;

    public TrichoscopyController(TrichoscopyUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<TrichoscopyImageResponse> capture(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID contactId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            @AuthenticationPrincipal CurrentUser principal) throws IOException {
        TrichoscopyImageResponse response = useCase.capture(companyId, contactId, file.getOriginalFilename(),
                file.getContentType(), file.getBytes(), notes, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<TrichoscopyImageResponse>> list(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID contactId) {
        return ResponseEntity.ok(useCase.list(companyId, contactId));
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal CurrentUser principal) {
        useCase.delete(companyId, contactId, imageId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
