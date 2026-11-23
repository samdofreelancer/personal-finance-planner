package com.finance.planner.auth;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration(proxyBeanMethods = false)
public class PasswordResetMailConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
    PasswordResetDelivery smtpPasswordResetDelivery(
            @Value("${app.mail.host}") String host,
            @Value("${app.mail.port:587}") int port,
            @Value("${app.mail.username:}") String username,
            @Value("${app.mail.password:}") String password,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.starttls:true}") boolean startTls) {
        if (host.isBlank() || from.isBlank()) {
            throw new IllegalStateException("SMTP host and sender address are required when password recovery email is enabled.");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        if (!username.isBlank()) sender.setUsername(username);
        if (!password.isBlank()) sender.setPassword(password);
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", Boolean.toString(!username.isBlank()));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        properties.put("mail.smtp.connectiontimeout", "5000");
        properties.put("mail.smtp.timeout", "5000");
        properties.put("mail.smtp.writetimeout", "5000");
        return new SmtpPasswordResetDelivery(sender, from);
    }

    @Bean
    @ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
    PasswordResetDelivery disabledPasswordResetDelivery() {
        return new DisabledPasswordResetDelivery();
    }
}
