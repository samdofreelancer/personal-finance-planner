package com.finance.planner.auth;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpPasswordResetDelivery implements PasswordResetDelivery {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpPasswordResetDelivery(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public void send(String recipient, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Khôi phục mật khẩu Planwise");
        message.setText("Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Planwise của bạn.\n\n"
                + "Mở liên kết sau trong vòng 30 phút để tạo mật khẩu mới:\n" + resetLink
                + "\n\nNếu bạn không yêu cầu thao tác này, hãy bỏ qua email. Mật khẩu hiện tại vẫn được giữ nguyên.");
        mailSender.send(message);
    }
}
