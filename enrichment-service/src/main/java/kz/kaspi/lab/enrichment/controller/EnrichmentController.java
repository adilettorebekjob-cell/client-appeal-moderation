package kz.kaspi.lab.enrichment.controller;

import jakarta.validation.constraints.NotBlank;
import kz.kaspi.lab.enrichment.model.EnrichmentData;
import kz.kaspi.lab.enrichment.service.EnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Validated
public class EnrichmentController {

    private final EnrichmentService enrichmentService;

    @GetMapping("/{clientId}/enrichment")
    public ResponseEntity<EnrichmentData> getEnrichment(@PathVariable @NotBlank String clientId) {

        log.info("Received enrichment request for clientId: {}", clientId);

        EnrichmentData data = enrichmentService.enrichClient(clientId);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Enrichment Service is UP");
    }
}