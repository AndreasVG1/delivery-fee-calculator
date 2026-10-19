# Delivery Fee Domain Model

This document describes the domain represented by [`class_diagram_v2.svg`](./class_diagram_v2.svg), with the calculation behavior and constraints from [`REQUIREMENTS.md`](./REQUIREMENTS.md). The diagram is the source for the entities and value sets below; the requirements are the source for the fee formula and default business rules.

## Purpose

The domain determines a delivery fee from a requested city, vehicle type, and the weather observation applicable to the requested time. A fee combines a regional base fee with weather-related surcharges. If the weather makes the selected vehicle forbidden, the request returns an error instead of a fee.

## Core concepts

### Weather observation

A **WeatherObservation** is a recorded set of weather conditions at a named station and time. It contains a station name, WMO code, air temperature, wind speed, weather phenomenon, and observation time. Temperature and wind speed are decimal measurements; `observed_at` identifies when the conditions were observed.

Observations are retained as historical records. The station used for a city is fixed as follows:

| City | Weather station |
|---|---|
| `TALLINN` | `Tallinn-Harku` |
| `TARTU` | `Tartu-Tõravere` |
| `PÄRNU` | `Pärnu` |

For a requested datetime, the calculator selects the observation from that city's station with the nearest `observed_at` time. The absolute difference must be no more than 24 hours. If no observation is within that window, calculation fails with `No weather data available for the requested time`. Fee rules active at calculation time apply, including for requests with a historical datetime.

### Regional base fee

A **RegionalBaseFee** is the base charge for one city and vehicle type. The fee is selected using both dimensions and is added to any applicable weather fees. Only one rule may exist for a given city and vehicle type; creating a duplicate is rejected.

### Weather fee rules

Weather fee rules associate a vehicle type with a condition and a fee outcome:

| Rule | Condition represented | Outcome data |
|---|---|---|
| **TemperatureFeeRule** | A lower and upper temperature threshold, with separate inclusivity flags | Fee amount |
| **WindFeeRule** | A lower and upper wind-speed threshold, with separate inclusivity flags | `SURCHARGE` or `FORBIDDEN`, and fee amount |
| **PhenomenonFeeRule** | A weather phenomenon | `SURCHARGE` or `FORBIDDEN`, and fee amount |

Thresholds for temperature and wind rules are expressed as decimals and are nullable. A null lower threshold means that the range has no lower bound; a null upper threshold means that it has no upper bound. At least one of the two thresholds must be present. For example, “air temperature is less than -10” has `lower_threshold = NULL`, `upper_threshold = -10.0`, and an exclusive upper bound. The inclusivity flags determine whether a value equal to a present threshold belongs to the range.

Threshold ranges for the same rule type and vehicle type must not overlap. Creating a rule must be rejected if its range overlaps an existing rule's range. During fee calculation, if a measured value falls into no configured range, calculation fails with the error message `Uncovered range` instead of silently applying no fee.

## Value sets

| Type | Values |
|---|---|
| **City** | `TALLINN`, `TARTU`, `PÄRNU` |
| **VehicleType** | `CAR`, `SCOOTER`, `BIKE` |
| **WeatherPhenomenon** | `SNOW`, `SLEET`, `RAIN`, `GLAZE`, `HAIL`, `THUNDER`, `OTHER` |
| **RuleOutcome** | `SURCHARGE`, `FORBIDDEN` |

## Relationships

- A regional base fee is associated with one city and one vehicle type.
- Temperature and wind fee rules are associated with a vehicle type.
- A phenomenon fee rule is associated with a vehicle type and a weather phenomenon.
- A weather observation is recorded independently of fee rules. The diagram does not draw an association between observations and rules; calculation uses an observation as weather input and selects rules for the requested vehicle type.
- A delivery fee calculation records the request, the selected weather observation, and the calculation result or error.

### Delivery fee calculation

A **DeliveryFeeCalculation** records the outcome of a calculation so that the exact result can be displayed later even after active fee rules change. It contains an ID, request datetime, calculation datetime, city, vehicle type, a reference to the selected weather observation when one was available, the regional base fee (`rbf`), temperature fee (`atef`), wind fee (`wsef`), phenomenon fee (`wpef`), and total fee. `error_message` is optional and records why the calculation failed; a failed calculation has no total fee.

The component amounts and total are retained as calculated, rather than recalculated from current rules when displaying a stored calculation.

## Fee calculation

For an allowed vehicle, the requirements define the fee as:

```text
Delivery fee = Regional base fee
             + temperature extra fee
             + wind extra fee
             + phenomenon extra fee
```

The weather rules apply as follows:

- **Temperature:** surcharge rules apply to scooters and bikes; cars receive no temperature surcharge.
- **Wind:** surcharge or restriction rules apply to bikes; cars and scooters receive no wind surcharge.
- **Phenomenon:** surcharge or restriction rules apply to scooters and bikes; cars receive no phenomenon surcharge.
- **Restriction:** if any applicable rule yields `FORBIDDEN`, no fee is returned. The requirements specify the error message `Usage of selected vehicle type is forbidden`.

For a request with a datetime, calculation history is checked first. A successful `DeliveryFeeCalculation` is reusable when its city and vehicle type match the request and its referenced observation's `observed_at` is within 24 hours of the requested datetime. The closest matching observation is preferred. If no stored calculation matches, the calculator uses the nearest observation from the mapped station within 24 hours and calculates using the currently active rules. If neither a calculation nor an observation is available within that window, the request fails with `No weather data available for the requested time`.

The default thresholds and amounts are detailed in the business rules in [`REQUIREMENTS.md`](./REQUIREMENTS.md). The class diagram models these as configurable rule records rather than embedding the threshold values in the entity definitions.

## Model notes and open points

- `WeatherObservation.phenomenon` remains a `String` to preserve the exact value returned by the weather API. A separate mapping translates it for comparison with the `WeatherPhenomenon` values used by fee rules.
- If multiple successful calculations have equally near referenced observation times, or multiple weather observations are equally near, the selection tie-break is not defined yet.
