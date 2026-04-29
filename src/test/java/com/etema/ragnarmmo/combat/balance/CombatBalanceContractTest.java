package com.etema.ragnarmmo.combat.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.etema.ragnarmmo.mobs.profile.MobTier;
import org.junit.jupiter.api.Test;

class CombatBalanceContractTest {
    @Test
    void ttkBandsMatchP4Contract() {
        assertEquals(new TtkBand(5, 8), CombatBalanceContract.targetTtk(MobTier.WEAK));
        assertEquals(new TtkBand(10, 15), CombatBalanceContract.targetTtk(MobTier.NORMAL));
        assertEquals(new TtkBand(20, 40), CombatBalanceContract.targetTtk(MobTier.ELITE));
        assertThrows(IllegalArgumentException.class, () -> CombatBalanceContract.targetTtk(MobTier.BOSS));
    }

    @Test
    void ttkBandContainsOnlyItsRange() {
        TtkBand band = new TtkBand(10, 15);

        assertTrue(band.contains(10));
        assertTrue(band.contains(15));
        assertThrows(IllegalArgumentException.class, () -> new TtkBand(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TtkBand(8, 7));
    }
}
