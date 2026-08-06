package com.company.specvalidator.service;

import com.company.specvalidator.dto.ai.AiValidationRequest;
import com.company.specvalidator.dto.ai.AiValidationResponse;
import com.company.specvalidator.entity.DocumentEntity;
import com.company.specvalidator.entity.ExtractedDocumentEntity;
import com.company.specvalidator.entity.ValidationReportEntity;
import com.company.specvalidator.enums.DevType;
import com.company.specvalidator.enums.DocumentStatus;
import com.company.specvalidator.enums.ValidationStatus;
import com.company.specvalidator.exception.ResourceValidationException;
import com.company.specvalidator.repository.ExtractedDocumentRepository;
import com.company.specvalidator.service.ai.AiProviderClient;
import com.company.specvalidator.service.ai.LangFuseClient;
import com.company.specvalidator.service.ai.LangFusePromptService;
import com.company.specvalidator.service.ai.PromptBuilderService;
import com.company.specvalidator.service.ai.PromptResult;
import com.company.specvalidator.service.validator.ScoreCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.company.specvalidator.dto.ai.ChecklistItem;
import com.company.specvalidator.enums.ChecklistItemKey;
import com.company.specvalidator.enums.ChecklistStatus;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final LangFusePromptService langFusePromptService;

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
                                  LangFuseClient langFuseClient,
                                  LangFusePromptService langFusePromptService) {
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
        this.langFusePromptService = langFusePromptService;
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
        langFuseClient.startTrace(traceId, "document-validation", "doc-" + documentId.toString(), Map.of("documentId", documentId.toString()));

        String extractionSpanId = langFuseClient.startSpan(traceId, null, "text-extraction",
                Map.of("documentId", documentId,
                        "originalFileName", document.getOriginalFileName(),
                        "documentType", document.getDocumentType().toString()));
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
                Map.of("rawTextLength", extracted.getRawText().length(),
                        "sectionsDetected", extracted.getSections().size(),
                        "textPreview", preview(extracted.getRawText())),
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
        langFuseClient.endSpan(sectionAnalysisSpanId,
                Map.of("sectionsAnalyzed", sectionAnalysis.size(), "sections", sectionAnalysis),
                null);

        // Detecta o tipo WRICEF uma unica vez — usado tanto pro prompt (criterios especificos
        // por tipo) quanto pro calculo do score (quais criterios condicionais contam).
        DevType devType = promptBuilderService.detectDevType(normalized.getNormalizedText());

        // Tenta o prompt versionado na Langfuse (Prompt Management); se estiver desabilitada,
        // sem label "production" ou fora do ar, cai pro fallback hardcoded do
        // PromptBuilderService — a validacao nunca deve depender da Langfuse estar disponivel.
        Map<String, String> promptVariables = promptBuilderService.buildPromptVariables(normalized.getNormalizedText(), devType);
        PromptResult promptResult = langFusePromptService.buildPrompt(promptVariables)
                .orElseGet(() -> new PromptResult(
                        promptBuilderService.buildSystemPrompt(normalized.getNormalizedText(), devType),
                        promptBuilderService.buildUserPrompt(normalized.getNormalizedText()),
                        null));

        AiValidationResponse aiResponse = aiProviderClient.validateFunctionalSpecification(
                AiValidationRequest.builder()
                        .documentId(documentId)
                        .systemPrompt(promptResult.systemPrompt())
                        .userPrompt(promptResult.userPrompt())
                        .traceId(traceId)
                        .promptVersion(promptResult.promptVersion())
                        .build()
        );

        aiResponse.setSectionAnalysis(sectionAnalysis);

        List<ChecklistItem> checklist = finalizeChecklist(aiResponse.getChecklist());
        aiResponse.setChecklist(checklist);

        String scoringSpanId = langFuseClient.startSpan(traceId, null, "scoring",
                Map.of("checklistCount", checklist.size(), "devType", devType.toString()));
        int score = scoreCalculator.calculateScore(aiResponse.getChecklist(), devType);
        ValidationStatus classificacao = scoreCalculator.calculateClassificacao(score);
        aiResponse.setScore(score);
        aiResponse.setClassificacao(classificacao);
        langFuseClient.endSpan(scoringSpanId,
                Map.of("score", score, "classificacao", classificacao.toString(), "checklist", checklist),
                null);

        log.info("Score: {}, classificacao: {}", score, classificacao);

        ValidationReportEntity report = validationReportService.saveReport(document, aiResponse);

        document.setStatus(DocumentStatus.VALIDATED);
        documentService.save(document);

        langFuseClient.endTrace(traceId,
                Map.of("qualidade", aiResponse.getQualidade(), "score", score, "classificacao", classificacao.toString()),
                List.of(devType.toString(), classificacao.toString()));

        log.info("Validacao concluida para documento id={}, reportId={}", documentId, report.getId());
        return report;
    }

    /**
     * Roda o pipeline de IA+score (sem extracao de arquivo, sem persistencia em banco) pra um
     * texto de EF avulso — usado pra rodar itens de um Dataset da Langfuse contra o sistema real
     * e comparar o resultado com o expected_output de cada item. sessionId agrupa todas as
     * execucoes de uma mesma rodada na aba Sessions da Langfuse.
     */
    public DatasetRunItemResult runForDatasetItem(String rawText, String traceId, String sessionId, String itemLabel) {
        langFuseClient.startTrace(traceId, "dataset-run-item", sessionId, preview(rawText), Map.of("itemLabel", itemLabel));

        NormalizedDocument normalized = documentNormalizerService.normalize(rawText);
        var sectionAnalysis = sectionAnalyzerService.analyze(Map.of(), rawText);

        DevType devType = promptBuilderService.detectDevType(normalized.getNormalizedText());
        Map<String, String> promptVariables = promptBuilderService.buildPromptVariables(normalized.getNormalizedText(), devType);
        PromptResult promptResult = langFusePromptService.buildPrompt(promptVariables)
                .orElseGet(() -> new PromptResult(
                        promptBuilderService.buildSystemPrompt(normalized.getNormalizedText(), devType),
                        promptBuilderService.buildUserPrompt(normalized.getNormalizedText()),
                        null));

        AiValidationResponse aiResponse = aiProviderClient.validateFunctionalSpecification(
                AiValidationRequest.builder()
                        .systemPrompt(promptResult.systemPrompt())
                        .userPrompt(promptResult.userPrompt())
                        .traceId(traceId)
                        .promptVersion(promptResult.promptVersion())
                        .build()
        );
        aiResponse.setSectionAnalysis(sectionAnalysis);

        List<ChecklistItem> checklist = finalizeChecklist(aiResponse.getChecklist());
        int score = scoreCalculator.calculateScore(checklist, devType);
        ValidationStatus classificacao = scoreCalculator.calculateClassificacao(score);

        langFuseClient.endTrace(traceId,
                Map.of(
                "qualidade", aiResponse.getQualidade(), "score", score, 
                "classificacao", classificacao.toString(), "resumo executivo", aiResponse.getResumoExecutivo(), 
                "checklist", checklist.toString(), "parecer final", aiResponse.getParecerFinal(), 
                "recomendacoes", aiResponse.getRecomendacoes(), "pontos criticos", aiResponse.getPontosCriticos()),
                List.of("dataset-run", devType.toString(), classificacao.toString()));

        return new DatasetRunItemResult(score, classificacao.toString(), aiResponse.getQualidade(),
            aiResponse.getResumoExecutivo(), checklist.toString(), aiResponse.getParecerFinal(),
            aiResponse.getRecomendacoes(), aiResponse.getPontosCriticos());
    }

    /**
     * Marca itens retornados pela IA como aplicaveis e calcula peso/pontos, e completa com os
     * criterios nao enviados ao prompt (nao aplicaveis ao DevType detectado). Usado tanto pelo
     * fluxo real de upload quanto pelos dataset runs.
     */
    private List<ChecklistItem> finalizeChecklist(List<ChecklistItem> raw) {
        List<ChecklistItem> checklist = new ArrayList<>(raw.stream()
                .filter(item -> item.getChave() != ChecklistItemKey.UNKNOWN)
                .toList());
        checklist.forEach(item -> {
            item.setAplicavel(true);
            item.setPontos(scoreCalculator.calculatePontosPerdidos(item));
            item.setPeso(scoreCalculator.pesoDe(item.getChave()));
            item.setPontosConquistados(scoreCalculator.pontosConquistados(item));
        });

        Set<ChecklistItemKey> retornados = checklist.stream()
                .map(ChecklistItem::getChave).collect(Collectors.toSet());
        Arrays.stream(ChecklistItemKey.values())
                .filter(chave -> chave != ChecklistItemKey.UNKNOWN)
                .filter(chave -> !retornados.contains(chave))
                .forEach(chave -> {
                    int peso = scoreCalculator.pesoDe(chave);
                    checklist.add(ChecklistItem.builder()
                            .chave(chave)
                            .item(chave.name())
                            .status(ChecklistStatus.AUSENTE)
                            .comentario("Não aplicável para este tipo de desenvolvimento.")
                            .aplicavel(false)
                            .peso(peso)
                            .pontosConquistados(0.0)
                            .pontos(0)
                            .build());
                });
        return checklist;
    }

    private String preview(String text) {
        if (text == null) return "";
        return text.length() > 500 ? text.substring(0, 500) + "..." : text;
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
