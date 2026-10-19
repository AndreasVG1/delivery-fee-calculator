-- Default fees from docs/REQUIREMENTS.md, sections 4.2-4.6.
-- Zero-fee rules cover conditions for vehicles without a surcharge so that
-- an uncovered range is reserved for genuinely missing rule coverage.

INSERT INTO regional_base_fee (city, vehicle_type, fee_amount) VALUES
    ('TALLINN', 'CAR', 4.00),
    ('TALLINN', 'SCOOTER', 3.50),
    ('TALLINN', 'BIKE', 3.00),
    ('TARTU', 'CAR', 3.50),
    ('TARTU', 'SCOOTER', 3.00),
    ('TARTU', 'BIKE', 2.50),
    ('PÄRNU', 'CAR', 3.00),
    ('PÄRNU', 'SCOOTER', 2.50),
    ('PÄRNU', 'BIKE', 2.00);

INSERT INTO temperature_fee_rule
    (vehicle_type, lower_threshold, upper_threshold, lower_inclusive, upper_inclusive, fee_amount)
VALUES
    ('CAR', NULL, 0, FALSE, FALSE, 0.00),
    ('CAR', 0, NULL, TRUE, FALSE, 0.00),
    ('SCOOTER', NULL, -10, FALSE, FALSE, 1.00),
    ('SCOOTER', -10, 0, TRUE, FALSE, 0.50),
    ('SCOOTER', 0, NULL, TRUE, FALSE, 0.00),
    ('BIKE', NULL, -10, FALSE, FALSE, 1.00),
    ('BIKE', -10, 0, TRUE, FALSE, 0.50),
    ('BIKE', 0, NULL, TRUE, FALSE, 0.00);

INSERT INTO wind_fee_rule
    (vehicle_type, lower_threshold, upper_threshold, lower_inclusive, upper_inclusive, outcome, fee_amount)
VALUES
    ('CAR', NULL, 10, FALSE, FALSE, 'SURCHARGE', 0.00),
    ('CAR', 10, NULL, TRUE, FALSE, 'SURCHARGE', 0.00),
    ('SCOOTER', NULL, 10, FALSE, FALSE, 'SURCHARGE', 0.00),
    ('SCOOTER', 10, NULL, TRUE, FALSE, 'SURCHARGE', 0.00),
    ('BIKE', NULL, 10, FALSE, FALSE, 'SURCHARGE', 0.00),
    ('BIKE', 10, 20, TRUE, TRUE, 'SURCHARGE', 0.50),
    ('BIKE', 20, NULL, FALSE, FALSE, 'FORBIDDEN', 0.00);

INSERT INTO phenomenon_fee_rule (vehicle_type, phenomenon, outcome, fee_amount) VALUES
    ('CAR', 'SNOW', 'SURCHARGE', 0.00),
    ('CAR', 'SLEET', 'SURCHARGE', 0.00),
    ('CAR', 'RAIN', 'SURCHARGE', 0.00),
    ('CAR', 'GLAZE', 'SURCHARGE', 0.00),
    ('CAR', 'HAIL', 'SURCHARGE', 0.00),
    ('CAR', 'THUNDER', 'SURCHARGE', 0.00),
    ('CAR', 'OTHER', 'SURCHARGE', 0.00),
    ('SCOOTER', 'SNOW', 'SURCHARGE', 1.00),
    ('SCOOTER', 'SLEET', 'SURCHARGE', 1.00),
    ('SCOOTER', 'RAIN', 'SURCHARGE', 0.50),
    ('SCOOTER', 'GLAZE', 'FORBIDDEN', 0.00),
    ('SCOOTER', 'HAIL', 'FORBIDDEN', 0.00),
    ('SCOOTER', 'THUNDER', 'FORBIDDEN', 0.00),
    ('SCOOTER', 'OTHER', 'SURCHARGE', 0.00),
    ('BIKE', 'SNOW', 'SURCHARGE', 1.00),
    ('BIKE', 'SLEET', 'SURCHARGE', 1.00),
    ('BIKE', 'RAIN', 'SURCHARGE', 0.50),
    ('BIKE', 'GLAZE', 'FORBIDDEN', 0.00),
    ('BIKE', 'HAIL', 'FORBIDDEN', 0.00),
    ('BIKE', 'THUNDER', 'FORBIDDEN', 0.00),
    ('BIKE', 'OTHER', 'SURCHARGE', 0.00);
