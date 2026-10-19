package andvil.feeCalculator.fee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
interface WindFeeRuleRepository extends JpaRepository<WindFeeRule, Long> {
    List<WindFeeRule> findAllByVehicleType(VehicleType vehicleType);
}
