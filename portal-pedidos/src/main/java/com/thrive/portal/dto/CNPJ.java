package com.thrive.portal.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Lab 2.3 - validador customizado de CNPJ. */
@Documented
@Constraint(validatedBy = CnpjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CNPJ {
    String message() default "CNPJ invalido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
