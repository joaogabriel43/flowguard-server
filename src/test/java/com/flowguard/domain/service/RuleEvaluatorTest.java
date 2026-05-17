package com.flowguard.domain.service;

import com.flowguard.domain.model.FlagRule;
import com.flowguard.domain.model.RuleOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEvaluatorTest {

    private RuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RuleEvaluator();
    }

    @Test
    void shouldReturnTrueWhenNoRulesExist() {
        assertTrue(evaluator.evaluate(null, Map.of()));
        assertTrue(evaluator.evaluate(List.of(), Map.of()));
    }

    @Test
    void shouldReturnFalseWhenRulesExistButNoAttributesProvided() {
        FlagRule rule = FlagRule.builder()
                .attributeKey("plan")
                .operator(RuleOperator.EQUALS)
                .attributeValue("premium")
                .build();
        assertFalse(evaluator.evaluate(List.of(rule), null));
    }

    @Test
    void shouldEvaluateEqualsOperator() {
        FlagRule rule = FlagRule.builder()
                .attributeKey("plan")
                .operator(RuleOperator.EQUALS)
                .attributeValue("premium")
                .build();

        // Positive case
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("plan", "premium")));
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("plan", "PREMIUM"))); // Case-insensitivity

        // Negative case
        assertFalse(evaluator.evaluate(List.of(rule), Map.of("plan", "free")));
        assertFalse(evaluator.evaluate(List.of(rule), Map.of("other", "premium"))); // Missing key
    }

    @Test
    void shouldEvaluateNotEqualsOperator() {
        FlagRule rule = FlagRule.builder()
                .attributeKey("plan")
                .operator(RuleOperator.NOT_EQUALS)
                .attributeValue("free")
                .build();

        // Positive case
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("plan", "premium")));

        // Negative case
        assertFalse(evaluator.evaluate(List.of(rule), Map.of("plan", "free")));
        assertFalse(evaluator.evaluate(List.of(rule), Map.of("plan", "FREE"))); // Case-insensitivity
    }

    @Test
    void shouldEvaluateContainsOperator() {
        FlagRule rule = FlagRule.builder()
                .attributeKey("email")
                .operator(RuleOperator.CONTAINS)
                .attributeValue("@flowguard.com")
                .build();

        // Positive case
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("email", "john@flowguard.com")));
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("email", "JOHN@FLOWGUARD.COM"))); // Case-insensitivity

        // Negative case
        assertFalse(evaluator.evaluate(List.of(rule), Map.of("email", "john@gmail.com")));
    }

    @Test
    void shouldEvaluateStartsWithOperator() {
        FlagRule rule = FlagRule.builder()
                .attributeKey("region")
                .operator(RuleOperator.STARTS_WITH)
                .attributeValue("us-")
                .build();

        // Positive case
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("region", "us-east-1")));
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("region", "US-WEST-2"))); // Case-insensitivity

        // Negative case
        assertFalse(evaluator.evaluate(List.of(rule), Map.of("region", "eu-central-1")));
    }

    @Test
    void shouldEvaluateInOperator() {
        FlagRule rule = FlagRule.builder()
                .attributeKey("plan")
                .operator(RuleOperator.IN)
                .attributeValue("premium, enterprise, partner")
                .build();

        // Positive case
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("plan", "premium")));
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("plan", "enterprise")));
        assertTrue(evaluator.evaluate(List.of(rule), Map.of("plan", "  PARTNER "))); // Trimming & Case-insensitivity

        // Negative case
        assertFalse(evaluator.evaluate(List.of(rule), Map.of("plan", "free")));
    }

    @Test
    void shouldEvaluateMultipleRulesWithAndLogic() {
        FlagRule rule1 = FlagRule.builder()
                .attributeKey("plan")
                .operator(RuleOperator.EQUALS)
                .attributeValue("premium")
                .build();

        FlagRule rule2 = FlagRule.builder()
                .attributeKey("region")
                .operator(RuleOperator.STARTS_WITH)
                .attributeValue("br-")
                .build();

        List<FlagRule> rules = List.of(rule1, rule2);

        // All match
        assertTrue(evaluator.evaluate(rules, Map.of("plan", "premium", "region", "br-south")));

        // One mismatch
        assertFalse(evaluator.evaluate(rules, Map.of("plan", "free", "region", "br-south")));
        assertFalse(evaluator.evaluate(rules, Map.of("plan", "premium", "region", "us-east")));
    }
}
