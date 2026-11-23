package com.finance.planner.auth;

public class PasswordResetDeliveryFailedException extends RuntimeException {

    public PasswordResetDeliveryFailedException() {
        super("Không thể gửi email khôi phục lúc này. Vui lòng thử lại sau.");
    }
}
