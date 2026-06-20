package com.company.specvalidator.repository;

import com.company.specvalidator.entity.ValidationQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationQuestionRepository extends JpaRepository<ValidationQuestionEntity, Long> {
    List<ValidationQuestionEntity> findByReportId(Long reportId);
}
