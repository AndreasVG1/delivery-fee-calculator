package andvil.feeCalculator.fee.application;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Calculates the total from fee amounts that have already been resolved and validated. */
@Component
public class FeeCalculator {

    /**
     * Adds the regional base fee, air temperature, wind speed, and phenomenon surcharges.
     * All inputs must be non-null amounts in euros; rule validation happens before this call.
     *
     * @return the total delivery fee in euros
     */
    public BigDecimal calculate(BigDecimal rbf, BigDecimal atef, BigDecimal wsef, BigDecimal wpef) {
        return rbf.add(atef).add(wsef).add(wpef);
    }
}
