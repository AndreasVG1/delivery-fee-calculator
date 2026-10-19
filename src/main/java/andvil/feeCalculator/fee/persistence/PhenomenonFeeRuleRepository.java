package andvil.feeCalculator.fee.persistence;

import java.util.List;

import andvil.feeCalculator.fee.model.PhenomenonFeeRule;
import andvil.feeCalculator.fee.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores weather phenomenon fee rules. */
public interface PhenomenonFeeRuleRepository extends JpaRepository<PhenomenonFeeRule, Long> {
    List<PhenomenonFeeRule> findAllByVehicleType(VehicleType vehicleType);
}
