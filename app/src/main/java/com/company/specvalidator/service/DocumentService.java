package com.company.specvalidator.service;

import com.company.specvalidator.entity.DocumentEntity;
import com.company.specvalidator.enums.DocumentStatus;
import com.company.specvalidator.enums.DocumentType;
import com.company.specvalidator.exception.DocumentNotFoundException;
import com.company.specvalidator.exception.ResourceValidationException;
import com.company.specvalidator.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;

@Slf4j
@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public DocumentService(DocumentRepository documentRepository, FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
    }

    public DocumentEntity upload(MultipartFile file) {
        validateFile(file);
        String storedName = fileStorageService.store(file);

        DocumentEntity document = DocumentEntity.builder()
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedName)
                .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .documentType(detectType(file))
                .fileSize(file.getSize())
                .status(DocumentStatus.UPLOADED)
                .build();

        log.info("Documento salvo: {} ({})", file.getOriginalFilename(), document.getDocumentType());
        return documentRepository.save(document);
    }

    public DocumentEntity getById(Long id) {
        return documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }

    public Page<DocumentEntity> listAll(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    public DocumentEntity save(DocumentEntity documentEntity) {
        return documentRepository.save(documentEntity);
    }

    public Resource loadFile(DocumentEntity document) {
        Path path = fileStorageService.resolve(document.getStoredFileName());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceValidationException(
                        "Arquivo nao encontrado no armazenamento para o documento id=" + document.getId());
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceValidationException("Caminho de arquivo invalido para o documento id=" + document.getId());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResourceValidationException("Arquivo vazio");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResourceValidationException("Documento muito grande. Limite de 20MB.");
        }
        if (detectType(file) == DocumentType.UNKNOWN) {
            throw new ResourceValidationException("Tipo de arquivo nao suportado. Use PDF, DOCX ou TXT.");
        }
    }

    private DocumentType detectType(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (name.endsWith(".pdf") || contentType.contains("pdf")) {
            return DocumentType.PDF;
        }
        if (name.endsWith(".docx") || contentType.contains("wordprocessingml")) {
            return DocumentType.DOCX;
        }
        if (name.endsWith(".txt") || contentType.contains("text/plain")) {
            return DocumentType.TXT;
        }
        return DocumentType.UNKNOWN;
    }
}
