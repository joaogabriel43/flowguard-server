package com.flowguard.domain.util;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolloutEvaluatorPropertyTest {

    @Property
    void rolloutIsConsistent(@ForAll String flagKey, @ForAll String userId, @ForAll @IntRange(min = 0, max = 100) int percentage) {
        boolean firstRun = RolloutEvaluator.isUserInRollout(flagKey, userId, percentage);
        boolean secondRun = RolloutEvaluator.isUserInRollout(flagKey, userId, percentage);
        
        // Assert that same inputs always return the same result
        assertTrue(firstRun == secondRun);
    }

    @Property
    void zeroPercentAlwaysFalse(@ForAll String flagKey, @ForAll String userId) {
        assertFalse(RolloutEvaluator.isUserInRollout(flagKey, userId, 0));
    }

    @Property
    void hundredPercentAlwaysTrue(@ForAll String flagKey, @ForAll String userId) {
        assertTrue(RolloutEvaluator.isUserInRollout(flagKey, userId, 100));
    }

    @Property
    void bucketPercentageRelationship(@ForAll String flagKey, @ForAll String userId, @ForAll @IntRange(min = 0, max = 99) int percentage) {
        boolean inLowerRollout = RolloutEvaluator.isUserInRollout(flagKey, userId, percentage);
        boolean inHigherRollout = RolloutEvaluator.isUserInRollout(flagKey, userId, percentage + 1);

        // A user who falls into a rollout of percentage X should also fall into a rollout of percentage X + 1
        if (inLowerRollout) {
            assertTrue(inHigherRollout);
        }
    }
}
