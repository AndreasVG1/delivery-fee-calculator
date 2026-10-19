package andvil.feeCalculator.fee.application;

import java.math.BigDecimal;

/** Four validated components resolved from the currently active rules. */
public record ResolvedFees(BigDecimal rbf, BigDecimal atef, BigDecimal wsef, BigDecimal wpef) { }
