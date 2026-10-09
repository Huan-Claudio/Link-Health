package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.AccountRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserAccountJpaSpringRepository extends JpaRepository<UserAccountEntity, UUID> {
    @Override
    @EntityGraph(attributePaths = {"patientProfile", "nutritionistProfile"})
    Optional<UserAccountEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"patientProfile", "nutritionistProfile"})
    Optional<UserAccountEntity> findByEmail(String email);

    @EntityGraph(attributePaths = {"patientProfile", "nutritionistProfile"})
    @Query("select u from UserAccountEntity u left join u.patientProfile p left join u.nutritionistProfile n "
            + "where p.cpf = :document or n.professionalRegistration = :document")
    Optional<UserAccountEntity> findByDocument(@Param("document") String document);

    @EntityGraph(attributePaths = {"patientProfile", "nutritionistProfile"})
    List<UserAccountEntity> findAllByRoleOrderByFullNameAsc(AccountRole role);
}
