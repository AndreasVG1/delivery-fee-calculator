# FR-05: Fee configuration REST interface

## Summary

Add `FeeConfigurationController` for CRUD management of regional base fees and weather fee rules. Keep rule validation in `FeeConfigurationService` and use one entity-specific `JpaRepository` per rule type.

## HTTP interface

- Expose four collections under `/api/configurations/`: `base-fees`, `temperature-rules`, `wind-rules`, and `phenomenon-rules`. Each supports `GET` list, `POST` create, and `GET`, `PUT`, and `DELETE` by numeric ID.
- Use full replacement for `PUT`. JSON uses the existing entity field names and canonical enum values; responses include `id`. Return `201` with `Location` for create, `200` for reads and updates, and `204` for delete.
- Return `400` for malformed or invalid rule data, `404` for a missing ID, and `409` for duplicate city/vehicle or phenomenon/vehicle pairs and overlapping ranges. Errors use the existing `{"error":"..."}` shape.

## Implementation

- Remove the read-only `FeeConfigurationRepository` and its JPA adapter. Use the four entity-specific repositories for CRUD. Add IDs and the entity mutation support needed for CRUD; retain the service’s fee resolution behavior.
- Validate fee amounts, outcomes, bounds, and range overlap in the service. Compare only rules of the same type and vehicle, excluding the rule being updated. Equal endpoints overlap only when both touching ends are inclusive; null bounds are unbounded.
- Serialize all configuration writes with one service lock held **through transaction commit or rollback** using `TransactionTemplate`. This protects concurrent HTTP requests in the current single process, embedded H2 deployment without changing the schema or existing Docker data.
- Permit edits and deletions that leave coverage gaps; fee calculation continues to report `Uncovered range`. Document the endpoints and update FR-05 traceability after verification.

## TDD and verification

- Use the confirmed seam: MockMvc requests through the real Spring context and H2. Work in vertical red → green slices, one failing HTTP behavior test and its minimal implementation at a time.
- Cover CRUD for all four rule types; validation and duplicate errors; unbounded ranges; equal endpoints with every inclusivity combination; adjacent non-overlapping ranges; and updates that exclude their own prior range.
- Run concurrent overlapping creates and updates, then verify through `GET` that at most one succeeds and stored ranges remain non-overlapping. Verify that an edited rule changes a subsequent delivery fee response. Run the full Maven suite.

## Assumptions

- Configuration endpoints have no authentication requirement in FR-05.
- The current embedded H2 deployment has one application process; supporting multiple writers through H2 server mode would require a database-level lock.
