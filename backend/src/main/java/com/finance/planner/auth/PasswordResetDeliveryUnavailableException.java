package com.finance.planner.auth;

public class PasswordResetDeliveryUnavailableException extends RuntimeException {

    public PasswordResetDeliveryUnavailableException() {
        super("Chức năng khôi phục mật khẩu chưa được cấu hình gửi email. Vui lòng liên hệ quản trị viên.");
    }
}
