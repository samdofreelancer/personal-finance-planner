package com.finance.planner.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService, LoginAttemptService loginAttemptService,
                          SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.loginAttemptService = loginAttemptService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/bootstrap")
    public BootstrapStatusResponse bootstrap() {
        return new BootstrapStatusResponse(authService.isRegistrationAvailable());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterHttpRequest request,
                                                 HttpServletRequest servletRequest,
                                                 HttpServletResponse servletResponse) {
        RegisterRequest command = new RegisterRequest(request.email(), request.password());
        AuthResponse response = StringUtils.hasText(request.invitationToken())
                ? authService.registerWithInvitation(command, request.invitationToken())
                : authService.register(command);
        establishSession(toPrincipal(response), servletRequest, servletResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginHttpRequest request,
                                               HttpServletRequest servletRequest,
                                               HttpServletResponse servletResponse) {
        String attemptKey = loginAttemptKey(servletRequest, request.email());
        loginAttemptService.ensureAllowed(attemptKey);

        AuthenticatedUser principal;
        try {
            principal = authService.authenticate(new LoginRequest(request.email(), request.password()));
        } catch (BadCredentialsException exception) {
            loginAttemptService.recordFailure(attemptKey);
            throw exception;
        }
        loginAttemptService.recordSuccess(attemptKey);
        establishSession(principal, servletRequest, servletResponse);
        return ResponseEntity.ok(toResponse(principal));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setSecurityContextRepository(securityContextRepository);
        logoutHandler.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }
        return toResponse(principal);
    }

    private void establishSession(AuthenticatedUser principal, HttpServletRequest request,
                                  HttpServletResponse response) {
        request.getSession(true);
        request.changeSessionId();

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private AuthenticatedUser toPrincipal(AuthResponse response) {
        return new AuthenticatedUser(response.userId(), response.email(), response.householdId(), response.role());
    }

    private AuthResponse toResponse(AuthenticatedUser principal) {
        return new AuthResponse(principal.userId(), principal.email(), principal.householdId(), principal.role());
    }

    private String loginAttemptKey(HttpServletRequest request, String email) {
        return request.getRemoteAddr() + ":" + email.trim().toLowerCase(Locale.ROOT);
    }
}
