# FR-03 delivery fee calculation

## Summary

Implement fee calculation from the seeded database rules and stored weather observations. `DeliveryFeeService` returns a `DeliveryFeeCalculation`; saving results and reusing historical calculations remain in FR-06. No REST controller or rule CRUD is included.

## Implementation

- Add typed `City` and `VehicleType` values and map `RegionalBaseFee`, `TemperatureFeeRule`, `WindFeeRule`, `PhenomenonFeeRule`, and `DeliveryFeeCalculation` to the existing tables. Returned calculations have no ID until persistence is added.
- Give `FeeConfigurationRepository` read-only database queries. `FeeConfigurationService` selects the applicable rules, checks threshold coverage and forbidden outcomes, and supplies four resolved amounts. Missing coverage returns the specified `Uncovered range` error.
- Expose `DeliveryFeeService.calculate(City, VehicleType, OffsetDateTime requestedAt)`. A null time means now. Select the nearest observation from the city's mapped station within an inclusive 24-hour window; break equal-distance ties by earlier observation, then ID. Return an error result when none qualifies.
- Map the weather provider's documented phenomenon values explicitly to the seeded categories; unfamiliar values map to `OTHER`. Preserve the original observation string. The mapping will include the provider's documented snow, sleet, rain, shower, glaze, and hail values, plus its thunder values. [Estonian Environment Agency observation values](https://www.ilmateenistus.ee/teenused/ilmainfo/eesti-vaatlusandmed-xml/).
- Give `FeeCalculator` only the four validated `BigDecimal` amounts. It sums them; `DeliveryFeeService` builds the result. Failed results have an error message, no total, and no component amounts; they retain the selected observation when one was found.

## TDD and verification

- Use the confirmed seams: focused tests through `FeeCalculator`, and seeded H2 integration tests through `DeliveryFeeService`. Work one failing test and minimal implementation at a time.
- Cover city and vehicle base fees; temperature boundaries -10°C and 0°C; wind boundaries 10 and 20 m/s and the bike restriction; phenomenon surcharges and restrictions; combined totals; missing rule coverage; nearest observation, the exact 24-hour limit, and no observation.
- Run the full Maven suite after each completed slice. The baseline run could not start because Maven could not create a lock under `~/.m2`; use a writable Maven cache during implementation and report the final test result.

## Assumptions

- Invalid city or vehicle text is handled when FR-04 adds REST input parsing; the FR-03 service accepts typed values.
- Calculation results are returned without database writes in this phase. Historical lookup and reuse remain in FR-06.
