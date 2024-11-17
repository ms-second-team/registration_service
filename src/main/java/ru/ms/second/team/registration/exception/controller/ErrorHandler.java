package ru.ms.second.team.registration.exception.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;
import ru.ms.second.team.registration.exception.exceptions.PasswordIncorrectException;
import ru.ms.second.team.registration.exception.model.ErrorResponse;
import ru.ms.second.team.registration.exception.model.Response;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final NotFoundException e) {
        log.warn("{}, {}", Response.NOT_FOUND, e.getLocalizedMessage());
        return new ErrorResponse(Response.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePasswordIncorrectException(final PasswordIncorrectException e) {
        log.warn("{}, {}", Response.BAD_REQUEST, e.getLocalizedMessage());
        return new ErrorResponse(Response.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleDataIntegrityException(final ConstraintViolationException e) {
        log.warn("{}, {}", Response.BAD_REQUEST, e.getLocalizedMessage());
        return new ErrorResponse(Response.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityException(final DataIntegrityViolationException e) {
        log.warn("{}, {}", Response.CONFLICT, e.getLocalizedMessage());
        return new ErrorResponse(Response.CONFLICT, e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        log.warn("{}, {}", Response.BAD_REQUEST, e.getLocalizedMessage());
        return new ErrorResponse(Response.BAD_REQUEST, e.getMessage());
    }
}
