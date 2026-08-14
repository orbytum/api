package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.EmailAnexo;
import com.orbytum.api.models.dto.request.EmailRequest;
import com.orbytum.api.models.exceptions.EmailSendingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String defaultFromAddress;
    private final String defaultFromName;

    public EmailService(
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            @Value("${orbytum.mail.from:no-reply@orbytum.com}") String defaultFromAddress,
            @Value("${orbytum.mail.from-name:Orbytum System}") String defaultFromName
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.defaultFromAddress = defaultFromAddress;
        this.defaultFromName = defaultFromName;
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendEmail(EmailRequest.comHtmlDireto(to, subject, htmlBody));
    }

    public void sendHtmlEmail(List<String> to, String subject, String htmlBody) {
        sendEmail(new EmailRequest(to, subject, htmlBody, null, null, null, null, null));
    }

    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        sendEmail(EmailRequest.comTemplate(to, subject, templateName, variables));
    }

    public void sendTemplateEmail(List<String> to, String subject, String templateName, Map<String, Object> variables) {
        sendEmail(new EmailRequest(to, subject, null, templateName, variables, null, null, null));
    }

    public void sendEmail(EmailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("O objeto EmailRequest não pode ser nulo.");
        }
        if (request.para() == null || request.para().isEmpty()) {
            throw new IllegalArgumentException("É necessário informar ao menos um destinatário.");
        }
        if (request.assunto() == null || request.assunto().isBlank()) {
            throw new IllegalArgumentException("O assunto do e-mail não pode ser vazio.");
        }

        String htmlContent = processarCorpoHtml(request);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            boolean isMultipart = (request.anexos() != null && !request.anexos().isEmpty());

            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    isMultipart,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(request.para().toArray(new String[0]));
            helper.setSubject(request.assunto());

            String remetenteEmail = (request.remetente() != null && !request.remetente().isBlank())
                    ? request.remetente()
                    : defaultFromAddress;

            String remetenteNome = (request.nomeRemetente() != null && !request.nomeRemetente().isBlank())
                    ? request.nomeRemetente()
                    : defaultFromName;

            helper.setFrom(remetenteEmail, remetenteNome);
            helper.setText(htmlContent, true);

            if (isMultipart) {
                for (EmailAnexo anexo : request.anexos()) {
                    if (anexo.contentType() != null) {
                        helper.addAttachment(anexo.nomeArquivo(), anexo.conteudo(), anexo.contentType());
                    } else {
                        helper.addAttachment(anexo.nomeArquivo(), anexo.conteudo());
                    }
                }
            }

            mailSender.send(mimeMessage);
            logger.info("E-mail com assunto '{}' enviado com sucesso para {}", request.assunto(), request.para());

        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            logger.error("Erro ao enviar e-mail para {}: {}", request.para(), e.getMessage(), e);
            throw new EmailSendingException("Falha ao processar ou enviar o e-mail: " + e.getMessage(), e);
        }
    }

    private String processarCorpoHtml(EmailRequest request) {
        if (request.templateName() != null && !request.templateName().isBlank()) {
            Context context = new Context();
            if (request.variaveis() != null) {
                context.setVariables(request.variaveis());
            }
            return templateEngine.process(request.templateName(), context);
        }

        if (request.corpoHtml() != null) {
            String html = request.corpoHtml();
            if (request.variaveis() != null && !request.variaveis().isEmpty()) {
                for (Map.Entry<String, Object> entry : request.variaveis().entrySet()) {
                    String valor = entry.getValue() != null ? entry.getValue().toString() : "";
                    html = html.replace("${" + entry.getKey() + "}", valor)
                               .replace("{{" + entry.getKey() + "}}", valor);
                }
            }
            return html;
        }

        throw new IllegalArgumentException("É necessário informar o corpoHtml ou o templateName para enviar o e-mail.");
    }
}
