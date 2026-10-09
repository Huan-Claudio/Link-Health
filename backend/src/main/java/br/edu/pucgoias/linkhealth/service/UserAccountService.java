package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import br.edu.pucgoias.linkhealth.domain.UserAccountRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class UserAccountService {
    private final UserAccountRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserAccountService(UserAccountRepository repository) {
        this.repository = repository;
    }

    public synchronized UserAccount register(
            String fullName,
            LocalDate birthDate,
            String email,
            String phone,
            String document,
            String password,
            AccountRole role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedDocument = document.trim().toUpperCase(Locale.ROOT);
        if (repository.findByEmail(normalizedEmail).isPresent()) {
            throw new InvalidRequestException("Este e-mail já está cadastrado.");
        }
        if (repository.findByDocument(normalizedDocument).isPresent()) {
            throw new InvalidRequestException("Este documento já está cadastrado.");
        }
        UserAccount account = new UserAccount(
                UUID.randomUUID(),
                fullName.trim(),
                birthDate,
                normalizedEmail,
                phone == null ? null : phone.trim(),
                normalizedDocument,
                passwordEncoder.encode(password),
                role,
                Instant.now());
        return repository.save(account);
    }

    public UserAccount checkCredentials(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        UserAccount account = repository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidRequestException("E-mail ou senha inválidos."));
        if (!passwordEncoder.matches(password, account.passwordHash())) {
            throw new InvalidRequestException("E-mail ou senha inválidos.");
        }
        return account;
    }

    public UserAccount find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    public List<UserAccount> searchPatients(String query) {
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return List.of();
        }
        String digits = text.replaceAll("\\D", "");
        return repository.findByRole(AccountRole.PACIENTE).stream()
                .filter(account -> account.fullName().toLowerCase(Locale.ROOT).startsWith(text)
                        || account.email().startsWith(text)
                        || (!digits.isEmpty() && account.document().replaceAll("\\D", "").startsWith(digits)))
                .toList();
    }
}
