package com.github.robert2411.m3uplaylistfilter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlaylistImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlaylistImportScheduler.class);

    @Value("${import.on-startup:true}")
    private boolean importOnStartup;

    private final M3uImportService importService;

    public PlaylistImportScheduler(M3uImportService importService) {
        this.importService = importService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!importOnStartup) {
            log.info("Startup import disabled (import.on-startup=false)");
            return;
        }
        log.info("Application started — triggering initial playlist import");
        try {
            importService.importPlaylist();
            log.info("Application started - import completed successfully");
        } catch (Exception e) {
            log.error("Startup playlist import failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledImport() {
        log.info("Starting scheduled playlist import");
        try {
            importService.importPlaylist();
            log.info("Scheduled playlist import completed successfully");
        } catch (Exception e) {
            log.error("Scheduled playlist import failed: {}", e.getMessage(), e);
        }
    }
}
