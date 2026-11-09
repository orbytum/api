package com.orbytum.api.models.exceptions;

public class ArquivoStorageException extends RuntimeException {

    public ArquivoStorageException(String message) {
        super(message);
    }

    public ArquivoStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
