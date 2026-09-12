package mekceuqiostorage.common.integration.transfer;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/** Executes once, observes before returning, and never assumes an attempted compensation worked. */
public final class NativeTransferAccounting {
    private NativeTransferAccounting() { }

    public static long observed(long requested, boolean insertion, DoubleSupplier balance,
          Runnable operation, DoubleConsumer compensation) {
        Double before = read(balance);
        if (before == null) return 0;
        Throwable failure = null;
        try {
            operation.run();
        } catch (LinkageError | RuntimeException thrown) {
            failure = thrown;
        }
        return settle(requested, insertion, before, balance, compensation, failure);
    }

    /** Normal native acknowledgements retain their API semantics, including routed resources. */
    public static long reported(long requested, boolean insertion, DoubleSupplier balance,
          LongSupplier operation, DoubleConsumer compensation) {
        Double before = read(balance);
        try {
            long moved = operation.getAsLong();
            if (moved >= 0 && moved <= requested) return moved;
            return settle(requested, insertion, before, balance, compensation,
                  new IllegalStateException("Native acknowledgement outside the request"));
        } catch (UncertainTransferException failure) {
            throw failure;
        } catch (LinkageError | RuntimeException failure) {
            return settle(requested, insertion, before, balance, compensation, failure);
        }
    }

    /** A successful emission is charged even when consumed immediately by the native receiver. */
    public static long delivered(long requested, Runnable operation) {
        try {
            operation.run();
            return requested;
        } catch (LinkageError | RuntimeException failure) {
            throw new UncertainTransferException(requested, null, null, null, failure);
        }
    }

    private static long settle(long requested, boolean insertion, Double before,
          DoubleSupplier balance, DoubleConsumer compensation, Throwable failure) {
        Double after = read(balance);
        // A transient read failure can be retried synchronously without repeating the mutation.
        if (after == null) after = read(balance);
        Long actual = movement(before, after, insertion);
        if (actual != null && actual >= 0 && actual <= requested) return actual;
        if (before != null && after != null && compensation != null) {
            double delta = insertion ? after - before : before - after;
            long bounded = failure == null ? Math.max(0, Math.min(requested, (long) Math.floor(delta))) : 0;
            double correction = (bounded - delta) * (insertion ? 1 : -1);
            try {
                compensation.accept(correction);
            } catch (LinkageError | RuntimeException thrown) {
                failure = thrown;
            }
            after = read(balance);
            if (after == null) after = read(balance);
            actual = movement(before, after, insertion);
            if (actual != null && actual >= 0 && actual <= requested) return actual;
        }
        throw new UncertainTransferException(requested, before, after, actual, failure);
    }

    private static Double read(DoubleSupplier balance) {
        if (balance == null) return null;
        try {
            double value = balance.getAsDouble();
            return Double.isFinite(value) && Math.abs(value) <= 9_007_199_254_740_991D ? value : null;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static Long movement(Double before, Double after, boolean insertion) {
        if (before == null || after == null) return null;
        double delta = insertion ? after - before : before - after;
        double integral = Math.rint(delta);
        // Whole-unit native double stores can round a subtraction by a few ulps.
        return Math.abs(delta - integral) <= 1E-8 ? (long) integral : null;
    }
}
