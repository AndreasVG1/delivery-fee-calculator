package andvil.feeCalculator.fee.application;

import java.math.BigDecimal;

/**
 * Four validated euro amounts resolved from the currently active rules.
 *
 * @param rbf regional base fee
 * @param atef air temperature extra fee
 * @param wsef wind speed extra fee
 * @param wpef weather phenomenon extra fee
 */
public record ResolvedFees(BigDecimal rbf, BigDecimal atef, BigDecimal wsef, BigDecimal wpef) { }
