package andvil.feeCalculator.fee;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adapts fee rule repositories to the read-only configuration interface. */
@Repository
class JpaFeeConfigurationRepository implements FeeConfigurationRepository {
    private final RegionalBaseFeeRepository baseFees;
    private final TemperatureFeeRuleRepository temperatures;
    private final WindFeeRuleRepository winds;
    private final PhenomenonFeeRuleRepository phenomena;
    JpaFeeConfigurationRepository(RegionalBaseFeeRepository baseFees,
            TemperatureFeeRuleRepository temperatures, WindFeeRuleRepository winds,
            PhenomenonFeeRuleRepository phenomena) {
        this.baseFees = baseFees; this.temperatures = temperatures; this.winds = winds; this.phenomena = phenomena;
    }
    @Override public Optional<RegionalBaseFee> findBaseFee(City city, VehicleType vehicle) {
        return baseFees.findByCityAndVehicleType(city, vehicle);
    }
    @Override public List<TemperatureFeeRule> findTemperatureRules(VehicleType vehicle) {
        return temperatures.findAllByVehicleType(vehicle);
    }
    @Override public List<WindFeeRule> findWindRules(VehicleType vehicle) { return winds.findAllByVehicleType(vehicle); }
    @Override public List<PhenomenonFeeRule> findPhenomenonRules(VehicleType vehicle) {
        return phenomena.findAllByVehicleType(vehicle);
    }
}
