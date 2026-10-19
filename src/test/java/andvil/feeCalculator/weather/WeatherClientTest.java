package andvil.feeCalculator.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import andvil.feeCalculator.weather.importing.WeatherClient;
import andvil.feeCalculator.weather.importing.WeatherClientException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeatherClientTest {

    private HttpServer server;
    private ExecutorService executor;
    private String responseBody;
    private int responseCode;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/observations", exchange -> {
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseCode, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.start();
        responseBody = "<observations/>";
        responseCode = 200;
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void fetchLatestXmlReturnsThePortalResponseBody() {
        responseBody = "<observations timestamp=\"1736933400\"/>";
        WeatherClient client = new WeatherClient(endpoint(), Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThat(client.fetchLatestXml()).isEqualTo(responseBody);
    }

    @Test
    void fetchLatestXmlRejectsUnsuccessfulHttpResponses() {
        responseCode = 503;
        WeatherClient client = new WeatherClient(endpoint(), Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThatThrownBy(client::fetchLatestXml)
                .isInstanceOf(WeatherClientException.class)
                .hasMessageContaining("503");
    }

    private URI endpoint() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/observations");
    }
}
