package andvil.feeCalculator.fee.model;

/** Supported delivery cities and their weather stations. */
public enum City {
    TALLINN("Tallinn-Harku"), TARTU("Tartu-Tõravere"), PÄRNU("Pärnu");

    private final String stationName;

    City(String stationName) { this.stationName = stationName; }

    /** Returns the weather station mapped to this city. */
    public String getStationName() { return stationName; }
}
