package com.orbytum.api.models.exceptions;

/** Convite nominal acessado sem sessão ou por uma conta diferente da do e-mail convidado. */
public class ConviteRestritoErro extends RuntimeException {
    public ConviteRestritoErro(String message) {
        super(message);
    }
}
