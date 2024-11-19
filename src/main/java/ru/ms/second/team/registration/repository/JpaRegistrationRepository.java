package ru.ms.second.team.registration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ms.second.team.registration.model.Registration;

public interface JpaRegistrationRepository extends JpaRepository<Registration, Long> {
    Page<Registration> findAllByEventId(Long eventId, Pageable pageable);
}
