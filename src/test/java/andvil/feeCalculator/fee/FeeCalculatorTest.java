package andvil.feeCalculator.fee;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator();

    @Test
    void addsTheFourResolvedFeeComponents() {
        assertThat(calculator.calculate(new BigDecimal("3.50"), new BigDecimal("1.00"),
                new BigDecimal("0.50"), new BigDecimal("1.00")))
                .isEqualByComparingTo("6.00");
    }
}
