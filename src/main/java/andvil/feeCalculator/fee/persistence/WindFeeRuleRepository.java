package andvil.feeCalculator.fee.persistence;

import java.util.List;

import andvil.feeCalculator.fee.model.VehicleType;
import andvil.feeCalculator.fee.model.WindFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores wind fee rules. */
public interface WindFeeRuleRepository extends JpaRepository<WindFeeRule, Long> {
    /** Returns all wind-speed ranges configured for the vehicle; the result may be empty. */
    List<WindFeeRule> findAllByVehicleType(VehicleType vehicleType);
}
