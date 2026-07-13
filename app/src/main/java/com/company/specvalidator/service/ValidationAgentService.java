package com.company.specvalidator.service;

import com.company.specvalidator.dto.ai.AiValidationRequest;
import com.company.specvalidator.dto.ai.AiValidationResponse;
import com.company.specvalidator.entity.DocumentEntity;
import com.company.specvalidator.entity.ExtractedDocumentEntity;
import com.company.specvalidator.entity.ValidationReportEntity;
import com.company.specvalidator.enums.DocumentStatus;
import com.company.specvalidator.enums.ValidationStatus;
import com.company.specvalidator.exception.ResourceValidationException;
import com.company.specvalidator.repository.ExtractedDocumentRepository;
import com.company.specvalidator.service.ai.AiProviderClient;
import com.company.specvalidator.service.ai.LangFuseClient;
import com.company.specvalidator.service.ai.PromptBuilderService;
import com.company.specvalidator.service.validator.ScoreCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

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
    private final ScoreCalculator scoreCalculator;
    private final FileStorageService fileStorageService;
    private final SectionAnalyzerService sectionAnalyzerService;
    private final LangFuseClient langFuseClient;

    public ValidationAgentService(DocumentService documentService,
                                  TextExtractionService textExtractionService,
                                  DocumentNormalizerService documentNormalizerService,
                                  PromptBuilderService promptBuilderService,
                                  AiProviderClient aiProviderClient,
                                  ValidationReportService validationReportService,
                                  ExtractedDocumentRepository extractedDocumentRepository,
                                  ScoreCalculator scoreCalculator,
                                  FileStorageService fileStorageService,
                                  SectionAnalyzerService sectionAnalyzerService,
                                  LangFuseClient langFuseClient) {
        this.documentService = documentService;
        this.textExtractionService = textExtractionService;
        this.documentNormalizerService = documentNormalizerService;
        this.promptBuilderService = promptBuilderService;
        this.aiProviderClient = aiProviderClient;
        this.validationReportService = validationReportService;
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.scoreCalculator = scoreCalculator;
        this.fileStorageService = fileStorageService;
        this.sectionAnalyzerService = sectionAnalyzerService;
        this.langFuseClient = langFuseClient;
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

        String traceId = UUID.randomUUID().toString();
        langFuseClient.startTrace(traceId, "document-validation", Map.of("documentId", documentId.toString()));

        String extractionSpanId = langFuseClient.startSpan(traceId, null, "text-extraction",
                Map.of("documentId", documentId));
        ExtractedDocument extracted;
        try {
            extracted = extract(documentId, directFile);
        } catch (RuntimeException e) {
            langFuseClient.endSpanWithError(extractionSpanId, e.getMessage());
            throw e;
        }
        log.info("Texto extraido: {} caracteres, {} secoes detectadas via headings",
                extracted.getRawText().length(), extracted.getSections().size());
        langFuseClient.endSpan(extractionSpanId,
                Map.of("rawTextLength", extracted.getRawText().length(), "sectionsDetected", extracted.getSections().size()),
                null);

        String normalizationSpanId = langFuseClient.startSpan(traceId, null, "normalization",
                Map.of("rawTextLength", extracted.getRawText().length()));
        NormalizedDocument normalized;
        try {
            normalized = documentNormalizerService.normalize(extracted.getRawText());
        } catch (RuntimeException e) {
            langFuseClient.endSpanWithError(normalizationSpanId, e.getMessage());
            throw e;
        }
        log.info("Texto normalizado: {} caracteres", normalized.getNormalizedText().length());
        langFuseClient.endSpan(normalizationSpanId,
                Map.of("normalizedTextLength", normalized.getNormalizedText().length()), null);

        saveExtracted(document, extracted, normalized);

        String sectionAnalysisSpanId = langFuseClient.startSpan(traceId, null, "section-analysis",
                Map.of("sectionsDetected", extracted.getSections().size()));
        var sectionAnalysis = sectionAnalyzerService.analyze(extracted.getSections(), extracted.getRawText());
        langFuseClient.endSpan(sectionAnalysisSpanId, Map.of("sectionsAnalyzed", sectionAnalysis.size()), null);

        String systemPrompt = promptBuilderService.buildSystemPrompt(normalized.getNormalizedText());
        String userPrompt = promptBuilderService.buildUserPrompt(normalized.getNormalizedText());
        AiValidationResponse aiResponse = aiProviderClient.validateFunctionalSpecification(
                AiValidationRequest.builder()
                        .documentId(documentId)
                        .systemPrompt(systemPrompt)
                        .userPrompt(userPrompt)
                        .traceId(traceId)
                        .build()
        );

        aiResponse.setSectionAnalysis(sectionAnalysis);

        String scoringSpanId = langFuseClient.startSpan(traceId, null, "scoring",
                Map.of("issuesCount", aiResponse.getIssues().size()));
        int rawScore = scoreCalculator.calculateScore(aiResponse.getIssues());
        int sectionBonus = scoreCalculator.calculateSectionBonus(aiResponse.getSectionAnalysis());
        int adjustedScore = rawScore + sectionBonus;
        ValidationStatus status = scoreCalculator.calculateStatus(adjustedScore, aiResponse.getIssues());
        int normalizedScore = scoreCalculator.normalizeScore(adjustedScore, status);
        aiResponse.setScore(normalizedScore);
        aiResponse.setStatus(status);
        langFuseClient.endSpan(scoringSpanId,
                Map.of("rawScore", rawScore, "sectionBonus", sectionBonus,
                        "normalizedScore", normalizedScore, "status", status.toString()),
                null);

        log.info("Score raw: {}, normalizado: {}, status: {}", rawScore, normalizedScore, status);

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
            ExtractedDocumentEntity entity = extractedDocumentRepository.findByDocumentId(document.getId())
                    .orElse(ExtractedDocumentEntity.builder().document(document).build());
            entity.setRawText(extracted.getRawText());
            entity.setNormalizedText(normalized.getNormalizedText());
            entity.setDetectedSections("{}");
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
