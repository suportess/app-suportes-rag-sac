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
public class DocxTextExtractor {

    public ExtractedDocument extract(MultipartFile file) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            parser.parse(file.getInputStream(), handler, metadata, context);

            return ExtractedDocument.builder()
                    .rawText(handler.toString())
                    .pageCount(null)
                    .build();
        } catch (Exception e) {
            throw new DocumentExtractionException("Nao foi possivel extrair texto do DOCX", e);
        }
    }
}
