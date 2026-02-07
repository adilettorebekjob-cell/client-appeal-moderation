package kz.kaspi.lab.moderation.model;

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
    private Double fraudScore;
    private Double supportRating;
    private Boolean isVIP;
    private Integer previousComplaints;
    private String riskCategory;
    private Long lastInteractionTimestamp;
}