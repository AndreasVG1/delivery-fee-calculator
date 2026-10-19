package andvil.feeCalculator.fee;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores wind fee rules. */
interface WindFeeRuleRepository extends JpaRepository<WindFeeRule, Long> {
    List<WindFeeRule> findAllByVehicleType(VehicleType vehicleType);
}
