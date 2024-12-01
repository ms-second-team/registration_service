package ru.ms.second.team.registration.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ms.second.team.registration.client.EventClientErrorDecoder;

@Configuration
public class EventClientConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new EventClientErrorDecoder();
    }
}
