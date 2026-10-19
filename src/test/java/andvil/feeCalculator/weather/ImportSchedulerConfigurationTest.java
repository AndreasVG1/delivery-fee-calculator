package andvil.feeCalculator.weather;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;

@SpringBootTest(properties = "weather.import.cron=0 0 8 * * *")
class ImportSchedulerConfigurationTest {

    @Autowired
    private ImportScheduler importScheduler;

    @Test
    void applicationPropertyChangesTheRegisteredImportSchedule() {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        importScheduler.configureTasks(registrar);

        Instant now = Instant.parse("2025-01-15T07:16:00Z");
        Instant nextImport = registrar.getTriggerTaskList().getFirst().getTrigger()
                .nextExecution(new SimpleTriggerContext(now, now, now));

        assertThat(nextImport).isEqualTo(Instant.parse("2025-01-16T06:00:00Z"));
    }
}
