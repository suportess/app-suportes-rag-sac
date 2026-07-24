package com.company.specvalidator.repository;

import com.company.specvalidator.entity.ChecklistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItemEntity, Long> {
    List<ChecklistItemEntity> findByReportId(Long reportId);
}
