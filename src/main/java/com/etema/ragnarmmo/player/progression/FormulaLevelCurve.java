package com.etema.ragnarmmo.player.progression;

public final class FormulaLevelCurve implements LevelCurve {
    private final int minimum;
    private final double exponentialBase;
    private final double exponentialGrowth;
    private final double polynomialPower;
    private final double polynomialScale;

    public FormulaLevelCurve(int minimum, double exponentialBase, double exponentialGrowth,
            double polynomialPower, double polynomialScale) {
        this.minimum = minimum;
        this.exponentialBase = exponentialBase;
        this.exponentialGrowth = exponentialGrowth;
        this.polynomialPower = polynomialPower;
        this.polynomialScale = polynomialScale;
    }

    @Override
    public int expToNext(int level) {
        int sanitizedLevel = Math.max(1, level);
        double expRequired = exponentialBase * Math.pow(exponentialGrowth, sanitizedLevel - 1)
                + Math.pow(sanitizedLevel, polynomialPower) * polynomialScale;
        return Math.max(minimum, (int) Math.round(expRequired));
    }
}
