package andvil.feeCalculator.weather;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Retrieves the latest observation XML from the weather portal. */
@Component
public class WeatherClient {

    private final URI endpoint;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    /** Creates a client for the configured portal endpoint and request timeouts. */
    public WeatherClient(
            @Value("${weather.import.url:https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php}") URI endpoint,
            @Value("${weather.import.connect-timeout:5s}") Duration connectTimeout,
            @Value("${weather.import.request-timeout:15s}") Duration requestTimeout) {
        this.endpoint = endpoint;
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }

    /**
     * Fetches the latest XML document.
     *
     * @return the portal response body
     * @throws WeatherClientException when the request fails or the portal returns a non-success status
     */
    public String fetchLatestXml() {
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(requestTimeout).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new WeatherClientException("Weather portal returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new WeatherClientException("Could not retrieve weather observations", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WeatherClientException("Weather observation request was interrupted", exception);
        }
    }
}
