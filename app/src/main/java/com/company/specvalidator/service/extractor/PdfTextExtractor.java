package com.company.specvalidator.service.extractor;

import com.company.specvalidator.exception.DocumentExtractionException;
import com.company.specvalidator.service.ExtractedDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

@Component
public class PdfTextExtractor {

    public ExtractedDocument extract(MultipartFile file) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            parser.parse(file.getInputStream(), handler, metadata, context);

            String text = handler.toString();
            Integer pageCount = parseIntOrNull(metadata.get("xmpTPg:NPages"));
            if (pageCount == null) {
                pageCount = parseIntOrNull(metadata.get("meta:page-count"));
            }

            return ExtractedDocument.builder()
                    .rawText(text)
                    .pageCount(pageCount)
                    .build();
        } catch (Exception e) {
            throw new DocumentExtractionException("Nao foi possivel extrair texto do PDF", e);
        }
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
