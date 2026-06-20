package com.company.specvalidator.service;

import org.springframework.web.multipart.MultipartFile;

public interface TextExtractionService {
    ExtractedDocument extract(MultipartFile file);
}
