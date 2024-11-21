package ru.ms.second.team.registration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.ms.second.team.registration.model.Registration;
import ru.ms.second.team.registration.model.RegistrationStatus;

import java.util.List;

public interface JpaRegistrationRepository extends JpaRepository<Registration, Long> {
    Page<Registration> findAllByEventId(Long eventId, Pageable pageable);

    @Query("SELECT r FROM Registration r WHERE r.status = 'APPROVED' ORDER BY r.createdAt ASC")
    Registration findEarliestWaitingRegistration();

    @Query("SELECT r FROM Registration r WHERE r.status IN (:statuses) AND r.eventId = :eventId ORDER BY r.createdAt ASC ")
    List<Registration> searchRegistrations(List<RegistrationStatus> statuses, Long eventId);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.status = :status AND r.eventId = :eventId")
    long getRegistrationsCountByStatusAndEventId(RegistrationStatus status, Long eventId);
}
