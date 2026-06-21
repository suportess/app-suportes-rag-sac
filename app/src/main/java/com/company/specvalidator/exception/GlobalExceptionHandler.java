package com.company.specvalidator.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ResourceValidationException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DocumentExtractionException.class)
    public ResponseEntity<ApiErrorResponse> handleExtraction(DocumentExtractionException ex, HttpServletRequest request) {
        log.error("Document extraction error", ex);
        return build(HttpStatus.BAD_REQUEST, "DOCUMENT_EXTRACTION_ERROR", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiErrorResponse> handleAi(AiProviderException ex, HttpServletRequest request) {
        log.error("AI provider error", ex);
        return build(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(DocumentNotFoundException ex, HttpServletRequest request) {
        log.warn("Document not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(Exception ex, HttpServletRequest request) {
        log.warn("Bean validation error: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_ERROR", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File too large: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "Arquivo excede o tamanho maximo permitido de 20MB.", request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recurso nao encontrado. API disponivel em /api/v1/", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocorreu um erro interno. Tente novamente ou contate o suporte.", request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String error, String message, String path) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    @Data
    @Builder
    public static class ApiErrorResponse {
        private LocalDateTime timestamp;
        private Integer status;
        private String error;
        private String message;
        private String path;
    }
}
