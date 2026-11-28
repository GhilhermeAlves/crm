package com.becommerce.crm.clinical.trichology.web;

import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.VerifyView;
import com.becommerce.crm.clinical.trichology.application.service.PublicTrichologyService;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationRateLimitException;
import com.becommerce.crm.masterdata.contact.registration.infrastructure.rate.RegistrationRateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas sem login: quiz capilar pelo link (token) e verificação pública do
 * relatório pelo código. Limite de taxa por IP, como os demais links públicos.
 */
@RestController
@RequestMapping("/api/v1/public/trichology")
public class PublicTrichologyController {

    private final PublicTrichologyService service;
    private final RegistrationRateLimiter rateLimiter;

    public PublicTrichologyController(PublicTrichologyService service, RegistrationRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/verify/{code}")
    public VerifyView verify(@PathVariable String code,
                             @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        checkRate(realIp);
        return service.verify(code);
    }

    private void checkRate(String realIp) {
        String key = "trichology:" + (realIp != null && !realIp.isBlank() ? realIp : "unknown");
        if (!rateLimiter.tryAcquire(key)) {
            throw new RegistrationRateLimitException();
        }
    }
}
