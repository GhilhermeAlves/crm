package com.becommerce.crm.clinical.trichoscopy.live;

import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Tela ao vivo da tricoscopia. Abrir a sessão exige login; o envio e a leitura
 * dos quadros ficam em {@code /public/forms/live/...} (fora do gateway, que limita
 * requisições por minuto), protegidos pelas chaves aleatórias da sessão.
 */
@RestController
@RequestMapping("/api/v1")
public class LiveViewController {

    private static final String OWN_COMPANY = "Você só pode acessar tricoscopia da sua própria empresa.";
    private static final String SEQ_HEADER = "X-Frame-Seq";

    private final LiveViewRegistry registry;

    public LiveViewController(LiveViewRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/companies/{companyId}/trichoscopy/live")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<LiveViewRegistry.Tokens> open(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registry.open(companyId));
    }

    @PutMapping(value = "/public/forms/live/push/{publishToken}", consumes = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Void> push(@PathVariable String publishToken, @RequestBody byte[] jpeg) {
        try {
            return registry.push(publishToken, jpeg)
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.status(HttpStatus.GONE).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/public/forms/live/push/{publishToken}")
    public ResponseEntity<Void> close(@PathVariable String publishToken) {
        registry.close(publishToken);
        return ResponseEntity.noContent().build();
    }

    /** Último quadro; 204 se ainda não há quadro novo depois de {@code after}, 410 se a sessão acabou. */
    @GetMapping("/public/forms/live/view/{viewToken}")
    public ResponseEntity<byte[]> frame(@PathVariable String viewToken,
                                        @RequestParam(defaultValue = "0") long after) {
        return registry.latest(viewToken)
                .map(frame -> frame
                        .filter(f -> f.seq() > after)
                        .map(f -> ResponseEntity.ok()
                                .contentType(MediaType.IMAGE_JPEG)
                                .cacheControl(CacheControl.noStore())
                                .header(SEQ_HEADER, Long.toString(f.seq()))
                                .header("Access-Control-Expose-Headers", SEQ_HEADER)
                                .body(f.jpeg()))
                        .orElseGet(() -> ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.GONE).build());
    }
}
