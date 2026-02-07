package kz.kaspi.lab.moderation.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kaspi.lab.moderation.model.AppealEvent;
import kz.kaspi.lab.moderation.model.ModerationResult;
import kz.kaspi.lab.moderation.producer.ModerationResultProducer;
import kz.kaspi.lab.moderation.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppealConsumer {

    private final ModerationService moderationService;
    private final ModerationResultProducer resultProducer;
    private final ObjectMapper objectMapper;

    // Потокобезопасное хранилище обработанных ID с персистентностью в файл
    private final IdempotencyStore idempotencyStore = new IdempotencyStore("processed_appeals.txt");

    @KafkaListener(
            topics = "${kafka.topics.appeals}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAppeal(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            log.info("Received message from topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());

            AppealEvent appeal = objectMapper.readValue(record.value(), AppealEvent.class);

            // Проверка идемпотентности — ключевая строка!
            if (idempotencyStore.isProcessed(appeal.getAppealId())) {
                log.warn("Appeal {} already processed (idempotency hit), skipping", appeal.getAppealId());
                acknowledgment.acknowledge();
                return;
            }

            // Обработка
            ModerationResult result = moderationService.moderateAppeal(appeal);

            // Публикация результата
            resultProducer.sendResult(result);

            // Сохраняем ID как обработанный (атомарно с подтверждением)
            idempotencyStore.markAsProcessed(appeal.getAppealId());

            // Подтверждаем обработку в Kafka
            acknowledgment.acknowledge();

            log.info("Successfully processed and acknowledged appealId={}", appeal.getAppealId());

        } catch (Exception e) {
            log.error("Error processing message from offset={}: {}", record.offset(), e.getMessage(), e);
            // Не подтверждаем сообщение — Kafka повторит доставку позже
        }
    }

    private static class IdempotencyStore {
        private final Path storageFile;
        private final Set<String> processedIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

        public IdempotencyStore(String filename) {
            this.storageFile = Path.of(filename);
            loadFromFile();
        }

        private void loadFromFile() {
            if (!Files.exists(storageFile)) {
                try {
                    Files.createFile(storageFile);
                    log.info("Created idempotency storage file: {}", storageFile);
                } catch (IOException e) {
                    log.error("Failed to create idempotency storage file", e);
                }
                return;
            }

            try (BufferedReader reader = Files.newBufferedReader(storageFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        processedIds.add(line.trim());
                    }
                }
                log.info("Loaded {} processed appeal IDs from file", processedIds.size());
            } catch (IOException e) {
                log.error("Failed to load processed IDs from file", e);
            }
        }

        public boolean isProcessed(String appealId) {
            return processedIds.contains(appealId);
        }

        public synchronized void markAsProcessed(String appealId) {
            if (processedIds.add(appealId)) {
                try {
                    Files.writeString(storageFile, appealId + System.lineSeparator(),
                            StandardOpenOption.APPEND);
                    log.debug("Persisted processed appeal ID: {}", appealId);
                } catch (IOException e) {
                    log.error("Failed to persist appeal ID {} to file", appealId, e);
                    // Не удаляем из памяти, даже если запись в файл не удалась
                    // При следующем перезапуске придётся обработать повторно, но это лучше потери данных
                }
            }
        }
    }
}