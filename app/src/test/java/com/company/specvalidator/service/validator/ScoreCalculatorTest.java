package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.AiValidationIssue;
import com.company.specvalidator.enums.IssueSeverity;
import com.company.specvalidator.enums.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculatorTest {

    private ScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ScoreCalculator();
    }

    private AiValidationIssue issueWith(IssueSeverity severity) {
        return AiValidationIssue.builder()
                .severity(severity)
                .category("TEST")
                .title("Test issue")
                .description("Test description")
                .suggestion("Test suggestion")
                .build();
    }

    @Test
    void testPerfectScore() {
        int score = calculator.calculateScore(Collections.emptyList());
        assertEquals(100, score, "No issues should yield a perfect score of 100");
    }

    @Test
    void testCriticalIssueRemoves20Points() {
        List<AiValidationIssue> issues = List.of(issueWith(IssueSeverity.CRITICAL));
        int score = calculator.calculateScore(issues);
        assertEquals(80, score, "One CRITICAL issue should reduce score by 20");
    }

    @Test
    void testModerateIssueRemoves10Points() {
        List<AiValidationIssue> issues = List.of(issueWith(IssueSeverity.MODERATE));
        int score = calculator.calculateScore(issues);
        assertEquals(90, score, "One MODERATE issue should reduce score by 10");
    }

    @Test
    void testMinorIssueRemoves3Points() {
        List<AiValidationIssue> issues = List.of(issueWith(IssueSeverity.MINOR));
        int score = calculator.calculateScore(issues);
        assertEquals(97, score, "One MINOR issue should reduce score by 3");
    }

    @Test
    void testMinimumScoreIsZero() {
        // 6 CRITICAL issues = -120 points, but minimum should be 0
        List<AiValidationIssue> issues = List.of(
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL)
        );
        int score = calculator.calculateScore(issues);
        assertEquals(0, score, "Score should never go below 0");
    }

    @Test
    void testApprovedStatus() {
        // score >= 85, 0 critical issues
        List<AiValidationIssue> issues = List.of(issueWith(IssueSeverity.MINOR));
        ValidationStatus status = calculator.calculateStatus(97, issues);
        assertEquals(ValidationStatus.APPROVED, status);
    }

    @Test
    void testApprovedWithWarningsStatus() {
        // score >= 60, < 3 critical issues
        List<AiValidationIssue> issues = List.of(
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.MODERATE)
        );
        ValidationStatus status = calculator.calculateStatus(70, issues);
        assertEquals(ValidationStatus.APPROVED_WITH_WARNINGS, status);
    }

    @Test
    void testRejectedStatus() {
        // score < 60
        List<AiValidationIssue> issues = List.of(
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL)
        );
        ValidationStatus status = calculator.calculateStatus(40, issues);
        assertEquals(ValidationStatus.REJECTED, status);
    }

    @Test
    void testRejectedWhenTooManyCritical() {
        // score >= 60 but >= 3 critical issues
        List<AiValidationIssue> issues = List.of(
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL)
        );
        ValidationStatus status = calculator.calculateStatus(60, issues);
        assertEquals(ValidationStatus.REJECTED, status,
                "Should be REJECTED when there are 3 or more critical issues even with score >= 60");
    }

    @Test
    void testApprovedBoundary() {
        // Exactly score 85 with 0 critical
        ValidationStatus status = calculator.calculateStatus(85, Collections.emptyList());
        assertEquals(ValidationStatus.APPROVED, status,
                "Score of exactly 85 with no critical issues should be APPROVED");
    }

    @Test
    void testApprovedWithWarningsBoundary() {
        // score = 85 but has 1 critical -> APPROVED_WITH_WARNINGS (score>=85 but criticalCount!=0)
        List<AiValidationIssue> issues = List.of(issueWith(IssueSeverity.CRITICAL));
        ValidationStatus status = calculator.calculateStatus(85, issues);
        assertEquals(ValidationStatus.APPROVED_WITH_WARNINGS, status,
                "Score 85 with 1 critical should be APPROVED_WITH_WARNINGS, not APPROVED");
    }
}
