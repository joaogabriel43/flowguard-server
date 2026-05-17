package com.flowguard.application.dto;

import com.flowguard.domain.model.FlagRule;
import com.flowguard.domain.model.RuleOperator;

import java.util.UUID;

public record FlagRuleDto(
    UUID id,
    String attributeKey,
    RuleOperator operator,
    String attributeValue
) {
    public static FlagRuleDto fromEntity(FlagRule rule) {
        return new FlagRuleDto(
            rule.getId(),
            rule.getAttributeKey(),
            rule.getOperator(),
            rule.getAttributeValue()
        );
    }
}
