package com.finance.planner.household;

import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.finance.planner.security.HouseholdContextResolver;

@RestController
@RequestMapping("/api/household/invitations")
public class HouseholdInvitationController {

    private final InvitationService invitationService;
    private final HouseholdContextResolver contextResolver;

    public HouseholdInvitationController(InvitationService invitationService,
                                         HouseholdContextResolver contextResolver) {
        this.invitationService = invitationService;
        this.contextResolver = contextResolver;
    }

    @GetMapping
    public java.util.List<InvitationSummary> list() {
        return invitationService.listInvitations(contextResolver.requireContext());
    }

    @PostMapping
    public ResponseEntity<CreatedInvitation> create(@Valid @RequestBody(required = false) InvitationCreateRequest request) {
        Integer validityDays = request == null ? null : request.validityDays();
        Duration validity = validityDays == null ? null : Duration.ofDays(validityDays);
        CreatedInvitation created = invitationService.createInvitation(contextResolver.requireContext(), validity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        invitationService.revoke(contextResolver.requireContext(), id);
        return ResponseEntity.noContent().build();
    }
}
