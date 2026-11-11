package com.orbytum.api.models.exceptions;

public class ProjetoNaoPodeSerFinalizado extends RuntimeException {
    public ProjetoNaoPodeSerFinalizado(String message) {
        super(message);
    }
}
