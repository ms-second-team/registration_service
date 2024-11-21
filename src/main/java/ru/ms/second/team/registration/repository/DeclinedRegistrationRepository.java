package ru.ms.second.team.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ms.second.team.registration.model.DeclinedRegistration;

public interface DeclinedRegistrationRepository extends JpaRepository<DeclinedRegistration, Long> {

    void deleteAllByRegistrationId(Long id);
}
