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
        return Math.max(score, 0);
    }

    public ValidationStatus calculateStatus(int score, List<AiValidationIssue> issues) {
        long criticalCount = issues.stream().filter(i -> i.getSeverity() == IssueSeverity.CRITICAL).count();

        if (score >= 85 && criticalCount == 0) {
            return ValidationStatus.APPROVED;
        }
        if (score >= 60 && criticalCount < 3) {
            return ValidationStatus.APPROVED_WITH_WARNINGS;
        }
        return ValidationStatus.REJECTED;
    }
}
