package kz.kaspi.lab.moderation.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kaspi.lab.moderation.model.ModerationResult;
import kz.kaspi.lab.moderation.model.ModerationResult.ModerationDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationResultProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.approved}")
    private String approvedTopic;

    @Value("${kafka.topics.review}")
    private String reviewTopic;

    public void sendResult(ModerationResult result) {
        String targetTopic = determineTargetTopic(result.getDecision());

        log.info("Sending result for appealId={} to topic={}, decision={}",
                result.getAppealId(), targetTopic, result.getDecision());

        try {
            String jsonPayload = objectMapper.writeValueAsString(result);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(targetTopic, result.getAppealId(), jsonPayload);

            future.whenComplete((sendResult, exception) -> {
                if (exception == null) {
                    log.info("Successfully sent message to topic={}, partition={}, offset={}",
                            targetTopic,
                            sendResult.getRecordMetadata().partition(),
                            sendResult.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send message to topic={} for appealId={}: {}",
                            targetTopic, result.getAppealId(), exception.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ModerationResult for appealId={}: {}",
                    result.getAppealId(), e.getMessage());
        }
    }

    private String determineTargetTopic(ModerationDecision decision) {
        return decision == ModerationDecision.APPROVED ? approvedTopic : reviewTopic;
    }
}