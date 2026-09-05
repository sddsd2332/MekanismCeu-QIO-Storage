package mekceuqiostorage.common.integration.transfer;

/** Overflow-safe integer helpers used by every optional capability adapter. */
public final class QIOStorageTransferMath {

    private QIOStorageTransferMath() {
    }

    /** Returns zero for non-positive input and never returns a value above {@code maximum}. */
    public static long limit(long amount, long maximum) {
        if (amount <= 0 || maximum <= 0) {
            return 0;
        }
        return Math.min(amount, maximum);
    }

    /** Returns a non-negative value without allowing a signed value to wrap. */
    public static long nonNegative(long amount) {
        return Math.max(0L, amount);
    }

    /** Returns an int value in the native API range without wrapping. */
    public static int nonNegativeInt(int amount) {
        return Math.max(0, amount);
    }

    /** Converts a QIO request to an API-sized int without wrapping. */
    public static int intLimit(long amount) {
        long bounded = limit(amount, Integer.MAX_VALUE);
        return (int) bounded;
    }

    /** Positive increase observed after an insert operation. */
    public static long increase(long before, long after) {
        if (before < 0 || after <= before) {
            return 0;
        }
        long delta = after - before;
        // Both operands are non-negative and after is greater, so a negative result can only
        // indicate arithmetic overflow in a hostile implementation.
        return delta < 0 ? Long.MAX_VALUE : delta;
    }

    /** Positive decrease observed after an extract operation. */
    public static long decrease(long before, long after) {
        if (before < 0 || after < 0 || after >= before) {
            return 0;
        }
        long delta = before - after;
        return delta < 0 ? Long.MAX_VALUE : delta;
    }

    /** Clamps an externally reported amount to a valid QIO transfer result. */
    public static long result(long amount, long requested) {
        return limit(amount, limit(requested, Long.MAX_VALUE));
    }

    /** Floors a non-negative floating API value to whole QIO units without wrapping. */
    public static long wholeUnits(double amount) {
        if (!(amount > 0)) {
            return 0;
        }
        if (amount >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) Math.floor(amount);
    }

    /** Floors the positive whole-unit delta between two floating API values. */
    public static long wholeIncrease(double before, double after) {
        if (!isFinite(before) || !isFinite(after)) {
            return 0;
        }
        long beforeWhole = wholeUnits(before);
        long afterWhole = wholeUnits(after);
        return increase(beforeWhole, afterWhole);
    }

    /** Floors the positive whole-unit decrease between two floating API values. */
    public static long wholeDecrease(double before, double after) {
        if (!isFinite(before) || !isFinite(after)) {
            return 0;
        }
        long beforeWhole = wholeUnits(before);
        long afterWhole = wholeUnits(after);
        return decrease(beforeWhole, afterWhole);
    }

    /** Returns whether a floating value is finite and strictly positive. */
    public static boolean isFinitePositive(double amount) {
        return amount > 0 && !Double.isNaN(amount) && !Double.isInfinite(amount);
    }

    /** Returns whether a floating value is finite (including zero and negative values). */
    public static boolean isFinite(double amount) {
        return !Double.isNaN(amount) && !Double.isInfinite(amount);
    }
}
