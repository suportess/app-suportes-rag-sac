package com.company.specvalidator.service.extractor;

import com.company.specvalidator.enums.DocumentType;
import com.company.specvalidator.exception.DocumentExtractionException;
import com.company.specvalidator.service.ExtractedDocument;
import com.company.specvalidator.service.TextExtractionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Service
public class TextExtractorFacade implements TextExtractionService {

    private final PdfTextExtractor pdfTextExtractor;
    private final DocxTextExtractor docxTextExtractor;

    public TextExtractorFacade(PdfTextExtractor pdfTextExtractor, DocxTextExtractor docxTextExtractor) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.docxTextExtractor = docxTextExtractor;
    }

    @Override
    public ExtractedDocument extract(MultipartFile file) {
        DocumentType documentType = detectType(file.getOriginalFilename(), file.getContentType());

        return switch (documentType) {
            case PDF -> {
                ExtractedDocument extracted = pdfTextExtractor.extract(file);
                if (extracted.getRawText() == null || extracted.getRawText().isBlank()) {
                    // Evolucao futura: integrar OCR com Tesseract para PDFs escaneados.
                    throw new DocumentExtractionException("PDF parece ser imagem escaneada. OCR ainda nao implementado no MVP.");
                }
                yield extracted;
            }
            case DOCX -> docxTextExtractor.extract(file);
            case TXT -> extractTxt(file);
            default -> throw new DocumentExtractionException("Tipo de arquivo nao suportado. Use PDF, DOCX ou TXT.");
        };
    }

    private ExtractedDocument extractTxt(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            return ExtractedDocument.builder().rawText(text).pageCount(null).build();
        } catch (Exception e) {
            throw new DocumentExtractionException("Nao foi possivel ler arquivo TXT", e);
        }
    }

    private DocumentType detectType(String fileName, String contentType) {
        String normalizedName = fileName == null ? "" : fileName.toLowerCase();
        String normalizedContent = contentType == null ? "" : contentType.toLowerCase();

        if (normalizedName.endsWith(".pdf") || normalizedContent.contains("pdf")) {
            return DocumentType.PDF;
        }
        if (normalizedName.endsWith(".docx") || normalizedContent.contains("wordprocessingml")) {
            return DocumentType.DOCX;
        }
        if (normalizedName.endsWith(".txt") || normalizedContent.contains("text/plain")) {
            return DocumentType.TXT;
        }
        return DocumentType.UNKNOWN;
    }
}
