package andvil.feeCalculator.fee;

import java.util.Locale;

/** Fee-rule categories for the weather portal's free-text phenomenon values. */
public enum WeatherPhenomenon {
    SNOW, SLEET, RAIN, GLAZE, HAIL, THUNDER, OTHER;

    /** Maps known Estonian weather portal values to fee categories; unknown values are OTHER. */
    public static WeatherPhenomenon fromProviderValue(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "light snow shower", "moderate snow shower", "heavy snow shower",
                    "light snowfall", "moderate snowfall", "heavy snowfall", "snowstorm", "drifting snow" -> SNOW;
            case "light sleet", "moderate sleet" -> SLEET;
            case "light shower", "moderate shower", "heavy shower", "light rain", "moderate rain", "heavy rain" -> RAIN;
            case "glaze", "risk of glaze" -> GLAZE;
            case "hail" -> HAIL;
            case "thunder", "thunderstorm" -> THUNDER;
            default -> OTHER;
        };
    }
}
