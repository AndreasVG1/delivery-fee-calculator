package andvil.feeCalculator.fee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
interface TemperatureFeeRuleRepository extends JpaRepository<TemperatureFeeRule, Long> {
    List<TemperatureFeeRule> findAllByVehicleType(VehicleType vehicleType);
}
