package com.etema.ragnarmmo.system.stats.progression;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.Stats6;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JobBonusDataTest {

    @Test
    void noviceHasNoJobBonuses() {
        assertEquals(Stats6.ZERO, JobBonusData.getBonus(JobType.NOVICE, 10));
    }

    @Test
    void swordsmanExactLevel50MatchesTable() {
        assertEquals(new Stats6(9, 2, 3, 0, 3, 2), JobBonusData.getBonus(JobType.SWORDSMAN, 50));
    }

    @Test
    void knightUsesOwnTableInsteadOfSwordsmanFallback() {
        Stats6 swordsman = JobBonusData.getBonus(JobType.SWORDSMAN, 50);
        Stats6 knight = JobBonusData.getBonus(JobType.KNIGHT, 50);

        assertNotEquals(swordsman, knight);
        assertEquals(new Stats6(8, 2, 10, 0, 6, 4), knight);
    }

    @Test
    void hunterUsesExplicitSecondJobBonuses() {
        assertEquals(new Stats6(4, 6, 2, 4, 10, 4), JobBonusData.getBonus(JobType.HUNTER, 50));
    }
}
