package com.flowguard.application.dto;

import com.flowguard.domain.model.RuleOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFlagRuleCommand(
    @NotBlank(message = "Attribute key is required") String attributeKey,
    @NotNull(message = "Rule operator is required") RuleOperator operator,
    @NotBlank(message = "Attribute value is required") String attributeValue
) {}
