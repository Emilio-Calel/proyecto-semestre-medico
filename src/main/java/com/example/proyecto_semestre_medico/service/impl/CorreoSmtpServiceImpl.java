package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.service.CorreoService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class CorreoSmtpServiceImpl
        implements CorreoService {

    private final JavaMailSender javaMailSender;

    @Value("${app.correo.remitente}")
    private String remitente;

    public CorreoSmtpServiceImpl(
            JavaMailSender javaMailSender
    ) {

        this.javaMailSender =
                javaMailSender;
    }

    @Override
    public void enviar(
            String destinatario,
            String asunto,
            String contenido
    ) {

        if (destinatario == null
                || destinatario.isBlank()) {

            throw new IllegalArgumentException(
                    "El destinatario del correo es obligatorio."
            );
        }

        if (remitente == null
                || remitente.isBlank()) {

            throw new IllegalStateException(
                    "No se ha configurado el correo remitente."
            );
        }

        try {

            MimeMessage mensaje =
                    javaMailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mensaje,
                            false,
                            "UTF-8"
                    );

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);

            boolean esHtml =
                    contenido != null
                            && (
                            contenido.contains("<html")
                                    || contenido.contains("<!DOCTYPE html")
                    );

            helper.setText(
                    contenido,
                    esHtml
            );

            javaMailSender.send(mensaje);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "No fue posible enviar el correo electrónico.",
                    e
            );
        }
    }
}