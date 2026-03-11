package com.anushibinj.resttosmtp.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint that requires at least one of {@code text} or {@code html} to be
 * non-blank on an {@link com.anushibinj.resttosmtp.dto.EmailProxyRequest}.
 *
 * <p>Both fields may be present simultaneously. If neither is provided the constraint fails
 * and the request is rejected with HTTP 400.
 */
@Constraint(validatedBy = AtLeastOneBodyPresentValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneBodyPresent {

    String message() default "At least one of 'text' or 'html' must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
