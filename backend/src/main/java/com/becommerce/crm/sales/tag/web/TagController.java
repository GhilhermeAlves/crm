package com.becommerce.crm.sales.tag.web;

import com.becommerce.crm.sales.tag.application.dto.CreateTagRequest;
import com.becommerce.crm.sales.tag.application.dto.TagResponse;
import com.becommerce.crm.sales.tag.application.dto.UpdateTagRequest;
import com.becommerce.crm.sales.tag.application.port.in.TagUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/tags")
public class TagController {

    private final TagUseCase useCase;

    public TagController(TagUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<List<TagResponse>> list(
            @CurrentCompanyId("Você só pode acessar os marcadores da sua própria empresa.") UUID companyId) {
        return ResponseEntity.ok(useCase.listByCompany(companyId));
    }

    @GetMapping("/{tagId}")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<TagResponse> getById(
            @CurrentCompanyId("Você só pode acessar os marcadores da sua própria empresa.") UUID companyId,
            @PathVariable UUID tagId) {
        return ResponseEntity.ok(useCase.getById(companyId, tagId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<TagResponse> create(
            @CurrentCompanyId("Você só pode acessar os marcadores da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(companyId, request));
    }

    @PutMapping("/{tagId}")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<TagResponse> update(
            @CurrentCompanyId("Você só pode acessar os marcadores da sua própria empresa.") UUID companyId,
            @PathVariable UUID tagId,
            @Valid @RequestBody UpdateTagRequest request) {
        return ResponseEntity.ok(useCase.update(companyId, tagId, request));
    }

    @DeleteMapping("/{tagId}")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar os marcadores da sua própria empresa.") UUID companyId,
            @PathVariable UUID tagId) {
        useCase.delete(companyId, tagId);
        return ResponseEntity.noContent().build();
    }
}
