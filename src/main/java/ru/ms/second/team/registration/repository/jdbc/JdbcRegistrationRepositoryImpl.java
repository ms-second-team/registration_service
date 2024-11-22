package ru.ms.second.team.registration.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JdbcRegistrationRepositoryImpl implements JdbcRegistrationRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Long> getStatusToNumberOfRegistrationsForEvent(Long eventId) {
        final String sql = "SELECT r.status, COUNT(r.status) AS reg_count FROM registrations r WHERE r.event_id = ? GROUP BY r.status";
        return jdbcTemplate.query(sql, this::extractToStatusCountMap, eventId);
    }

    private Map<String, Long> extractToStatusCountMap(ResultSet rs) throws SQLException {
        final Map<String, Long> statusToNumberOfRegistrations = new HashMap<>();
        while (rs.next()) {
            final String status = rs.getString(1);
            final Long regCount = rs.getLong(2);
            statusToNumberOfRegistrations.put(status, regCount);
        }
        return statusToNumberOfRegistrations;
    }
}
