package ru.ms.second.team.registration.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ms.second.team.registration.client.user.UserClientErrorDecoder;

@Configuration
public class UserClientConfig {
    @Bean
    public ErrorDecoder userClientErrorDecoder() {
        return new UserClientErrorDecoder();
    }
}
