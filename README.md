# Client Appeal Moderation Service

Event-driven moderation service with Kafka. Two independent Spring Boot apps:

- `enrichment-service` — mock external API for client data enrichment (`:8081`)
- `moderation-service` — core moderation logic with idempotency and fallback (`:8080`)

## Quick start

```bash
# 1. Start Kafka
docker-compose up -d

# 2. Run services (in separate terminals)
mvn spring-boot:run -f enrichment-service/pom.xml
mvn spring-boot:run -f moderation-service/pom.xml

# 3. Test
docker exec -it moderation-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 --topic appeals-topic

# Paste valid JSON:
{"appealId":"test","clientId":"user123","message":"help","timestamp":"2026-02-08T15:00:00","category":"QUESTION","priority":"NORMAL"}

appeals-topic → [Moderation Service] → approved-topic (APPROVED)
                                     → review-topic   (REJECTED / REVIEW_REQUIRED)
                                          ↑
                                  [Enrichment Service] (HTTP fallback on failure)

Topics
appeals-topic - Input appeals
approved-topic - Approved appeals (go to main processing)
review-topic - Rejected / need manual review

Test scenarios
REJECTED - fraudScore > 0.8
APPROVED - VIP client
REVIEW_REQUIRED - >2 previous complaints
APPROVED - Urgent priority

Kafka UI: http://localhost:8090

Оба сервиса — независимые Spring Boot приложения. Запускаются отдельно, общаются через:
- Kafka (appeals-topic → approved-topic/review-topic)
- HTTP (moderation → enrichment-service:8081)
