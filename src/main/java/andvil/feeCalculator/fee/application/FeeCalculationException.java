package andvil.feeCalculator.fee.application;

/** A business rule prevents a delivery fee from being calculated. */
public class FeeCalculationException extends RuntimeException {
    /** Creates a fee calculation failure with its user-facing reason. */
    public FeeCalculationException(String message) { super(message); }
}
