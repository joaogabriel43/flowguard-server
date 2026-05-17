package com.flowguard.domain.util;

public final class RolloutEvaluator {

    private RolloutEvaluator() {}

    /**
     * Evaluates if a user is included in a progress rollout.
     * Formula: hash(flagKey + userId) % 100 < rolloutPercentage
     */
    public static boolean isUserInRollout(String flagKey, String userId, int rolloutPercentage) {
        if (rolloutPercentage <= 0) {
            return false;
        }
        if (rolloutPercentage >= 100) {
            return true;
        }

        int hash = MurmurHash3.hash32(flagKey + userId);
        int bucket = (hash & Integer.MAX_VALUE) % 100;
        return bucket < rolloutPercentage;
    }
}
