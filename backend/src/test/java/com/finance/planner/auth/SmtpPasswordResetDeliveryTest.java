package com.finance.planner.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpPasswordResetDeliveryTest {

    @Mock private JavaMailSender mailSender;

    @Test
    void sendsTheResetLinkToTheAccountAddressWithoutLoggingOrExposingItElsewhere() {
        SmtpPasswordResetDelivery delivery = new SmtpPasswordResetDelivery(mailSender, "Planwise <no-reply@example.com>");

        delivery.send("owner@example.com", "https://planwise.example.test/?resetToken=sample-value");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("Planwise <no-reply@example.com>", message.getFrom());
        assertEquals("owner@example.com", message.getTo()[0]);
        assertEquals("Khôi phục mật khẩu Planwise", message.getSubject());
        assertTrue(message.getText().contains("https://planwise.example.test/?resetToken=sample-value"));
        assertTrue(message.getText().contains("30 phút"));
    }
}
