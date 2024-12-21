package ru.ms.second.team.registration.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Password must be at least 8 characters long, " +
            "include an uppercase letter, a lowercase letter, a digit, " +
            "and a special character \"!@#$%^&*()-+_\".";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}