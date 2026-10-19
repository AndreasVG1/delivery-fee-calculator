# FR-06: Delivery fee calculation storage

## Summary

Save every new calculation made from a valid fee request, including business failures. Keep saved records read-only. For requests with a datetime, reuse an eligible successful record before calculating with current fee rules.

## Implementation

- Add a calculation repository with append and read operations only. Make `DeliveryFeeCalculation` immutable after insertion. Save successful results with their component fees and selected weather observation; save failures with an error message, no total, and the observation when one was selected.
- In `DeliveryFeeService`, check history only when `datetime` is supplied. Match city and vehicle type, require a successful record whose observation is within the inclusive 24-hour window, and prefer the nearest observation. Break ties by newest calculation, then lowest ID. Return the saved record unchanged; do not create a row for a reuse. If none matches, calculate using the nearest eligible observation and current rules, then save the result. Requests without `datetime` always make and save a new calculation.
- Keep `GET /api/delivery-fee` and add `GET /api/delivery-fee/{id}`. Add `id` to successful calculation responses and to errors from valid fee requests. Retrieval returns the recorded ID, request and calculation times, city, vehicle type, weather observation ID, component fees, total, and error message without recalculation. A missing ID returns `404`. Invalid request parameters remain `400` and create no record. Add no update or delete route.
- Document the API and the resolved history tie-break in the README and domain model. Mark FR-06 complete in the requirements after verification.

## TDD and verification

- Use the agreed **HTTP and service seams** with the real H2 database. Work in vertical cycles: one failing behavior test, minimal implementation, then the next test.
- Cover saved success and failure records, retrieval after fee rules change, reuse with unchanged ID and amounts, city and vehicle filtering, the exact 24-hour boundary, nearest observation and tie-breaks, fallback to current rules, and omitted datetime. Verify that no update or delete API exists.
- Adjust existing service tests that expect a null ID or delete an observation after it has been referenced by a saved calculation. Run focused tests, then the full Maven suite. The baseline test attempt could not lock `~/.m2` in this sandbox, so use a writable temporary Maven cache.

## Assumptions

- “Read-only” is enforced at the application boundary; direct database administrators retain SQL access.
- Reuse returns the original record’s ID and timestamps, including its original `requestedAt`.
- A stored failure is retrievable by ID but is never eligible for reuse.
