package andvil.feeCalculator.weather;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/** Registers the hourly weather import in the Tallinn time zone. */
@Component
public class ImportScheduler implements SchedulingConfigurer {

    static final String DEFAULT_CRON = "0 */10 * * * *";
    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Europe/Tallinn");

    private final WeatherImportService weatherImportService;
    private final String cronExpression;

    /** Creates the scheduler using the configured cron expression or the hourly default. */
    public ImportScheduler(WeatherImportService weatherImportService,
            @Value("${weather.import.cron:" + DEFAULT_CRON + "}") String cronExpression) {
        this.weatherImportService = weatherImportService;
        this.cronExpression = cronExpression;
    }

    /** Registers the import operation with Spring's task scheduler. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        Trigger trigger = new CronTrigger(cronExpression, SCHEDULE_ZONE);
        taskRegistrar.addTriggerTask(weatherImportService::importLatest, trigger);
    }
}
