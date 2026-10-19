# FR-01: Weather observation persistence

## Summary

Add `WeatherObservation` and `WeatherRepository` using the existing H2 schema. Test behavior through the repository with a real H2 database, as agreed.

## Implementation

- Map `WeatherObservation` to `weather_observation`: generated `Long` ID; station name, WMO code, and exact phenomenon `String`; `BigDecimal` temperature and wind speed; and `OffsetDateTime` observation time. Leave the database-generated `mapped_city` column unmapped.
- Add `WeatherRepository extends JpaRepository<WeatherObservation, Long>` with a method to retrieve one WMO code’s observations in time order. Document the public Java API.
- Keep import scheduling, duplicate import handling, and fee-rule phenomenon mapping for later requirements. Keep `schema.sql` as the source of database constraints.

## TDD and verification

1. Write a failing H2 repository test for saving an observation and retrieving every field by ID, including an API phenomenon string unchanged. Add the entity and basic repository to pass it.
2. Write a failing test that retrieves two observations with the same WMO code at different times as separate historical records. Add the ordered repository query to pass it.
3. Run the repository tests and full Maven suite against the existing `schema.sql`, then update FR-01’s requirements traceability entry.

## Assumptions

- FR-01 needs one Java entity: `WeatherObservation`.
- “Do not overwrite historical records” means observations at distinct times remain separate rows. FR-02 will decide what happens when the same WMO code and time are imported again.
- The existing baseline `./mvnw -q test` passes.
