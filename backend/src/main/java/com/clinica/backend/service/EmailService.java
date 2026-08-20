package com.clinica.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public boolean isConfigured() {
        return mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * Envía un correo HTML. Si el correo no está configurado o el envío falla,
     * registra el motivo y devuelve false sin lanzar excepciones.
     */
    public boolean sendHtml(String to, String subject, String htmlBody) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP no configurado; no se envió el correo a {} (asunto: {})", to, subject);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            log.warn("No se pudo enviar el correo a {} (asunto: {}): {}", to, subject, e.getMessage());
            return false;
        }
    }
}
