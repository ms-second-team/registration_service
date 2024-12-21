package ru.ms.second.team.registration.client.user;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;
import ru.ms.second.team.registration.exception.exceptions.PasswordIncorrectException;

public class UserClientErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String s, Response response) {

        return switch (response.status()) {
            case 400 -> new PasswordIncorrectException("Password is incorrect");
            case 404 -> new NotFoundException("User was not found");
            default -> new Exception("Unknown error");
        };
    }
}