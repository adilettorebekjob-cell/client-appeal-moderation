package kz.kaspi.lab.moderation.client;

import kz.kaspi.lab.moderation.model.EnrichmentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
public class EnrichmentClient {

    private final WebClient webClient;
    private final int timeout;

    public EnrichmentClient(WebClient.Builder webClientBuilder,
                            @Value("${enrichment.service.url}") String baseUrl,
                            @Value("${enrichment.service.timeout}") int timeout) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.timeout = timeout;
    }

    public EnrichmentData getEnrichment(String clientId) {
        log.debug("Requesting enrichment for clientId: {}", clientId);

        try {
            return webClient.get()
                    .uri("/api/v1/clients/{clientId}/enrichment", clientId)
                    .retrieve()
                    .bodyToMono(EnrichmentData.class)
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(500))
                            .filter(this::isRetryableException))
                    .doOnError(e -> log.error("Failed to get enrichment for clientId={}: {}",
                            clientId, e.getMessage()))
                    .onErrorResume(this::handleFallback)
                    .block();
        } catch (Exception e) {
            log.error("Unexpected error calling enrichment service for clientId={}", clientId, e);
            return getFallbackEnrichment(clientId);
        }
    }

    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof WebClientResponseException wcre
                && (wcre.getStatusCode().is5xxServerError() || wcre.getStatusCode().value() == 429);
    }

    private Mono<EnrichmentData> handleFallback(Throwable throwable) {
        log.warn("Using fallback enrichment data due to: {}", throwable.getMessage());
        return Mono.just(getFallbackEnrichment("unknown"));
    }

    private EnrichmentData getFallbackEnrichment(String clientId) {
        return EnrichmentData.builder()
                .clientId(clientId)
                .fraudScore(0.5)
                .supportRating(3.0)
                .isVIP(false)
                .previousComplaints(0)
                .riskCategory("MEDIUM")
                .lastInteractionTimestamp(System.currentTimeMillis())
                .build();
    }
}