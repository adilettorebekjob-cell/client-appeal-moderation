package kz.kaspi.lab.enrichment.service;

import kz.kaspi.lab.enrichment.model.EnrichmentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Slf4j
@Service
public class EnrichmentService {

    private final Random random = new Random();

    @Cacheable(value = "clientEnrichment", key = "#clientId")
    public EnrichmentData enrichClient(String clientId) {
        log.debug("Enriching data for clientId: {}", clientId);

        // Имитация обращения к БД / внешнему API
        simulateLatency();

        // Генерация данных на основе clientId для предсказуемости
        int hashCode = Math.abs(clientId.hashCode());

        double fraudScore = (hashCode % 100) / 100.0;
        double supportRating = 1.0 + (hashCode % 40) / 10.0;
        boolean isVIP = hashCode % 10 == 0;
        int previousComplaints = hashCode % 5;

        String riskCategory = calculateRiskCategory(fraudScore, previousComplaints);

        EnrichmentData data = EnrichmentData.builder()
                .clientId(clientId)
                .fraudScore(fraudScore)
                .supportRating(supportRating)
                .isVIP(isVIP)
                .previousComplaints(previousComplaints)
                .riskCategory(riskCategory)
                .lastInteractionTimestamp(Instant.now().toEpochMilli())
                .build();

        log.info("Enrichment completed for clientId={}: fraudScore={}, rating={}, VIP={}, risk={}",
                clientId, fraudScore, supportRating, isVIP, riskCategory);

        return data;
    }

    private String calculateRiskCategory(double fraudScore, int complaints) {
        if (fraudScore > 0.8 || complaints > 3) {
            return "CRITICAL";
        } else if (fraudScore > 0.6 || complaints > 2) {
            return "HIGH";
        } else if (fraudScore > 0.4 || complaints > 1) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private void simulateLatency() {
        try {
            // Имитация сетевой задержки 50-150ms
            Thread.sleep(50 + random.nextInt(100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}