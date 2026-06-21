package com.company.specvalidator.service;

import com.company.specvalidator.dto.ai.AiValidationRequest;
import com.company.specvalidator.dto.ai.AiValidationResponse;
import com.company.specvalidator.entity.DocumentEntity;
import com.company.specvalidator.entity.ExtractedDocumentEntity;
import com.company.specvalidator.entity.ValidationReportEntity;
import com.company.specvalidator.enums.DocumentStatus;
import com.company.specvalidator.exception.ResourceValidationException;
import com.company.specvalidator.repository.ExtractedDocumentRepository;
import com.company.specvalidator.service.ai.AiProviderClient;
import com.company.specvalidator.service.ai.PromptBuilderService;
import com.company.specvalidator.service.validator.BusinessRuleValidator;
import com.company.specvalidator.service.validator.IntegrationValidator;
import com.company.specvalidator.service.validator.SapAbapValidator;
import com.company.specvalidator.service.validator.ScoreCalculator;
import com.company.specvalidator.service.validator.StructureValidator;
import com.company.specvalidator.service.validator.TestScenarioValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.util.ArrayList;

@Slf4j
@Service
public class ValidationAgentService {

    private final DocumentService documentService;
    private final TextExtractionService textExtractionService;
    private final DocumentNormalizerService documentNormalizerService;
    private final PromptBuilderService promptBuilderService;
    private final AiProviderClient aiProviderClient;
    private final ValidationReportService validationReportService;
    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final ObjectMapper objectMapper;
    private final StructureValidator structureValidator;
    private final SapAbapValidator sapAbapValidator;
    private final BusinessRuleValidator businessRuleValidator;
    private final IntegrationValidator integrationValidator;
    private final TestScenarioValidator testScenarioValidator;
    private final ScoreCalculator scoreCalculator;
    private final FileStorageService fileStorageService;

    public ValidationAgentService(DocumentService documentService,
                                  TextExtractionService textExtractionService,
                                  DocumentNormalizerService documentNormalizerService,
                                  PromptBuilderService promptBuilderService,
                                  AiProviderClient aiProviderClient,
                                  ValidationReportService validationReportService,
                                  ExtractedDocumentRepository extractedDocumentRepository,
                                  ObjectMapper objectMapper,
                                  StructureValidator structureValidator,
                                  SapAbapValidator sapAbapValidator,
                                  BusinessRuleValidator businessRuleValidator,
                                  IntegrationValidator integrationValidator,
                                  TestScenarioValidator testScenarioValidator,
                                  ScoreCalculator scoreCalculator,
                                  FileStorageService fileStorageService) {
        this.documentService = documentService;
        this.textExtractionService = textExtractionService;
        this.documentNormalizerService = documentNormalizerService;
        this.promptBuilderService = promptBuilderService;
        this.aiProviderClient = aiProviderClient;
        this.validationReportService = validationReportService;
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.objectMapper = objectMapper;
        this.structureValidator = structureValidator;
        this.sapAbapValidator = sapAbapValidator;
        this.businessRuleValidator = businessRuleValidator;
        this.integrationValidator = integrationValidator;
        this.testScenarioValidator = testScenarioValidator;
        this.scoreCalculator = scoreCalculator;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public ValidationReportEntity uploadAndValidate(MultipartFile file) {
        DocumentEntity document = documentService.upload(file);
        return validateDocument(document.getId(), file);
    }

    @Transactional
    public ValidationReportEntity validateUploadedDocument(Long documentId, MultipartFile fileFromRequest) {
        DocumentEntity document = documentService.getById(documentId);
        return validateDocument(documentId, fileFromRequest, document);
    }

    @Transactional
    public ValidationReportEntity validateUploadedDocument(Long documentId) {
        DocumentEntity document = documentService.getById(documentId);
        return validateDocument(documentId, null, document);
    }

    private ValidationReportEntity validateDocument(Long documentId, MultipartFile directFile) {
        DocumentEntity document = documentService.getById(documentId);
        return validateDocument(documentId, directFile, document);
    }

    private ValidationReportEntity validateDocument(Long documentId, MultipartFile directFile, DocumentEntity document) {
        log.info("Iniciando validacao do documento id={}", documentId);

        ExtractedDocument extracted = extract(documentId, directFile);
        log.info("Texto extraido: {} caracteres", extracted.getRawText().length());

        NormalizedDocument normalized = documentNormalizerService.normalize(extracted.getRawText());
        log.info("Texto normalizado: {} caracteres, {} secoes detectadas",
                normalized.getNormalizedText().length(), normalized.getSections().size());

        saveExtracted(document, extracted, normalized);

        String prompt = promptBuilderService.buildValidationPrompt(normalized.getNormalizedText());
        AiValidationResponse aiResponse = aiProviderClient.validateFunctionalSpecification(
                AiValidationRequest.builder()
                        .documentId(documentId)
                        .normalizedText(normalized.getNormalizedText())
                        .prompt(prompt)
                        .build()
        );

        int recalculatedScore = scoreCalculator.calculateScore(aiResponse.getIssues());
        aiResponse.setScore(recalculatedScore);
        aiResponse.setStatus(scoreCalculator.calculateStatus(recalculatedScore, aiResponse.getIssues()));

        log.info("Score recalculado: {}, status: {}", recalculatedScore, aiResponse.getStatus());

        ValidationReportEntity report = validationReportService.saveReport(document, aiResponse);

        document.setStatus(DocumentStatus.VALIDATED);
        documentService.save(document);

        log.info("Validacao concluida para documento id={}, reportId={}", documentId, report.getId());
        return report;
    }

    private ExtractedDocument extract(Long documentId, MultipartFile directFile) {
        if (directFile != null && !directFile.isEmpty()) {
            return textExtractionService.extract(directFile);
        }

        ExtractedDocumentEntity existing = extractedDocumentRepository.findByDocumentId(documentId).orElse(null);
        if (existing != null && existing.getRawText() != null && !existing.getRawText().isBlank()) {
            return ExtractedDocument.builder()
                    .rawText(existing.getRawText())
                    .pageCount(existing.getPageCount())
                    .build();
        }

        try {
            DocumentEntity document = documentService.getById(documentId);
            byte[] content = Files.readAllBytes(fileStorageService.resolve(document.getStoredFileName()));
            MultipartFile reconstructed = new InMemoryMultipartFile(
                    "file",
                    document.getOriginalFileName(),
                    document.getContentType(),
                    content
            );
            return textExtractionService.extract(reconstructed);
        } catch (Exception e) {
            throw new ResourceValidationException("Nao foi possivel recuperar arquivo para validacao");
        }
    }

    private void saveExtracted(DocumentEntity document, ExtractedDocument extracted, NormalizedDocument normalized) {
        try {
            String sectionsJson = objectMapper.writeValueAsString(normalized.getSections());
            ExtractedDocumentEntity entity = extractedDocumentRepository.findByDocumentId(document.getId())
                    .orElse(ExtractedDocumentEntity.builder().document(document).build());
            entity.setRawText(extracted.getRawText());
            entity.setNormalizedText(normalized.getNormalizedText());
            entity.setDetectedSections(sectionsJson);
            entity.setPageCount(extracted.getPageCount());
            extractedDocumentRepository.save(entity);

            document.setStatus(DocumentStatus.EXTRACTED);
            documentService.save(document);
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            documentService.save(document);
            throw new ResourceValidationException("Falha ao persistir documento extraido");
        }
    }
}
