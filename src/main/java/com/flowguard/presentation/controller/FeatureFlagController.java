package com.flowguard.presentation.controller;

import com.flowguard.application.dto.*;
import com.flowguard.application.service.FeatureFlagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @PostMapping
    public ResponseEntity<FeatureFlagDto> createFeatureFlag(@Valid @RequestBody CreateFeatureFlagCommand command) {
        FeatureFlagDto response = featureFlagService.createFeatureFlag(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FeatureFlagDto>> listFeatureFlags() {
        List<FeatureFlagDto> response = featureFlagService.listFeatureFlags();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{key}")
    public ResponseEntity<FeatureFlagDto> getFeatureFlagByKey(@PathVariable String key) {
        FeatureFlagDto response = featureFlagService.getFeatureFlagByKey(key);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{key}")
    public ResponseEntity<FeatureFlagDto> updateFeatureFlag(
            @PathVariable String key,
            @Valid @RequestBody UpdateFeatureFlagCommand command) {
        FeatureFlagDto response = featureFlagService.updateFeatureFlag(key, command);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteFeatureFlag(@PathVariable String key) {
        featureFlagService.deleteFeatureFlag(key);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{key}/toggle")
    public ResponseEntity<FeatureFlagDto> toggleFeatureFlag(@PathVariable String key, Principal principal) {
        String performedBy = principal != null ? principal.getName() : "anonymous";
        FeatureFlagDto response = featureFlagService.toggleFeatureFlag(key, performedBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{key}/evaluate")
    public ResponseEntity<EvaluateResponse> evaluateFeatureFlag(
            @PathVariable String key,
            @Valid @RequestBody EvaluateRequest request) {
        EvaluateResponse response = featureFlagService.evaluateFlag(key, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{key}/rules")
    public ResponseEntity<FlagRuleDto> addRule(
            @PathVariable String key,
            @Valid @RequestBody CreateFlagRuleCommand command) {
        FlagRuleDto response = featureFlagService.addRule(key, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{key}/rules")
    public ResponseEntity<List<FlagRuleDto>> listRules(@PathVariable String key) {
        List<FlagRuleDto> response = featureFlagService.listRules(key);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{key}/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(
            @PathVariable String key,
            @PathVariable UUID ruleId) {
        featureFlagService.deleteRule(key, ruleId);
        return ResponseEntity.noContent().build();
    }
}
