package com.etema.ragnarmmo.combat.contract;

import com.etema.ragnarmmo.combat.api.CombatResolution;

public record CombatContractResult(
        CombatResolution resolution,
        boolean fallbackUsed,
        String rejectReason) {
    public boolean rejected() {
        return rejectReason != null && !rejectReason.isBlank();
    }
}
