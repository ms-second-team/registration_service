package ru.ms.second.team.registration.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import ru.ms.second.team.registration.config.EventClientConfig;
import ru.ms.second.team.registration.dto.event.EventDto;
import ru.ms.second.team.registration.dto.event.TeamMemberDto;

import java.util.List;

@FeignClient(name = "eventClient", url = "${app.event-service.url}", configuration = EventClientConfig.class)
public interface EventClient {

    @GetMapping("/events/{eventId}")
    ResponseEntity<EventDto> getEventById(@RequestHeader("X-User-Id") Long userId, @PathVariable Long eventId);

    @GetMapping("/events/teams/{eventId}")
    ResponseEntity<List<TeamMemberDto>> getTeamsByEventId(@RequestHeader("X-User-Id") Long userId, @PathVariable Long eventId);
}
