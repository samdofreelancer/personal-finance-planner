package com.finance.planner.auth;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        String displayName = request.displayName() == null ? "" : request.displayName().trim();
        if (displayName.isBlank() || displayName.length() > 80) {
            throw new IllegalArgumentException("Tên hiển thị phải có từ 1 đến 80 ký tự.");
        }
        String avatarUrl = normalizeAvatarUrl(request.avatarUrl());
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        return toResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        String currentPassword = request.currentPassword();
        if (currentPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        String newPassword = request.newPassword();
        if (newPassword.length() < MIN_PASSWORD_LENGTH
                || newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 12 ký tự và tối đa 72 byte UTF-8.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản."));
    }

    private String normalizeAvatarUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String candidate = value.trim();
        try {
            URI uri = URI.create(candidate);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Ảnh đại diện phải là URL HTTPS hợp lệ.");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Ảnh đại diện phải là URL HTTPS hợp lệ.");
        }
        return candidate;
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl(), user.getCreatedAt());
    }
}
