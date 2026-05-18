package com.flowguard.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AttributeMapValidator.class)
@Documented
public @interface ValidAttributeMap {

    String message() default "Attribute map must have at most {maxEntries} entries, "
            + "keys up to {maxKeyLength} chars and values up to {maxValueLength} chars";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int maxEntries() default 50;

    int maxKeyLength() default 50;

    int maxValueLength() default 200;
}
