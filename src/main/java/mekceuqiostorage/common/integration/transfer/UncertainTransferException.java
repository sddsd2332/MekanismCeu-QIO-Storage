package mekceuqiostorage.common.integration.transfer;

import javax.annotation.Nullable;

/** An executed native call that cannot be represented by an ordinary settled QIO amount. */
public final class UncertainTransferException extends RuntimeException {
    public final long requested;
    @Nullable public final Double before;
    @Nullable public final Double after;
    @Nullable public final Long actual;

    UncertainTransferException(long requested, Double before, Double after, Long actual, Throwable cause) {
        super("Native transfer requires reconciliation", cause);
        this.requested = requested;
        this.before = before;
        this.after = after;
        this.actual = actual;
    }
}
