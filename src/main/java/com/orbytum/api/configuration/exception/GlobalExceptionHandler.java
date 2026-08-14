package com.orbytum.api.configuration.exception;

import com.orbytum.api.models.dto.generico.ErroResponse;
import com.orbytum.api.models.exceptions.EmailJaCadastradoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoErro.class)
    public ResponseEntity<ErroResponse> handleRecursoNaoEncontrado(EmailJaCadastradoErro ex) {

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
}