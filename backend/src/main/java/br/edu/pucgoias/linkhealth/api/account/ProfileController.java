package br.edu.pucgoias.linkhealth.api.account;

import br.edu.pucgoias.linkhealth.config.MobileSessionService;
import br.edu.pucgoias.linkhealth.service.ClinicalAccessService;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProfileController {
    private final ClinicalAccessService access;
    private final UserAccountService accounts;
    private final MobileSessionService sessions;
    public ProfileController(ClinicalAccessService access, UserAccountService accounts, MobileSessionService sessions) {
        this.access = access; this.accounts = accounts; this.sessions = sessions;
    }
    @GetMapping("/api/v1/me")
    public AccountResponse get() { return AccountResponse.from(accounts.find(access.current().accountId())); }

    @PatchMapping("/api/v1/me")
    public AccountResponse save(@Valid @RequestBody Request request) {
        var id = access.current().accountId();
        var previous = accounts.find(id);
        var updated = accounts.update(id, request.fullName(), request.birthDate(), request.email(),
                request.phone(), request.currentPassword(), request.newPassword());
        if (!previous.email().equals(updated.email()) || !previous.passwordHash().equals(updated.passwordHash())) sessions.revokeAccount(id);
        return AccountResponse.from(updated);
    }
    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        access.current();
        sessions.revoke(authorization.substring(7));
        return ResponseEntity.noContent().build();
    }
    public record Request(@NotBlank @Size(max = 150) String fullName, @Past LocalDate birthDate,
            @NotBlank @Email @Size(max = 150) String email, @Size(max = 20) String phone,
            @Size(max = 72) String currentPassword, @Size(min = 8, max = 72) String newPassword) {}
}
