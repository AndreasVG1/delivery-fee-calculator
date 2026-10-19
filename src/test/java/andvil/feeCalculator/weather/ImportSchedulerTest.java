package andvil.feeCalculator.weather;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

class ImportSchedulerTest {

    private static final Instant WEDNESDAY_AT_09_16_TALLINN = Instant.parse("2025-01-15T07:16:00Z");

    @Test
    void defaultCronRegistersHourlyImportAtQuarterPastInTallinnTime() {
        AtomicInteger imports = new AtomicInteger();
        ImportScheduler scheduler = new ImportScheduler(countingService(imports), ImportScheduler.DEFAULT_CRON);
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        scheduler.configureTasks(registrar);

        TriggerTask task = registrar.getTriggerTaskList().getFirst();
        assertThat(task.getTrigger()).isInstanceOf(CronTrigger.class);
        assertThat(task.getTrigger().nextExecution(new SimpleTriggerContext(
                WEDNESDAY_AT_09_16_TALLINN, WEDNESDAY_AT_09_16_TALLINN, WEDNESDAY_AT_09_16_TALLINN)))
                .isEqualTo(Instant.parse("2025-01-15T08:15:00Z"));
        task.getRunnable().run();
        assertThat(imports).hasValue(1);
    }

    @Test
    void overriddenCronChangesTheRegisteredImportTime() {
        String configuredCron = "0 0 8 * * *";
        ImportScheduler scheduler = new ImportScheduler(countingService(new AtomicInteger()), configuredCron);
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        scheduler.configureTasks(registrar);

        TriggerTask task = registrar.getTriggerTaskList().getFirst();
        assertThat(task.getTrigger().nextExecution(new SimpleTriggerContext(
                WEDNESDAY_AT_09_16_TALLINN, WEDNESDAY_AT_09_16_TALLINN, WEDNESDAY_AT_09_16_TALLINN)))
                .isEqualTo(Instant.parse("2025-01-16T06:00:00Z"));
    }

    private WeatherImportService countingService(AtomicInteger imports) {
        return new WeatherImportService(null, null) {
            @Override
            public int importLatest() {
                imports.incrementAndGet();
                return 1;
            }
        };
    }
}
