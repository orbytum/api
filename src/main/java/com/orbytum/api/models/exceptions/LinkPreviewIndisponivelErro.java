package com.orbytum.api.models.exceptions;

public class LinkPreviewIndisponivelErro extends RuntimeException {

    public LinkPreviewIndisponivelErro(String message) {
        super(message);
    }

    public LinkPreviewIndisponivelErro(String message, Throwable cause) {
        super(message, cause);
    }
}
