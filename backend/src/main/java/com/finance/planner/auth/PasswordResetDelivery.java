package com.finance.planner.auth;

public interface PasswordResetDelivery {

    boolean isConfigured();

    void send(String recipient, String resetLink);
}
