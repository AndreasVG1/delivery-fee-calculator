package andvil.feeCalculator.fee.application;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Calculates the total from fee amounts that have already been resolved and validated. */
@Component
public class FeeCalculator {

    /** Adds the regional base fee and the three weather related fee components. */
    public BigDecimal calculate(BigDecimal rbf, BigDecimal atef, BigDecimal wsef, BigDecimal wpef) {
        return rbf.add(atef).add(wsef).add(wpef);
    }
}
