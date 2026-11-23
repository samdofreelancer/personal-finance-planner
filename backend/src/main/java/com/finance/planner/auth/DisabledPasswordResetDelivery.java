package com.finance.planner.auth;

public class DisabledPasswordResetDelivery implements PasswordResetDelivery {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public void send(String recipient, String resetLink) {
        throw new IllegalStateException("Password recovery email delivery is not configured.");
    }
}
