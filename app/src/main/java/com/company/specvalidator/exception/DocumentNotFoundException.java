package com.company.specvalidator.exception;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Long id) {
        super("Documento nao encontrado para id=" + id);
    }
}
