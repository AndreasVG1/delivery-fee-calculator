package andvil.feeCalculator.fee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
interface RegionalBaseFeeRepository extends JpaRepository<RegionalBaseFee, Long> {
    Optional<RegionalBaseFee> findByCityAndVehicleType(City city, VehicleType vehicleType);
}
