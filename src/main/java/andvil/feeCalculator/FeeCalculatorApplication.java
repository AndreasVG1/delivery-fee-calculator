package andvil.feeCalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FeeCalculatorApplication {

	/** Starts the Spring Boot application and its scheduled weather import. */
	public static void main(String[] args) {
		SpringApplication.run(FeeCalculatorApplication.class, args);
	}

}
