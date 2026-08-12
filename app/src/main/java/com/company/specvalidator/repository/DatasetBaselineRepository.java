package com.company.specvalidator.repository;

import com.company.specvalidator.entity.DatasetBaselineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatasetBaselineRepository extends JpaRepository<DatasetBaselineEntity, Long> {
    Optional<DatasetBaselineEntity> findByDatasetName(String datasetName);
}
