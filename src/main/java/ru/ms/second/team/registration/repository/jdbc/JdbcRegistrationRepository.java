package ru.ms.second.team.registration.repository.jdbc;

import java.util.Map;

public interface JdbcRegistrationRepository {

    Map<String, Long> getStatusToNumberOfRegistrationsForEvent(Long eventId);
}
