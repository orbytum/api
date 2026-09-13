package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record EmailRequest(
        @NotEmpty List<String> para,
        @NotNull String assunto,
        String corpoHtml,
        String templateName,
        Map<String, Object> variaveis,
        String remetente,
        String nomeRemetente,
        List<EmailAnexo> anexos
) {
    public EmailRequest {
        if (para == null) {
            para = Collections.emptyList();
        }
        if (variaveis == null) {
            variaveis = Collections.emptyMap();
        }
        if (anexos == null) {
            anexos = Collections.emptyList();
        }
    }

    public static EmailRequest comHtmlDireto(String para, String assunto, String corpoHtml) {
        return new EmailRequest(List.of(para), assunto, corpoHtml, null, null, null, null, null);
    }

    public static EmailRequest comTemplate(String para, String assunto, String templateName, Map<String, Object> variaveis) {
        return new EmailRequest(List.of(para), assunto, null, templateName, variaveis, null, null, null);
    }
}
