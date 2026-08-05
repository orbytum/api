package com.orbytum.api.configuration.exception;

import com.orbytum.api.models.dto.generico.ErroResponse;
import com.orbytum.api.models.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoErro.class)
    public ResponseEntity<ErroResponse> handleEmailJaCadastrado(EmailJaCadastradoErro ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.CONFLICT.value(),
                "Ops! " + ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(SemPermissaoConvidarErro.class)
    public ResponseEntity<ErroResponse> handleSemPermissaoConvidar(SemPermissaoConvidarErro ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    @ExceptionHandler(UsuarioJaNoGrupoErro.class)
    public ResponseEntity<ErroResponse> handleUsuarioJaNoGrupo(UsuarioJaNoGrupoErro ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler({UsuarioNaoEncontradoErro.class, GrupoNaoEncontradoErro.class})
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RuntimeException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }
}