package andvil.feeCalculator.weather.importing;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import andvil.feeCalculator.weather.persistence.WeatherRepository;
import andvil.feeCalculator.weather.model.WeatherObservation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** Imports weather observations for the three cities used by fee calculations. */
@Service
public class WeatherImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherImportService.class);
    private static final Set<String> MAPPED_STATIONS = Set.of("Tallinn-Harku", "Tartu-Tõravere", "Pärnu");

    private final WeatherClient weatherClient;
    private final WeatherRepository weatherRepository;

    /** Creates an importer using the weather portal client and observation repository. */
    public WeatherImportService(WeatherClient weatherClient, WeatherRepository weatherRepository) {
        this.weatherClient = weatherClient;
        this.weatherRepository = weatherRepository;
    }

    /**
     * Fetches and imports the latest observations for Tallinn-Harku, Tartu-Tõravere, and Pärnu.
     * Existing station and timestamp pairs are skipped, as are mapped stations with missing or
     * invalid required values. An invalid feed fails the import without storing new observations.
     *
     * @return the number of observations newly stored
     * @throws WeatherClientException if the weather portal request fails
     * @throws WeatherImportException if the feed is invalid
     */
    @Transactional
    public int importLatest() {
        List<WeatherObservation> parsed = parseFeed(weatherClient.fetchLatestXml());
        int inserted = 0;
        for (WeatherObservation observation : parsed) {
            if (!weatherRepository.existsByWmoCodeAndObservedAt(observation.getWmoCode(), observation.getObservedAt())) {
                weatherRepository.save(observation);
                inserted++;
            }
        }
        weatherRepository.flush();
        return inserted;
    }

    private List<WeatherObservation> parseFeed(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                /** Rejects malformed XML immediately so the import fails with its parsing error. */
                @Override
                public void fatalError(SAXParseException exception) throws SAXParseException {
                    throw exception;
                }
            });
            Element root = builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            if (root == null || !"observations".equals(root.getTagName()) || !root.hasAttribute("timestamp")) {
                throw new WeatherImportException("Weather feed is missing its observations root or Unix timestamp");
            }
            OffsetDateTime observedAt = OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(Long.parseLong(root.getAttribute("timestamp"))), ZoneOffset.UTC);
            NodeList stations = root.getElementsByTagName("station");
            List<WeatherObservation> observations = new ArrayList<>();
            for (int index = 0; index < stations.getLength(); index++) {
                Element station = (Element) stations.item(index);
                String stationName = value(station, "name").trim();
                if (!MAPPED_STATIONS.contains(stationName)) {
                    continue;
                }
                try {
                    String wmoCode = required(value(station, "wmocode")).trim();
                    BigDecimal airTemperature = new BigDecimal(required(value(station, "airtemperature")).trim());
                    BigDecimal windSpeed = new BigDecimal(required(value(station, "windspeed")).trim());
                    String phenomenon = required(value(station, "phenomenon"));
                    observations.add(new WeatherObservation(stationName, wmoCode,
                            airTemperature, windSpeed, phenomenon, observedAt));
                } catch (IllegalArgumentException exception) {
                    LOGGER.warn("Skipping weather station {}: required observation data is empty or invalid", stationName);
                }
            }
            return observations;
        } catch (WeatherImportException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WeatherImportException("Could not parse weather observation feed", exception);
        }
    }

    private String value(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        Node node = nodes.item(0);
        return node.getTextContent();
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("required value is empty");
        }
        return value;
    }
}
