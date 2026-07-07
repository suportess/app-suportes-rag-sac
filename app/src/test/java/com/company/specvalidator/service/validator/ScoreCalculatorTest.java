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
    void testScoreCanBeNegativeForVeryBadEfs() {
        // 6 CRITICALs = 100 - 120 = -20 (sem floor)
        List<AiValidationIssue> issues = List.of(
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL)
        );
        int score = calculator.calculateScore(issues);
        assertEquals(-20, score, "Raw score nao tem floor — pode ser negativo");
    }

    @Test
    void testNormalizeScoreRejectedMapsTo1to29() {
        // raw 0 deve mapear para valor entre 1 e 29
        int normalized = calculator.normalizeScore(0, ValidationStatus.REJECTED);
        assertTrue(normalized >= 1 && normalized <= 29, "Score REJECTED normalizado deve estar entre 1 e 29");
    }

    @Test
    void testNormalizeScoreRejectedBarelyRejectedIsNear29() {
        // raw 29 (limite superior do REJECTED) deve normalizar para 29
        int normalized = calculator.normalizeScore(29, ValidationStatus.REJECTED);
        assertEquals(29, normalized);
    }

    @Test
    void testNormalizeScoreRejectedExtremeIsOne() {
        // raw -100 (pior caso) deve normalizar para 1
        int normalized = calculator.normalizeScore(-100, ValidationStatus.REJECTED);
        assertEquals(1, normalized);
    }

    @Test
    void testNormalizeScoreApprovedWithWarningsPreserved() {
        int normalized = calculator.normalizeScore(55, ValidationStatus.APPROVED_WITH_WARNINGS);
        assertEquals(55, normalized);
    }

    @Test
    void testApprovedStatus() {
        // score >= 85, 0 critical issues
        List<AiValidationIssue> issues = List.of(issueWith(IssueSeverity.MINOR));
        ValidationStatus status = calculator.calculateStatus(97, issues);
        assertEquals(ValidationStatus.APPROVED, status);
    }

    @Test
    void testApprovedWithWarningsWithOneCritical() {
        List<AiValidationIssue> issues = List.of(
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.MODERATE)
        );
        ValidationStatus status = calculator.calculateStatus(70, issues);
        assertEquals(ValidationStatus.APPROVED_WITH_WARNINGS, status);
    }

    @Test
    void testApprovedWithWarningsWithThreeCriticalsAndDecentScore() {
        // 3 CRITICALs, sem mais nada: score = 100 - 60 = 40 >= 30 → APPROVED_WITH_WARNINGS
        List<AiValidationIssue> issues = List.of(
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL)
        );
        ValidationStatus status = calculator.calculateStatus(40, issues);
        assertEquals(ValidationStatus.APPROVED_WITH_WARNINGS, status,
                "3 CRITICALs com score 40 deve ser APPROVED_WITH_WARNINGS");
    }

    @Test
    void testRejectedWhenScoreTooLowEvenWithFewCriticals() {
        // Score 4 = acumulo grande de problemas; deve ser REJECTED mesmo com poucos CRITICALs
        List<AiValidationIssue> issues = List.of(
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL)
        );
        ValidationStatus status = calculator.calculateStatus(4, issues);
        assertEquals(ValidationStatus.REJECTED, status,
                "Score 4 deve ser REJECTED independente do numero de CRITICALs");
    }

    @Test
    void testRejectedWithFourCriticals() {
        // 4+ CRITICALs = REJECTED independente do score
        List<AiValidationIssue> issues = List.of(
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL),
                issueWith(IssueSeverity.CRITICAL)
        );
        ValidationStatus status = calculator.calculateStatus(0, issues);
        assertEquals(ValidationStatus.REJECTED, status,
                "4 or more CRITICALs should always be REJECTED");
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
