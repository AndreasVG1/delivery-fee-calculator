package andvil.feeCalculator.fee.persistence;

import java.util.List;

import andvil.feeCalculator.fee.model.VehicleType;
import andvil.feeCalculator.fee.model.WindFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores wind fee rules. */
public interface WindFeeRuleRepository extends JpaRepository<WindFeeRule, Long> {
    List<WindFeeRule> findAllByVehicleType(VehicleType vehicleType);
}
