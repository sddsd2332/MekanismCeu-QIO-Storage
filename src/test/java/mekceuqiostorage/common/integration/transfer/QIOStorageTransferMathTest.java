package mekceuqiostorage.common.integration.transfer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QIOStorageTransferMathTest {

    @Test
    void limitsRequestsWithoutOverflow() {
        assertEquals(0, QIOStorageTransferMath.limit(-1, 10));
        assertEquals(0, QIOStorageTransferMath.limit(10, 0));
        assertEquals(7, QIOStorageTransferMath.limit(10, 7));
        assertEquals(Long.MAX_VALUE, QIOStorageTransferMath.limit(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, QIOStorageTransferMath.intLimit(Long.MAX_VALUE));
    }

    @Test
    void calculatesOnlyValidObservedDeltas() {
        assertEquals(25, QIOStorageTransferMath.increase(100, 125));
        assertEquals(0, QIOStorageTransferMath.increase(125, 100));
        assertEquals(25, QIOStorageTransferMath.decrease(125, 100));
        assertEquals(0, QIOStorageTransferMath.decrease(100, 125));
        assertEquals(0, QIOStorageTransferMath.decrease(100, -1));
    }

    @Test
    void transferResultsCannotExceedTheRequest() {
        assertEquals(0, QIOStorageTransferMath.result(-1, 100));
        assertEquals(25, QIOStorageTransferMath.result(25, 100));
        assertEquals(100, QIOStorageTransferMath.result(125, 100));
    }

    @Test
    void convertsFloatingApisToWholeUnitsSafely() {
        assertEquals(0, QIOStorageTransferMath.wholeUnits(Double.NaN));
        assertEquals(0, QIOStorageTransferMath.wholeUnits(Double.NEGATIVE_INFINITY));
        assertEquals(12, QIOStorageTransferMath.wholeUnits(12.999));
        assertEquals(Long.MAX_VALUE, QIOStorageTransferMath.wholeUnits(Double.POSITIVE_INFINITY));
    }
}
