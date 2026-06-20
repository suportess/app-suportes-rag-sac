package com.company.specvalidator.dto.response;

import com.company.specvalidator.enums.DocumentStatus;
import com.company.specvalidator.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {
    private Long id;
    private String originalFileName;
    private String contentType;
    private DocumentType documentType;
    private Long fileSize;
    private DocumentStatus status;
    private LocalDateTime createdAt;
}
