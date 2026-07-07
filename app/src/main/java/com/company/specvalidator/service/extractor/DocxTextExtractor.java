package com.company.specvalidator.service.extractor;

import com.company.specvalidator.exception.DocumentExtractionException;
import com.company.specvalidator.service.ExtractedDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class DocxTextExtractor {

    public ExtractedDocument extract(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();

            // Extração de texto plano via Tika (inalterada)
            AutoDetectParser parser = new AutoDetectParser();
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            parser.parse(new ByteArrayInputStream(bytes), handler, metadata, context);
            String rawText = handler.toString();

            // Extração de seções via headings do Word (Apache POI)
            Map<String, String> sections = extractSections(new ByteArrayInputStream(bytes));

            return ExtractedDocument.builder()
                    .rawText(rawText)
                    .sections(sections)
                    .pageCount(null)
                    .build();
        } catch (Exception e) {
            throw new DocumentExtractionException("Nao foi possivel extrair texto do DOCX", e);
        }
    }

    private Map<String, String> extractSections(ByteArrayInputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            Map<String, String> sections = new LinkedHashMap<>();
            String currentSection = null;
            StringBuilder currentContent = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (isHeading(paragraph)) {
                    String headingText = paragraph.getText().trim();
                    if (!headingText.isEmpty()) {
                        if (currentSection != null) {
                            sections.put(currentSection, currentContent.toString().trim());
                        }
                        currentSection = headingText;
                        currentContent = new StringBuilder();
                    }
                } else {
                    String text = paragraph.getText().trim();
                    if (!text.isEmpty() && currentSection != null) {
                        currentContent.append(text).append("\n");
                    }
                }
            }
            if (currentSection != null) {
                sections.put(currentSection, currentContent.toString().trim());
            }

            // Retorna o mapa somente se encontrou estrutura real de headings
            if (sections.size() >= 2) {
                log.debug("Extraidas {} secoes via headings do Word", sections.size());
                return sections;
            }
            log.debug("Poucos headings detectados ({}), fallback para analise por texto", sections.size());
            return Map.of();
        } catch (Throwable e) {
            log.warn("Nao foi possivel extrair secoes via headings: {}. Usando fallback por texto.", e.getMessage());
            return Map.of();
        }
    }

    private boolean isHeading(XWPFParagraph paragraph) {
        String styleId = paragraph.getStyleID();
        if (styleId != null) {
            String lower = styleId.toLowerCase();
            if (lower.startsWith("heading") || lower.startsWith("ttulo") || lower.startsWith("titulo")) {
                return true;
            }
        }
        // Verifica outline level (funciona com templates customizados)
        try {
            if (paragraph.getCTP().getPPr() != null &&
                    paragraph.getCTP().getPPr().getOutlineLvl() != null) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
