package com.arcarshowcaseserver.service.verification;

import com.arcarshowcaseserver.configuration.SignupSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "app.signup.verification", name = "smtp-enabled", havingValue = "true")
public class SmtpVerificationEmailSender implements EmailVerificationSender {

    private final JavaMailSender mailSender;
    private final SignupSecurityProperties properties;

    public SmtpVerificationEmailSender(JavaMailSender mailSender, SignupSecurityProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendVerificationCode(String toEmail, String username, String code, Duration expiresIn) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFromAddress());
        message.setTo(toEmail);
        message.setSubject("Verify your AR Car Showcase account");
        message.setText("""
                Hi %s,

                Use this verification code to complete your signup:

                %s

                This code expires in %d minutes.

                If you did not request this account, you can ignore this email.
                """.formatted(username, code, Math.max(1, expiresIn.toMinutes())));
        mailSender.send(message);
    }
}
