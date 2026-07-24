package com.company.specvalidator.repository;

import com.company.specvalidator.entity.PontoCriticoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PontoCriticoRepository extends JpaRepository<PontoCriticoEntity, Long> {
    List<PontoCriticoEntity> findByReportId(Long reportId);
}
