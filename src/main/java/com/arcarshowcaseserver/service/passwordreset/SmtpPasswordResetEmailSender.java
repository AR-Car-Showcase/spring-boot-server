package com.arcarshowcaseserver.service.passwordreset;

import com.arcarshowcaseserver.configuration.PasswordResetSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "app.password-reset", name = "smtp-enabled", havingValue = "true")
public class SmtpPasswordResetEmailSender implements PasswordResetSender {

    private final JavaMailSender mailSender;
    private final PasswordResetSecurityProperties properties;

    public SmtpPasswordResetEmailSender(JavaMailSender mailSender,
                                        PasswordResetSecurityProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendResetCode(String toEmail, String username, String code, Duration expiresIn) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFromAddress());
        message.setTo(toEmail);
        message.setSubject("Reset your AR Car Showcase password");
        message.setText("""
                Hi %s,

                We received a request to reset your password.

                Use this one-time code to continue:

                %s

                This code expires in %d minutes.

                If you did not request this reset, you can ignore this email.
                """.formatted(username, code, Math.max(1, expiresIn.toMinutes())));
        mailSender.send(message);
    }
}
