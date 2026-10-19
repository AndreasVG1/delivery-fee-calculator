package andvil.feeCalculator.fee;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores weather phenomenon fee rules. */
interface PhenomenonFeeRuleRepository extends JpaRepository<PhenomenonFeeRule, Long> {
    List<PhenomenonFeeRule> findAllByVehicleType(VehicleType vehicleType);
}
