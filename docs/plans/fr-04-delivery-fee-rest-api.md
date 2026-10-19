# FR-04: Delivery fee REST endpoint

## Summary

Add `DeliveryFeeController` as the HTTP interface to the existing `DeliveryFeeService`. FR-04 calculates and returns a fee; storing or reusing calculations remains part of FR-06.

## HTTP contract

- `GET /api/delivery-fee` accepts required `city` and `vehicleType` query parameters and optional `datetime`.
- Accept city and vehicle names regardless of case, including `Pärnu` with its accent. Parse `datetime` as an ISO 8601 date and time with an offset. Omitting it requests current conditions.
- Return `200` JSON with `city`, `vehicleType`, `requestedAt`, and numeric `totalFee`. Use canonical enum names in the response; `requestedAt` is `null` when omitted.
- Return `{"error":"..."}` with `400` for missing or malformed input, `404` when no qualifying weather observation exists, and `422` for a forbidden vehicle or uncovered fee rule. Preserve the existing calculation error messages.

## Implementation and TDD

- Test the confirmed seam: MockMvc requests through the real Spring context and seeded H2 rules. Add one failing HTTP behavior test, make it pass with the smallest change, then repeat for the next behavior.
- Keep parsing, response mapping, and HTTP status selection in the web layer. Delegate fee calculation to `DeliveryFeeService`; do not add calculation persistence.
- Document the endpoint in the README and mark FR-04's acceptance criteria and traceability complete after verification.

## Test plan and assumptions

- Cover an explicit datetime and known fee, omitted datetime, case-insensitive names including `Pärnu`, missing and invalid parameters, no weather (`404`), forbidden delivery (`422`), and uncovered rules (`422`). Assert HTTP responses, not controller internals.
- Run the new test and full Maven suite with a writable temporary dependency cache. The baseline currently has 31 passing tests and two `WeatherClientTest` errors because this sandbox forbids its local test server socket; report that limitation if it persists.
