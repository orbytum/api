package com.orbytum.api.models.exceptions;

public class LembreteNaoEncontradoErro extends RuntimeException {
    public LembreteNaoEncontradoErro(String message) {
        super(message);
    }

    public LembreteNaoEncontradoErro() {
        super("Lembrete não encontrado.");
    }
}
