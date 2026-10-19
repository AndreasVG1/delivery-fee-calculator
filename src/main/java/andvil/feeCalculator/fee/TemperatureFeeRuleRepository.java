package andvil.feeCalculator.fee;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores temperature fee rules. */
interface TemperatureFeeRuleRepository extends JpaRepository<TemperatureFeeRule, Long> {
    List<TemperatureFeeRule> findAllByVehicleType(VehicleType vehicleType);
}
