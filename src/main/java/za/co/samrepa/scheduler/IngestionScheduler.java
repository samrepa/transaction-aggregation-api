package za.co.samrepa.scheduler;

import za.co.samrepa.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionScheduler {

    private final IngestionService ingestionService;

    @Scheduled(fixedDelayString = "${ingestion.interval-ms:300000}")
    public void run() {
        try {
            ingestionService.ingestAll();
        } catch (Exception ex) {
            log.error("Scheduled ingestion failed: {}", ex.getMessage(), ex);
        }
    }
}
