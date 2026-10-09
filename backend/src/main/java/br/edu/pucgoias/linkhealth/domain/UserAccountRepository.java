package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {
    Optional<UserAccount> findById(UUID id);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByDocument(String document);

    List<UserAccount> findByRole(AccountRole role);

    UserAccount save(UserAccount account);
}
