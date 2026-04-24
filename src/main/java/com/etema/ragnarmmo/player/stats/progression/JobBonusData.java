package com.etema.ragnarmmo.player.stats.progression;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.Stats6;

import java.util.EnumMap;
import java.util.Map;

public final class JobBonusData {

    private static final int STR = 0;
    private static final int AGI = 1;
    private static final int VIT = 2;
    private static final int INT = 3;
    private static final int DEX = 4;
    private static final int LUK = 5;

    private static final Map<JobType, Stats6[]> TABLES = new EnumMap<>(JobType.class);

    static {
        TABLES.put(JobType.NOVICE, createEmptyTable(10));

        TABLES.put(JobType.SWORDSMAN, buildTable(50,
                point(4, STR), point(6, DEX), point(8, VIT), point(11, STR), point(13, AGI),
                point(21, STR), point(24, LUK), point(26, VIT), point(29, DEX), point(31, STR),
                point(34, LUK), point(36, AGI), point(38, STR), point(41, DEX), point(42, STR),
                point(45, VIT), point(46, STR), point(49, STR), point(50, STR)));

        TABLES.put(JobType.MAGE, buildTable(50,
                point(2, INT), point(6, DEX), point(10, DEX), point(14, INT), point(18, AGI),
                point(22, INT), point(26, AGI), point(30, LUK), point(33, INT), point(36, DEX),
                point(38, INT), point(40, AGI), point(42, LUK), point(44, INT), point(46, INT),
                point(47, AGI), point(49, LUK), point(50, INT)));

        TABLES.put(JobType.ARCHER, buildTable(50,
                point(2, DEX), point(6, STR), point(10, INT), point(14, DEX), point(18, DEX),
                point(22, LUK), point(26, AGI), point(30, DEX), point(33, AGI), point(36, DEX),
                point(38, STR), point(40, STR), point(42, DEX), point(44, LUK), point(46, VIT),
                point(47, INT), point(49, AGI), point(50, DEX)));

        TABLES.put(JobType.THIEF, buildTable(50,
                point(2, AGI), point(6, STR), point(10, DEX), point(14, VIT), point(18, INT),
                point(22, DEX), point(26, LUK), point(30, STR), point(33, AGI), point(36, AGI),
                point(38, STR), point(40, LUK), point(42, DEX), point(44, VIT), point(46, LUK),
                point(47, STR), point(49, DEX), point(50, AGI)));

        TABLES.put(JobType.MERCHANT, buildTable(50,
                point(2, VIT), point(6, DEX), point(10, STR), point(14, DEX), point(18, VIT),
                point(22, STR), point(26, INT), point(30, VIT), point(33, AGI), point(36, LUK),
                point(38, DEX), point(40, STR), point(42, DEX), point(44, STR), point(46, LUK),
                point(47, VIT), point(49, STR), point(50, DEX)));

        TABLES.put(JobType.ACOLYTE, buildTable(50,
                point(2, LUK), point(6, VIT), point(10, INT), point(14, DEX), point(18, LUK),
                point(22, AGI), point(26, STR), point(30, VIT), point(33, INT), point(36, DEX),
                point(38, LUK), point(40, AGI), point(42, STR), point(44, VIT), point(46, INT),
                point(47, DEX), point(49, STR), point(50, LUK)));

        TABLES.put(JobType.KNIGHT, buildTable(50,
                point(1, VIT), point(3, VIT), point(4, STR), point(5, LUK), point(8, VIT),
                point(10, STR), point(11, DEX), point(12, VIT), point(13, AGI), point(15, STR),
                point(17, VIT), point(18, VIT), point(19, DEX), point(20, LUK), point(21, STR),
                point(23, VIT), point(27, STR), point(28, LUK), point(29, VIT), point(31, DEX),
                point(33, STR), point(36, VIT), point(37, LUK), point(38, AGI), point(40, DEX),
                point(43, VIT), point(46, STR), point(47, STR), point(48, DEX), point(49, DEX)));

        TABLES.put(JobType.WIZARD, buildTable(50,
                point(1, INT), point(2, DEX), point(4, INT), point(5, DEX), point(6, AGI),
                point(9, INT), point(10, AGI), point(12, STR), point(13, DEX), point(15, LUK),
                point(18, INT), point(22, INT), point(24, AGI), point(26, DEX), point(29, INT),
                point(31, INT), point(32, DEX), point(33, INT), point(34, AGI), point(36, LUK),
                point(38, VIT), point(39, DEX), point(40, INT), point(41, AGI), point(43, AGI),
                point(45, INT), point(46, AGI), point(47, AGI), point(48, INT), point(50, INT)));

        TABLES.put(JobType.HUNTER, buildTable(50,
                point(1, DEX), point(3, INT), point(4, DEX), point(5, LUK), point(6, STR),
                point(8, DEX), point(10, STR), point(11, STR), point(12, AGI), point(14, DEX),
                point(15, LUK), point(17, VIT), point(19, AGI), point(20, AGI), point(21, DEX),
                point(23, VIT), point(27, DEX), point(29, LUK), point(31, AGI), point(33, DEX),
                point(34, INT), point(38, DEX), point(39, AGI), point(41, INT), point(42, LUK),
                point(43, DEX), point(44, STR), point(46, INT), point(47, AGI), point(49, DEX)));

        TABLES.put(JobType.PRIEST, buildTable(50,
                point(1, LUK), point(3, LUK), point(4, STR), point(6, AGI), point(7, VIT),
                point(8, INT), point(9, INT), point(10, LUK), point(11, STR), point(14, VIT),
                point(16, DEX), point(17, STR), point(20, DEX), point(21, LUK), point(22, INT),
                point(25, DEX), point(27, STR), point(29, AGI), point(31, LUK), point(32, DEX),
                point(34, VIT), point(35, STR), point(36, VIT), point(37, AGI), point(39, LUK),
                point(42, INT), point(43, INT), point(45, VIT), point(48, AGI), point(50, LUK)));

        TABLES.put(JobType.ASSASSIN, buildTable(50,
                point(1, AGI), point(2, AGI), point(3, AGI), point(4, INT), point(6, VIT),
                point(8, VIT), point(9, DEX), point(11, STR), point(14, INT), point(15, AGI),
                point(16, AGI), point(17, AGI), point(18, AGI), point(19, AGI), point(20, AGI),
                point(21, AGI), point(24, DEX), point(25, STR), point(27, STR), point(30, DEX),
                point(31, DEX), point(32, STR), point(38, INT), point(40, DEX), point(41, DEX),
                point(42, INT), point(45, STR), point(46, DEX), point(48, STR), point(50, DEX)));

        TABLES.put(JobType.BLACKSMITH, buildTable(50,
                point(1, DEX), point(3, STR), point(4, DEX), point(5, DEX), point(7, VIT),
                point(8, STR), point(9, DEX), point(11, LUK), point(12, DEX), point(13, VIT),
                point(16, STR), point(19, DEX), point(20, VIT), point(21, INT), point(23, STR),
                point(26, DEX), point(28, DEX), point(29, AGI), point(31, STR), point(32, VIT),
                point(34, INT), point(36, DEX), point(37, VIT), point(38, AGI), point(39, DEX),
                point(40, DEX), point(44, STR), point(46, LUK), point(47, DEX), point(49, VIT)));
    }

    private JobBonusData() {
    }

    private static Stats6[] createEmptyTable(int maxLevel) {
        Stats6[] table = new Stats6[maxLevel + 1];
        for (int level = 0; level <= maxLevel; level++) {
            table[level] = Stats6.ZERO;
        }
        return table;
    }

    private static Stats6[] buildTable(int maxLevel, int[]... milestones) {
        Stats6[] table = new Stats6[maxLevel + 1];
        int[] running = new int[6];
        table[0] = Stats6.ZERO;

        int milestoneIndex = 0;
        for (int level = 1; level <= maxLevel; level++) {
            while (milestoneIndex < milestones.length && milestones[milestoneIndex][0] == level) {
                running[milestones[milestoneIndex][1]]++;
                milestoneIndex++;
            }
            table[level] = new Stats6(running[STR], running[AGI], running[VIT], running[INT], running[DEX], running[LUK]);
        }
        return table;
    }

    private static int[] point(int level, int statIndex) {
        return new int[] { level, statIndex };
    }

    public static Stats6 getBonus(JobType job, int level) {
        if (job == null) {
            return Stats6.ZERO;
        }

        Stats6[] table = TABLES.get(job);
        if (table == null) {
            return Stats6.ZERO;
        }

        int clamped = Math.max(0, Math.min(level, table.length - 1));
        return table[clamped];
    }
}
