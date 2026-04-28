package com.etema.ragnarmmo.combat.api;

import net.minecraft.world.entity.LivingEntity;

public record ResolvedTargetCandidate(
        int entityId,
        LivingEntity target,
        TargetRejectReason rejectReason) {

    public ResolvedTargetCandidate {
        rejectReason = rejectReason == null ? TargetRejectReason.NONE : rejectReason;
    }

    public boolean accepted() {
        return target != null && rejectReason == TargetRejectReason.NONE;
    }

    public static ResolvedTargetCandidate accepted(LivingEntity target) {
        return new ResolvedTargetCandidate(target.getId(), target, TargetRejectReason.NONE);
    }

    public static ResolvedTargetCandidate rejected(int entityId, TargetRejectReason reason) {
        return new ResolvedTargetCandidate(entityId, null, reason);
    }
}
