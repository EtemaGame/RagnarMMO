package com.etema.ragnarmmo.player.progression;

public final class TableLevelCurve implements LevelCurve {
    private final int[] expTable;
    private final LevelCurve overflowCurve;

    public TableLevelCurve(int[] expTable, LevelCurve overflowCurve) {
        this.expTable = expTable.clone();
        this.overflowCurve = overflowCurve;
    }

    @Override
    public int expToNext(int level) {
        int index = Math.max(1, level) - 1;
        if (index < expTable.length) {
            return Math.max(1, expTable[index]);
        }
        return overflowCurve.expToNext(level);
    }
}
