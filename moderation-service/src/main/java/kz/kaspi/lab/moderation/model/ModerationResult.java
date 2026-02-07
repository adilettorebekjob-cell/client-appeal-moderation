package kz.kaspi.lab.moderation.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {
    private String appealId;
    private String clientId;
    private ModerationDecision decision;
    private String reason;
    private String riskCategory;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;

    private AppealEvent originalAppeal;

    public enum ModerationDecision {
        APPROVED,
        REJECTED,
        REVIEW_REQUIRED
    }
}