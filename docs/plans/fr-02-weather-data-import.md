# FR-02: Weather data import

## Summary

Add `WeatherClient`, `WeatherImportService`, and `ImportScheduler` to import XML observations for Tallinn-Harku, Tartu-Tõravere, and Pärnu. The [weather portal’s field guide](https://www.ilmateenistus.ee/teenused/ilmainfo/eesti-vaatlusandmed-xml/) confirms the requested station fields.

## Implementation

- `WeatherClient.fetchLatestXml()` makes a timed HTTP GET to the portal and returns the XML. Make the endpoint configurable for testing.
- `WeatherImportService.importLatest()` parses the feed’s root Unix timestamp and station fields, preserves `phenomenon` exactly, and stores only the three mapped stations through `WeatherRepository`. Return the number of new observations.
- Skip a selected station with an empty or invalid required field and report it. Keep an existing row when its WMO code and observation time recur. A failed request or malformed feed stores nothing.
- `ImportScheduler` calls the import method using Spring cron scheduling. Keep the default `0 15 * * * *` and the overridable `weather.import.cron` setting there; changes take effect on restart. Resolve the schedule in `Europe/Tallinn`. Document the setting and credit the weather portal as the data source.

## TDD and verification

At the agreed **client, import, and scheduler seams**, work one failing test and minimal implementation at a time. Test HTTP retrieval and errors with a local stub server; test XML mapping, station filtering, incomplete data, and repeated imports through the import method with H2; then test the scheduler trigger and default and overridden cron registration. Run the full Maven suite and update FR-02 traceability.

## Assumptions

No import runs at startup, and FR-02 adds no REST endpoint for changing the schedule. The current baseline test command stopped before tests because the sandbox could not open Maven’s lock file under `~/.m2`; verification will need a writable Maven cache or suitable permissions.
