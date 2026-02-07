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
public class AppealEvent {
    private String appealId;
    private String clientId;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String category; // COMPLAINT, QUESTION, FEEDBACK
    private String priority; // LOW, NORMAL, HIGH, URGENT
}