package kz.kaspi.lab.moderation.service;

import kz.kaspi.lab.moderation.client.EnrichmentClient;
import kz.kaspi.lab.moderation.model.AppealEvent;
import kz.kaspi.lab.moderation.model.EnrichmentData;
import kz.kaspi.lab.moderation.model.ModerationResult;
import kz.kaspi.lab.moderation.model.ModerationResult.ModerationDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final EnrichmentClient enrichmentClient;

    private static final Set<String> COMPLAINT_KEYWORDS = Set.of(
            "problem", "issue", "complaint", "не работает", "ошибка", "неправильно"
    );

    public ModerationResult moderateAppeal(AppealEvent appeal) {
        log.info("Starting moderation for appealId={}, clientId={}",
                appeal.getAppealId(), appeal.getClientId());

        // 1. Обогащение данных
        EnrichmentData enrichment = enrichmentClient.getEnrichment(appeal.getClientId());

        // 2. Применение бизнес-правил
        ModerationDecision decision = applyBusinessRules(appeal, enrichment);
        String reason = buildReason(decision, appeal, enrichment);

        // 3. Формирование результата
        ModerationResult result = ModerationResult.builder()
                .appealId(appeal.getAppealId())
                .clientId(appeal.getClientId())
                .decision(decision)
                .reason(reason)
                .riskCategory(enrichment.getRiskCategory())
                .processedAt(LocalDateTime.now())
                .originalAppeal(appeal)
                .build();

        log.info("Moderation completed for appealId={}: decision={}, reason={}",
                appeal.getAppealId(), decision, reason);

        return result;
    }

    private ModerationDecision applyBusinessRules(AppealEvent appeal, EnrichmentData enrichment) {
        // Правило 1: Критический фрод-риск → REJECTED
        if (enrichment.getFraudScore() > 0.8) {
            log.warn("High fraud score detected: {}", enrichment.getFraudScore());
            return ModerationDecision.REJECTED;
        }

        // Правило 2: VIP клиент → APPROVED (приоритет)
        if (Boolean.TRUE.equals(enrichment.getIsVIP())) {
            log.info("VIP client detected, auto-approving");
            return ModerationDecision.APPROVED;
        }

        // Правило 3: Urgent priority → APPROVED
        if ("URGENT".equals(appeal.getPriority())) {
            log.info("Urgent priority appeal, auto-approving");
            return ModerationDecision.APPROVED;
        }

        // Правило 4: Низкий рейтинг + жалоба → REVIEW_REQUIRED
        if (enrichment.getSupportRating() < 2.5 && isComplaint(appeal.getMessage())) {
            log.info("Low rating client with complaint, requires manual review");
            return ModerationDecision.REVIEW_REQUIRED;
        }

        // Правило 5: Множественные жалобы в истории → REVIEW_REQUIRED
        if (enrichment.getPreviousComplaints() > 2) {
            log.info("Multiple previous complaints detected: {}", enrichment.getPreviousComplaints());
            return ModerationDecision.REVIEW_REQUIRED;
        }

        // Правило 6: Высокий/критический риск → REVIEW_REQUIRED
        if ("HIGH".equals(enrichment.getRiskCategory()) ||
                "CRITICAL".equals(enrichment.getRiskCategory())) {
            log.info("High risk category detected: {}", enrichment.getRiskCategory());
            return ModerationDecision.REVIEW_REQUIRED;
        }

        // По умолчанию: APPROVED
        return ModerationDecision.APPROVED;
    }

    private boolean isComplaint(String message) {
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();
        return COMPLAINT_KEYWORDS.stream().anyMatch(lowerMessage::contains);
    }

    private String buildReason(ModerationDecision decision, AppealEvent appeal, EnrichmentData enrichment) {
        return switch (decision) {
            case APPROVED -> {
                if (Boolean.TRUE.equals(enrichment.getIsVIP())) {
                    yield "VIP client - priority handling";
                } else if ("URGENT".equals(appeal.getPriority())) {
                    yield "Urgent priority appeal";
                } else {
                    yield "Standard processing - all checks passed";
                }
            }
            case REJECTED -> String.format("High fraud risk (score: %.2f)", enrichment.getFraudScore());
            case REVIEW_REQUIRED -> {
                if (enrichment.getSupportRating() < 2.5) {
                    yield String.format("Low rating (%.1f) with complaint", enrichment.getSupportRating());
                } else if (enrichment.getPreviousComplaints() > 2) {
                    yield String.format("Multiple previous complaints (%d)", enrichment.getPreviousComplaints());
                } else {
                    yield String.format("High risk category: %s", enrichment.getRiskCategory());
                }
            }
        };
    }
}