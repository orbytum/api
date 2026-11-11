package com.orbytum.api.models.exceptions;

public class S3StorageException extends ArquivoStorageException {

    public S3StorageException(String message) {
        super(message);
    }

    public S3StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
