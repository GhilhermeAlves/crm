package com.becommerce.crm.finance.cardfee;

import com.becommerce.crm.finance.cardfee.CardFeeDtos.RateItem;
import com.becommerce.crm.finance.cardfee.CardFeeDtos.SaveRatesRequest;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Taxas da maquininha da empresa, usadas pelo simulador de crédito. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/card-fees")
public class CardFeeController {

    private static final String ACCESS_MESSAGE =
            "Você só pode acessar as taxas do cartão da sua própria empresa.";

    private final CardFeeService service;

    public CardFeeController(CardFeeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<List<RateItem>> list(@CurrentCompanyId(ACCESS_MESSAGE) UUID companyId) {
        return ResponseEntity.ok(service.list(companyId));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('finance:manage')")
    public ResponseEntity<List<RateItem>> save(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @RequestBody SaveRatesRequest request) {
        return ResponseEntity.ok(service.save(companyId, request.rates()));
    }
}
