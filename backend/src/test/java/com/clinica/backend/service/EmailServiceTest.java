package com.clinica.backend.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    private final EmailService emailService = new EmailService(provider);

    @Test
    void doesNotSendWhenSmtpIsNotConfigured() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertFalse(emailService.isConfigured());
        assertFalse(emailService.sendHtml("paciente@example.org", "Recordatorio", "<p>Hola</p>"));
    }

    @Test
    void sendsHtmlMessageWithRecipientAndSubject() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(provider.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(message);

        assertTrue(emailService.sendHtml("paciente@example.org", "Recordatorio de cita", "<p>Hola</p>"));

        verify(sender).send(message);
        assertEquals("Recordatorio de cita", message.getSubject());
        assertEquals("paciente@example.org", message.getAllRecipients()[0].toString());
    }

    @Test
    void returnsFalseInsteadOfThrowingWhenTheMessageIsInvalid() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        assertFalse(emailService.sendHtml("no es un correo@@", "Asunto", "<p>Hola</p>"));
        verify(sender, never()).send(any(MimeMessage.class));
    }
}
