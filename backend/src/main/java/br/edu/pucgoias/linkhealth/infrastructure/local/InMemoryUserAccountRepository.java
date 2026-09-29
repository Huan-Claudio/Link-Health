package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import br.edu.pucgoias.linkhealth.domain.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryUserAccountRepository implements UserAccountRepository {
    private final ConcurrentMap<UUID, UserAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return accounts.values().stream().filter(account -> account.email().equals(email)).findFirst();
    }

    @Override
    public Optional<UserAccount> findByDocument(String document) {
        return accounts.values().stream().filter(account -> account.document().equals(document)).findFirst();
    }

    @Override
    public List<UserAccount> findByRole(AccountRole role) {
        return accounts.values().stream().filter(account -> account.role() == role).toList();
    }

    @Override
    public UserAccount save(UserAccount account) {
        accounts.put(account.id(), account);
        return account;
    }
}
