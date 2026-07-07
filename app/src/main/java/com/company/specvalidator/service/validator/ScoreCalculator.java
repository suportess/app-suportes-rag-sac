package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.AiValidationIssue;
import com.company.specvalidator.enums.IssueSeverity;
import com.company.specvalidator.enums.ValidationStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoreCalculator {

    public int calculateScore(List<AiValidationIssue> issues) {
        int score = 100;
        for (AiValidationIssue issue : issues) {
            if (issue.getSeverity() == IssueSeverity.CRITICAL) {
                score -= 20;
            } else if (issue.getSeverity() == IssueSeverity.MODERATE) {
                score -= 10;
            } else if (issue.getSeverity() == IssueSeverity.MINOR) {
                score -= 3;
            }
        }
        return score; // sem floor — score pode ser negativo para EFs muito ruins
    }

    public ValidationStatus calculateStatus(int rawScore, List<AiValidationIssue> issues) {
        long criticalCount = issues.stream().filter(i -> i.getSeverity() == IssueSeverity.CRITICAL).count();

        if (rawScore >= 85 && criticalCount == 0) {
            return ValidationStatus.APPROVED;
        }
        if (criticalCount <= 3 && rawScore >= 30) {
            return ValidationStatus.APPROVED_WITH_WARNINGS;
        }
        return ValidationStatus.REJECTED;
    }

    public int normalizeScore(int rawScore, ValidationStatus status) {
        return switch (status) {
            case APPROVED -> rawScore;
            case APPROVED_WITH_WARNINGS -> Math.max(30, Math.min(84, rawScore));
            case REJECTED -> {
                // Mapeia lineamente [-100, 29] → [1, 29]
                // EF com poucas falhas rejeitadas (raw 29) → exibe 29
                // EF muito ruim (raw ≤ -100) → exibe 1
                int worstCase = -100;
                int bestRejected = 29;
                int clamped = Math.max(worstCase, Math.min(bestRejected, rawScore));
                yield 1 + (int) Math.round((double)(clamped - worstCase) / (bestRejected - worstCase) * 28);
            }
        };
    }
}
