package com.orbytum.api.models.exceptions;

public class UsuarioJaNoGrupoErro extends RuntimeException {
    public UsuarioJaNoGrupoErro(String message) {
        super(message);
    }
}
