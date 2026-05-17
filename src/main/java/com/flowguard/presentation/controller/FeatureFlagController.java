package com.flowguard.presentation.controller;

import com.flowguard.application.dto.CreateFeatureFlagCommand;
import com.flowguard.application.dto.FeatureFlagDto;
import com.flowguard.application.dto.UpdateFeatureFlagCommand;
import com.flowguard.application.service.FeatureFlagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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
}
