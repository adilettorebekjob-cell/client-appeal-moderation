package kz.kaspi.lab.enrichment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentData {
    private String clientId;
    private Double fraudScore;        // 0.0 - 1.0
    private Double supportRating;     // 1.0 - 5.0
    private Boolean isVIP;
    private Integer previousComplaints;
    private String riskCategory;      // LOW, MEDIUM, HIGH, CRITICAL
    private Long lastInteractionTimestamp;
}