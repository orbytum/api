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

    @ExceptionHandler(com.orbytum.api.models.exceptions.EmailSendingException.class)
    public ResponseEntity<ErroResponse> handleEmailSendingException(com.orbytum.api.models.exceptions.EmailSendingException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Falha ao enviar e-mail: " + ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }

    @ExceptionHandler({SemPermissaoConvidarErro.class, IngressoRestritoErro.class})
    public ResponseEntity<ErroResponse> handleForbidden(RuntimeException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    @ExceptionHandler(ConviteInvalidoOuExpiradoErro.class)
    public ResponseEntity<ErroResponse> handleBadRequest(ConviteInvalidoOuExpiradoErro ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler({UsuarioJaNoGrupoErro.class, ProjetoNaoPertenceAoGrupoErro.class})
    public ResponseEntity<ErroResponse> handleConflitoOuInvalido(RuntimeException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler({UsuarioNaoEncontradoErro.class, GrupoNaoEncontradoErro.class, ProjetoNaoEncontradoErro.class})
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RuntimeException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }
}