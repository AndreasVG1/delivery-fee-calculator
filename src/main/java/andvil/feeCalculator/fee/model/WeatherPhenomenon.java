package andvil.feeCalculator.fee.model;

import java.util.Locale;

/** Fee-rule categories for the weather portal's free-text phenomenon values. */
public enum WeatherPhenomenon {
    SNOW, SLEET, RAIN, GLAZE, HAIL, THUNDER, OTHER;

    /**
     * Maps known weather portal descriptions, ignoring case and surrounding whitespace, to a
     * fee-rule category. Null and unrecognized descriptions map to {@link #OTHER}.
     */
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
