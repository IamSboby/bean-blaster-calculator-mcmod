package com.beanblaster.calculation;

import java.util.ArrayList;
import java.util.List;

/** Pure Java port of the original Python Bean Blaster formulas. */
public final class BeanBlasterCalculator {
    private static final int DISPENSER_CAPACITY = 9 * 64;

    private BeanBlasterCalculator() {
    }

    public static Result calculate(int machineX, int machineZ, int targetWorldX, int targetWorldZ) {
        double targetX = (double) targetWorldX - machineX;
        double targetZ = (double) targetWorldZ - machineZ;

        boolean aboveVectorOne = targetX / 13.5 < targetZ;
        boolean aboveVectorTwo = targetX / -15.0 < targetZ;
        boolean aboveVectorThree = targetZ / 13.7 < targetX;
        boolean aboveVectorFour = targetZ / -15.2 < targetX;

        String quadrant = null;

        if (aboveVectorOne) {
            if (aboveVectorThree) {
                quadrant = "NE";
            } else if (aboveVectorTwo) {
                quadrant = "NW";
            } else {
                quadrant = "SW";
            }
        } else if (aboveVectorFour) {
            quadrant = "SE";
        } else if (!aboveVectorTwo) {
            quadrant = "SW";
        }

        if (quadrant == null) {
            return Result.failure(
                    "The target is outside the sectors handled by the original calculation.",
                    targetX,
                    targetZ
            );
        }

        double factor;
        double otherFactor;
        double xDivisor;
        double zDivisor;
        int xDispenser;
        int zDispenser;

        switch (quadrant) {
            case "NE" -> {
                factor = 13.5;
                otherFactor = 13.7;
                xDivisor = -31.0;
                zDivisor = 36.0;
                xDispenser = 2;
                zDispenser = 3;
            }
            case "NW" -> {
                factor = -15.0;
                otherFactor = 13.7;
                xDivisor = 31.0;
                zDivisor = 36.0;
                xDispenser = 1;
                zDispenser = 3;
            }
            case "SE" -> {
                factor = 13.5;
                otherFactor = -15.2;
                xDivisor = -36.0;
                zDivisor = -31.0;
                xDispenser = 2;
                zDispenser = 4;
            }
            case "SW" -> {
                factor = -15.0;
                otherFactor = -15.2;
                xDivisor = 36.0;
                zDivisor = -31.0;
                xDispenser = 1;
                zDispenser = 4;
            }
            default -> throw new IllegalStateException("Unknown quadrant: " + quadrant);
        }

        // Operation order intentionally matches the Python source.
        double denominator = factor * otherFactor;
        double numerator = factor * targetZ;
        numerator = numerator - targetX;
        denominator = denominator - 1.0;

        double crossoverX = numerator / denominator;
        double crossoverZ = crossoverX * otherFactor;
        double rawZCharges = crossoverZ / zDivisor;
        double rawXCharges = (crossoverX - targetX) / xDivisor;

        if (!Double.isFinite(crossoverX)
                || !Double.isFinite(crossoverZ)
                || !Double.isFinite(rawXCharges)
                || !Double.isFinite(rawZCharges)) {
            return Result.failure("The calculation produced an invalid numerical result.", targetX, targetZ);
        }

        // Java casts truncate toward zero, matching Python int(float).
        int xCharges = (int) rawXCharges;
        int zCharges = (int) rawZCharges;

        if (xCharges < 0 || zCharges < 0) {
            return Result.failure("The calculation requires a negative Wind Charge quantity.", targetX, targetZ);
        }

        List<String> warnings = new ArrayList<>();

        if (targetX == 0.0 && targetZ == 0.0) {
            warnings.add("The target is the saved magma block position.");
        }

        if (xCharges == 0 || zCharges == 0) {
            warnings.add("One dispenser result is zero; verify the launcher orientation.");
        }

        if (xCharges > DISPENSER_CAPACITY || zCharges > DISPENSER_CAPACITY) {
            warnings.add("A result exceeds the 576-item capacity of one vanilla dispenser.");
        }

        return new Result(
                true,
                "Calculation completed.",
                quadrant,
                targetX,
                targetZ,
                crossoverX,
                crossoverZ,
                new Load(xDispenser, xCharges),
                new Load(zDispenser, zCharges),
                List.copyOf(warnings)
        );
    }

    public record Load(int dispenser, int windCharges) {
    }

    public record Result(
            boolean success,
            String message,
            String quadrant,
            double relativeX,
            double relativeZ,
            double crossoverX,
            double crossoverZ,
            Load firstLoad,
            Load secondLoad,
            List<String> warnings
    ) {
        private static Result failure(String message, double relativeX, double relativeZ) {
            return new Result(
                    false,
                    message,
                    null,
                    relativeX,
                    relativeZ,
                    Double.NaN,
                    Double.NaN,
                    null,
                    null,
                    List.of()
            );
        }
    }
}
