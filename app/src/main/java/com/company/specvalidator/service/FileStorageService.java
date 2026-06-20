package com.company.specvalidator.service;

import com.company.specvalidator.config.OpenAiConfig;
import com.company.specvalidator.exception.ResourceValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path basePath;

    public FileStorageService(OpenAiConfig.StorageProperties storageProperties) {
        this.basePath = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new ResourceValidationException("Nao foi possivel inicializar pasta de uploads");
        }
    }

    public String store(MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "documento" : file.getOriginalFilename();
        String storedName = UUID.randomUUID() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        try {
            Path target = basePath.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return storedName;
        } catch (IOException e) {
            throw new ResourceValidationException("Falha ao salvar arquivo no filesystem");
        }
    }

    public Path resolve(String storedFileName) {
        return basePath.resolve(storedFileName);
    }
}
