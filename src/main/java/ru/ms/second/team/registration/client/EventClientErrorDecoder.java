package ru.ms.second.team.registration.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;

public class EventClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String s, Response response) {

        switch (response.status()) {
            case 404:
                return new NotFoundException("Event was not found");
            default:
                return new Exception("Unknown error");
        }
    }
}
