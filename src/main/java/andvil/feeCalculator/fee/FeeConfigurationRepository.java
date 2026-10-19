package andvil.feeCalculator.fee;

import java.util.List;
import java.util.Optional;

/** Read-only access to the active seeded fee rules. */
public interface FeeConfigurationRepository {
    /** Finds a base fee for one request pair. */
    Optional<RegionalBaseFee> findBaseFee(City city, VehicleType vehicle);
    /** Lists the temperature rules for one vehicle. */
    List<TemperatureFeeRule> findTemperatureRules(VehicleType vehicle);
    /** Lists the wind rules for one vehicle. */
    List<WindFeeRule> findWindRules(VehicleType vehicle);
    /** Lists phenomenon rules for one vehicle. */
    List<PhenomenonFeeRule> findPhenomenonRules(VehicleType vehicle);
}
