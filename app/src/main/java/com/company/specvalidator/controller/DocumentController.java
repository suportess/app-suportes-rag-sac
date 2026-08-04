package com.company.specvalidator.controller;

import com.company.specvalidator.dto.response.DocumentResponse;
import com.company.specvalidator.dto.response.DocumentUploadResponse;
import com.company.specvalidator.dto.response.ValidationReportResponse;
import com.company.specvalidator.entity.DocumentEntity;
import com.company.specvalidator.entity.ValidationReportEntity;
import com.company.specvalidator.mapper.DocumentMapper;
import com.company.specvalidator.mapper.ValidationReportMapper;
import com.company.specvalidator.service.DocumentService;
import com.company.specvalidator.service.ValidationAgentService;
import com.company.specvalidator.service.ValidationReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Upload e validacao de especificacoes funcionais SAP ABAP")
public class DocumentController {

    private final DocumentService documentService;
    private final ValidationAgentService validationAgentService;
    private final ValidationReportService validationReportService;
    private final DocumentMapper documentMapper;
    private final ValidationReportMapper validationReportMapper;

    public DocumentController(DocumentService documentService,
                              ValidationAgentService validationAgentService,
                              ValidationReportService validationReportService,
                              DocumentMapper documentMapper,
                              ValidationReportMapper validationReportMapper) {
        this.documentService = documentService;
        this.validationAgentService = validationAgentService;
        this.validationReportService = validationReportService;
        this.documentMapper = documentMapper;
        this.validationReportMapper = validationReportMapper;
    }

    @Operation(summary = "Upload de documento sem validacao")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        log.info("Upload de documento: {}", file.getOriginalFilename());
        DocumentEntity saved = documentService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(documentMapper.toUploadResponse(saved));
    }

    @Operation(summary = "Upload e validacao direta do documento com IA")
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ValidationReportResponse> uploadAndValidate(@RequestPart("file") MultipartFile file) {
        log.info("Upload e validacao de documento: {}", file.getOriginalFilename());
        ValidationReportEntity report = validationAgentService.uploadAndValidate(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(toReportResponse(report));
    }

    @Operation(summary = "Validar documento ja enviado anteriormente")
    @PostMapping("/{documentId}/validate")
    public ResponseEntity<ValidationReportResponse> validateUploaded(@PathVariable Long documentId) {
        log.info("Validacao de documento existente: id={}", documentId);
        ValidationReportEntity report = validationAgentService.validateUploadedDocument(documentId);
        return ResponseEntity.ok(toReportResponse(report));
    }

    @Operation(summary = "Listar documentos com paginacao")
    @GetMapping
    public Page<DocumentResponse> listDocuments(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return documentService.listAll(pageable).map(documentMapper::toResponse);
    }

    @Operation(summary = "Buscar documento por ID")
    @GetMapping("/{documentId}")
    public DocumentResponse getDocument(@PathVariable Long documentId) {
        return documentMapper.toResponse(documentService.getById(documentId));
    }

    @Operation(summary = "Download do arquivo original do documento")
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long documentId) {
        DocumentEntity document = documentService.getById(documentId);
        Resource resource = documentService.loadFile(document);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    private ValidationReportResponse toReportResponse(ValidationReportEntity report) {
        var checklist = validationReportService.getChecklist(report.getId());
        var pontosCriticos = validationReportService.getPontosCriticos(report.getId());
        return validationReportMapper.toResponse(
                report,
                checklist,
                pontosCriticos,
                validationReportService.parseStringList(report.getPrincipaisRiscosJson()),
                validationReportService.parseStringList(report.getRecomendacoesJson()),
                validationReportService.parseSectionStatusList(report.getSectionAnalysisJson())
        );
    }
}
