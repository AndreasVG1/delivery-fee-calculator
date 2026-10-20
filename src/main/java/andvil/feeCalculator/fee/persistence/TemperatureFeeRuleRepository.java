package andvil.feeCalculator.fee.persistence;

import java.util.List;

import andvil.feeCalculator.fee.model.TemperatureFeeRule;
import andvil.feeCalculator.fee.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores temperature fee rules. */
public interface TemperatureFeeRuleRepository extends JpaRepository<TemperatureFeeRule, Long> {
    /** Returns all temperature ranges configured for the vehicle; the result may be empty. */
    List<TemperatureFeeRule> findAllByVehicleType(VehicleType vehicleType);
}
