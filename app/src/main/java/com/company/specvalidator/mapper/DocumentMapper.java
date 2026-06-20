package com.company.specvalidator.mapper;

import com.company.specvalidator.dto.response.DocumentResponse;
import com.company.specvalidator.dto.response.DocumentUploadResponse;
import com.company.specvalidator.entity.DocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentUploadResponse toUploadResponse(DocumentEntity entity) {
        return DocumentUploadResponse.builder()
                .id(entity.getId())
                .originalFileName(entity.getOriginalFileName())
                .contentType(entity.getContentType())
                .documentType(entity.getDocumentType())
                .fileSize(entity.getFileSize())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public DocumentResponse toResponse(DocumentEntity entity) {
        return DocumentResponse.builder()
                .id(entity.getId())
                .originalFileName(entity.getOriginalFileName())
                .storedFileName(entity.getStoredFileName())
                .contentType(entity.getContentType())
                .documentType(entity.getDocumentType())
                .fileSize(entity.getFileSize())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
