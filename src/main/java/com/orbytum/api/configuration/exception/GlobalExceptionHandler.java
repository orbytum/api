package com.orbytum.api.configuration.exception;

import com.orbytum.api.models.dto.generico.ErroResponse;
import com.orbytum.api.models.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/*
 * Toda exceção precisa ser tratada aqui: sem handler, o Spring encaminha o erro para /error,
 * rota que a configuração de segurança bloqueia, e o cliente recebe um 403 vazio no lugar do erro real.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({AccessDeniedException.class, org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<ErroResponse> handleAccessDenied(Exception ex) {
        String mensagem = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Você não possui permissão para realizar esta operação.";
        ErroResponse erro = new ErroResponse(
                HttpStatus.FORBIDDEN.value(),
                mensagem,
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(EmailJaCadastradoErro.class)
    public ResponseEntity<ErroResponse> handleEmailJaCadastrado(EmailJaCadastradoErro ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.CONFLICT.value(),
                "Ops! " + ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(CrenciaisInvalidas.class)
    public ResponseEntity<ErroResponse> handleCrenciaisInvalidas(CrenciaisInvalidas ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Ops! " + ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(ConviteRestritoErro.class)
    public ResponseEntity<ErroResponse> handleConviteRestrito(ConviteRestritoErro ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    @ExceptionHandler(ContaDesativadaErro.class)
    public ResponseEntity<ErroResponse> handleContaDesativada(ContaDesativadaErro ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.FORBIDDEN.value(),
                "Sua conta está desativada. Procure o administrador do sistema.",
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
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

    @ExceptionHandler(LinkPreviewIndisponivelErro.class)
    public ResponseEntity<ErroResponse> handleLinkPreviewIndisponivel(LinkPreviewIndisponivelErro ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.BAD_GATEWAY.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(erro);
    }

    @ExceptionHandler(ArquivoStorageException.class)
    public ResponseEntity<ErroResponse> handleArquivoStorageException(ArquivoStorageException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Falha na operação com o armazenamento de arquivos: " + ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }

    @ExceptionHandler({SemPermissaoConvidarErro.class, IngressoRestritoErro.class, SemPermissaoNoGrupoErro.class})
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

    @ExceptionHandler({UsuarioJaNoGrupoErro.class, ProjetoNaoPertenceAoGrupoErro.class, GrupoJaPossuiLiderErro.class, AtividadeNaoPertenceAoProjetoErro.class, EditalJaVinculadoErro.class})
    public ResponseEntity<ErroResponse> handleConflitoOuInvalido(RuntimeException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler({TransicaoAtividadeInvalidaErro.class, ResponsavelNaoPertenceAoProjetoErro.class, NoticiaNaoPendenteErro.class})
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(RuntimeException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler({UsuarioNaoEncontradoErro.class, GrupoNaoEncontradoErro.class, ProjetoNaoEncontradoErro.class, PublicacaoNaoEncontradaErro.class, AtividadeNaoEncontradaErro.class, LembreteNaoEncontradoErro.class, EventoNaoEncontradoErro.class, EditalNaoEncontradoErro.class, NoticiaNaoEncontradaErro.class})
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RuntimeException ex) {
        ErroResponse erro = new ErroResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    /** Falha de validação dos DTOs (@NotBlank, @Size…), em JSON (@RequestBody) ou multipart (@ModelAttribute). */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErroResponse> handleValidacao(BindException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(" "));
        if (mensagem.isBlank()) {
            mensagem = "Dados inválidos. Verifique os campos informados.";
        }
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), mensagem, System.currentTimeMillis()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(HttpStatus.BAD_REQUEST.value(),
                "Não foi possível ler os dados enviados. Verifique os campos informados.", System.currentTimeMillis()));
    }

    /** Restrições do banco (tamanho de coluna, unicidade, chave estrangeira…) que escaparam da validação. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase();
        String mensagem = causa.contains("too long")
                ? "Um dos campos excede o tamanho máximo permitido."
                : "Não foi possível salvar: os dados violam uma restrição do sistema.";
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), mensagem, System.currentTimeMillis()));
    }

    /** Último recurso: erros do Spring mantêm o status próprio (404, 405…); o resto vira 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleInesperado(Exception ex) {
        if (ex instanceof ErrorResponse respostaSpring) {
            HttpStatusCode status = respostaSpring.getStatusCode();
            String detalhe = respostaSpring.getBody().getDetail();
            return ResponseEntity.status(status).body(new ErroResponse(status.value(),
                    detalhe != null ? detalhe : "Não foi possível processar a requisição.", System.currentTimeMillis()));
        }
        log.error("Erro inesperado ao processar a requisição", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErroResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocorreu um erro inesperado. Tente novamente.", System.currentTimeMillis()));
    }
}
