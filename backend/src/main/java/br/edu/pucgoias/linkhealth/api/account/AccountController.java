package br.edu.pucgoias.linkhealth.api.account;

import br.edu.pucgoias.linkhealth.config.MobileSessionService;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "postgres"})
@RequestMapping("/api/v1")
public class AccountController {
    private final UserAccountService service;
    private final MobileSessionService mobileSessions;

    public AccountController(UserAccountService service, MobileSessionService mobileSessions) {
        this.service = service;
        this.mobileSessions = mobileSessions;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterAccountRequest request) {
        UserAccount account = service.register(request.fullName(), request.birthDate(), request.email(),
                request.phone(), request.document(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping("/auth/login")
    public AccountResponse login(@Valid @RequestBody LoginRequest request) {
        return AccountResponse.from(service.checkCredentials(request.email(), request.password()));
    }

    @PostMapping("/auth/mobile-session")
    public MobileSessionResponse mobileSession(@Valid @RequestBody LoginRequest request) {
        UserAccount account = service.checkCredentials(request.email(), request.password());
        MobileSessionService.IssuedSession session = mobileSessions.create(account);
        return MobileSessionResponse.from(account, session.token(), session.expiresAt());
    }

    @GetMapping("/patients/search")
    public List<AccountResponse> searchPatients(@RequestParam String query) {
        return service.searchPatients(query).stream().map(AccountResponse::from).toList();
    }
}
