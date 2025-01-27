package ru.ms.second.team.registration.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import ru.ms.second.team.registration.config.UserClientConfig;
import ru.ms.second.team.registration.dto.user.NewUserRequest;
import ru.ms.second.team.registration.dto.user.UserCredentials;
import ru.ms.second.team.registration.dto.user.UserDto;

@FeignClient(name = "userClient", url = "${app.user-service.url}", configuration = UserClientConfig.class)
public interface UserClient {
    @PostMapping("/users")
    UserDto createUser(@RequestBody NewUserRequest newUserRequest);

    @GetMapping("/users/{id}")
    UserDto findUserByUserId(@RequestHeader("X-User-Id") Long userId,
                             @PathVariable Long id);

    @PostMapping("/users/email")
    UserDto findUserByEmail(@RequestBody UserCredentials userCredentials);

    @DeleteMapping("/users")
    void deleteUser(@RequestHeader("X-User-Id") Long userId,
                    @RequestHeader("X-User-Password") String userPassword);
}
