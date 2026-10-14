package com.orbytum.api.models.exceptions;

public class EventoNaoEncontradoErro extends RuntimeException {
    public EventoNaoEncontradoErro(String message) {
        super(message);
    }
}
