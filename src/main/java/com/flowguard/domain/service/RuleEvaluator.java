package com.flowguard.domain.service;

import com.flowguard.domain.model.FlagRule;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class RuleEvaluator {

    /**
     * Evaluates if a set of user attributes satisfies all segmentation rules of a feature flag.
     * Union is strictly an AND logical gate.
     */
    public boolean evaluate(List<FlagRule> rules, Map<String, String> attributes) {
        if (rules == null || rules.isEmpty()) {
            return true; // No rules means vacuously satisfied
        }

        if (attributes == null) {
            return false; // Rules exist but no attributes provided -> rules are not satisfied
        }

        for (FlagRule rule : rules) {
            if (!evaluateRule(rule, attributes)) {
                return false; // AND logic: any rule fail means entire evaluation fails
            }
        }

        return true; // All rules satisfied
    }

    private boolean evaluateRule(FlagRule rule, Map<String, String> attributes) {
        String userValue = attributes.get(rule.getAttributeKey());
        if (userValue == null) {
            return false; // Attribute not provided by user -> rule fails
        }

        String ruleValue = rule.getAttributeValue();
        if (ruleValue == null) {
            return false;
        }

        return switch (rule.getOperator()) {
            case EQUALS -> userValue.equalsIgnoreCase(ruleValue);
            case NOT_EQUALS -> !userValue.equalsIgnoreCase(ruleValue);
            case CONTAINS -> userValue.toLowerCase().contains(ruleValue.toLowerCase());
            case STARTS_WITH -> userValue.toLowerCase().startsWith(ruleValue.toLowerCase());
            case IN -> evaluateInOperator(userValue, ruleValue);
        };
    }

    private boolean evaluateInOperator(String userValue, String ruleValue) {
        return Arrays.stream(ruleValue.split(","))
                .map(String::trim)
                .anyMatch(val -> val.equalsIgnoreCase(userValue.trim()));
    }
}
