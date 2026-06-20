package com.company.specvalidator.entity;

import com.company.specvalidator.enums.ValidationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "validation_reports")
public class ValidationReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatus status;

    @Column(nullable = false)
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String specificationSummary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String finalRecommendation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String positivePointsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String missingSectionsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String riskAnalysis;

    @Column(nullable = false)
    private Integer criticalIssuesCount;

    @Column(nullable = false)
    private Integer moderateIssuesCount;

    @Column(nullable = false)
    private Integer minorIssuesCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
