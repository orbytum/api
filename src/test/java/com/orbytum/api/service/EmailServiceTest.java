package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.EmailRequest;
import com.orbytum.api.models.exceptions.EmailSendingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    private EmailService emailService;

    private final String defaultFrom = "no-reply@orbytum.com";
    private final String defaultFromName = "Orbytum System";

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, templateEngine, defaultFrom, defaultFromName);
    }

    @Test
    @DisplayName("Deve enviar e-mail de convite para xhorizon3@gmail.com usando o template convite-template com sucesso")
    void sendConviteEmail_ParaXhorizon3_ComTemplateConvite() {
        String destinatario = "xhorizon3@gmail.com";
        String assunto = "Convite para participar da organização Orbytum";
        String templateName = "convite-template";
        Map<String, Object> variaveis = Map.of(
                "nomeOrganizacao", "Orbytum Dev Team",
                "loginUrl", "https://app.orbytum.com/cadastro?convite=token123"
        );

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/convite-template"), any(IContext.class)))
                .thenReturn("<html><body><h1>Convite Orbytum para xhorizon3@gmail.com</h1></body></html>");

        emailService.sendTemplateEmail(destinatario, assunto, templateName, variaveis);

        ArgumentCaptor<IContext> contextCaptor = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine, times(1)).process(eq("email/convite-template"), contextCaptor.capture());

        IContext capturedContext = contextCaptor.getValue();
        assertEquals("Orbytum Dev Team", capturedContext.getVariable("nomeOrganizacao"));
        assertEquals("https://app.orbytum.com/cadastro?convite=token123", capturedContext.getVariable("loginUrl"));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendHtmlEmail_ComSucesso() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendHtmlEmail(
                "xhorizon3@gmail.com",
                "Bem-vindo ao Orbytum",
                "<h1>Olá!</h1>"
        ));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendTemplateEmail_ComSucesso() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/bem-vindo"), any(IContext.class)))
                .thenReturn("<html><body><h1>Bem-vindo!</h1></body></html>");

        Map<String, Object> vars = Map.of("nomeUsuario", "João");

        assertDoesNotThrow(() -> emailService.sendTemplateEmail(
                "xhorizon3@gmail.com",
                "Boas-vindas!",
                "email/bem-vindo",
                vars
        ));

        verify(templateEngine, times(1)).process(eq("email/bem-vindo"), any(IContext.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_SubstituicaoVariaveisHtmlDireto() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailRequest request = new EmailRequest(
                List.of("xhorizon3@gmail.com"),
                "Teste de Variáveis",
                "<p>Olá ${nome}, seu código é {{codigo}}</p>",
                null,
                Map.of("nome", "User", "codigo", "987654"),
                null,
                null,
                null
        );

        assertDoesNotThrow(() -> emailService.sendEmail(request));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_FalhaMailSender_LancaEmailSendingException() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Erro de conexão SMTP")).when(mailSender).send(any(MimeMessage.class));

        EmailRequest request = EmailRequest.comHtmlDireto("xhorizon3@gmail.com", "Assunto", "<p>Corpo</p>");

        EmailSendingException exception = assertThrows(
                EmailSendingException.class,
                () -> emailService.sendEmail(request)
        );

        assertTrue(exception.getMessage().contains("Falha ao processar ou enviar o e-mail"));
    }

    @Test
    void sendEmail_ParametrosInvalidos_LancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmail(null));

        EmailRequest semDestinatarios = new EmailRequest(
                List.of(), "Assunto", "<p>Corpo</p>", null, null, null, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmail(semDestinatarios));

        EmailRequest semAssunto = new EmailRequest(
                List.of("xhorizon3@gmail.com"), "", "<p>Corpo</p>", null, null, null, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmail(semAssunto));

        EmailRequest semCorpoEConteudo = new EmailRequest(
                List.of("xhorizon3@gmail.com"), "Assunto", null, null, null, null, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmail(semCorpoEConteudo));
    }
}
