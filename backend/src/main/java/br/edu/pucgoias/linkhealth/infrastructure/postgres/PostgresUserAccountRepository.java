package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import br.edu.pucgoias.linkhealth.domain.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
@Transactional(readOnly = true)
public class PostgresUserAccountRepository implements UserAccountRepository {
    private final UserAccountJpaSpringRepository repository;

    public PostgresUserAccountRepository(UserAccountJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(id).map(UserAccountEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmail(email).map(UserAccountEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findByDocument(String document) {
        return repository.findByDocument(document).map(UserAccountEntity::toDomain);
    }

    @Override
    public List<UserAccount> findByRole(AccountRole role) {
        return repository.findAllByRoleOrderByFullNameAsc(role).stream().map(UserAccountEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public UserAccount save(UserAccount account) {
        try {
            return repository.saveAndFlush(UserAccountEntity.from(account)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new InvalidRequestException("Não foi possível salvar a conta. Verifique o e-mail e o documento.");
        }
    }
}
