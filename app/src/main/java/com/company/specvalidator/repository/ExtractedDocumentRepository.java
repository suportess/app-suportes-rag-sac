package com.company.specvalidator.repository;

import com.company.specvalidator.entity.ExtractedDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExtractedDocumentRepository extends JpaRepository<ExtractedDocumentEntity, Long> {
    Optional<ExtractedDocumentEntity> findByDocumentId(Long documentId);
}
