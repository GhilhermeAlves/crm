package com.becommerce.crm.finance.cardfee;

import com.becommerce.crm.finance.cardfee.CardFeeDtos.PresetRequest;
import com.becommerce.crm.finance.cardfee.CardFeeDtos.PresetResponse;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Modelos de taxa das maquininhas. Leitura dos ativos para qualquer usuário logado;
 * cadastro e edição só para o dono do CRM.
 */
@RestController
public class CardFeePresetController {

    private final CardFeePresetService service;

    public CardFeePresetController(CardFeePresetService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/card-fee-presets")
    public ResponseEntity<List<PresetResponse>> listActive(@AuthenticationPrincipal CurrentUser user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return ResponseEntity.ok(service.listActive());
    }

    @GetMapping("/api/v1/admin/card-fee-presets")
    public ResponseEntity<List<PresetResponse>> listAll(@AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping("/api/v1/admin/card-fee-presets")
    public ResponseEntity<PresetResponse> create(@AuthenticationPrincipal CurrentUser user,
                                                 @RequestBody PresetRequest request) {
        requireCrmAdmin(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/api/v1/admin/card-fee-presets/{id}")
    public ResponseEntity<PresetResponse> update(@AuthenticationPrincipal CurrentUser user,
                                                 @PathVariable UUID id, @RequestBody PresetRequest request) {
        requireCrmAdmin(user);
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/card-fee-presets/{id}")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        requireCrmAdmin(user);
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    private static void requireCrmAdmin(CurrentUser user) {
        if (user == null || !user.isPlatformAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o dono do CRM gerencia os modelos de taxa.");
        }
    }
}
