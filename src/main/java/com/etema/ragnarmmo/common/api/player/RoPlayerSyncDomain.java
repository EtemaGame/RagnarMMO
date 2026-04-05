package com.etema.ragnarmmo.common.api.player;

public enum RoPlayerSyncDomain {
    STATS(1 << 0),
    PROGRESSION(1 << 1),
    RESOURCES(1 << 2);

    private static final int ALL_MASK = STATS.bit() | PROGRESSION.bit() | RESOURCES.bit();

    private final int bit;

    RoPlayerSyncDomain(int bit) {
        this.bit = bit;
    }

    public int bit() {
        return bit;
    }

    public static int allMask() {
        return ALL_MASK;
    }

    public static boolean includes(int mask, RoPlayerSyncDomain domain) {
        return domain != null && (mask & domain.bit()) != 0;
    }

    public static boolean requiresDerivedSync(int mask) {
        return includes(mask, STATS) || includes(mask, PROGRESSION);
    }

    public static String describeMask(int mask) {
        if (mask == 0) {
            return "none";
        }

        StringBuilder builder = new StringBuilder();
        for (RoPlayerSyncDomain domain : values()) {
            if (!includes(mask, domain)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(domain.name().toLowerCase());
        }
        return builder.toString();
    }
}
