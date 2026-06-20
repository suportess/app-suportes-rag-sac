package com.company.specvalidator.repository;

import com.company.specvalidator.entity.ValidationIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationIssueRepository extends JpaRepository<ValidationIssueEntity, Long> {
    List<ValidationIssueEntity> findByReportId(Long reportId);
}
