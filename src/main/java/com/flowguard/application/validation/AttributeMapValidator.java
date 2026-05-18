package com.flowguard.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;

public class AttributeMapValidator implements ConstraintValidator<ValidAttributeMap, Map<String, String>> {

    private int maxEntries;
    private int maxKeyLength;
    private int maxValueLength;

    @Override
    public void initialize(ValidAttributeMap constraint) {
        this.maxEntries = constraint.maxEntries();
        this.maxKeyLength = constraint.maxKeyLength();
        this.maxValueLength = constraint.maxValueLength();
    }

    @Override
    public boolean isValid(Map<String, String> map, ConstraintValidatorContext context) {
        if (map == null) {
            return true;
        }
        if (map.size() > maxEntries) {
            return false;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey().length() > maxKeyLength) {
                return false;
            }
            if (entry.getValue() != null && entry.getValue().length() > maxValueLength) {
                return false;
            }
        }
        return true;
    }
}
