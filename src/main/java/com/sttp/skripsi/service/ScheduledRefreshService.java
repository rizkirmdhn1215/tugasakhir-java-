package com.sttp.skripsi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduledRefreshService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledRefreshService.class);
    
    @Autowired
    private GoogleSheetsService googleSheetsService;
    
    @Value("${app.auto-refresh.enabled:true}")
    private boolean autoRefreshEnabled;
    
    @Value("${app.auto-refresh.interval:86400000}") // Default 24 hours in milliseconds
    private long refreshInterval;
    
    // Scheduled method - runs based on configuration
    @Scheduled(fixedDelayString = "${app.auto-refresh.interval:86400000}") // 24 hours default
    public void scheduledRefreshAllSheets() {
        if (!autoRefreshEnabled) {
            logger.debug("Auto-refresh is disabled");
            return;
        }
        
        logger.info("Starting scheduled refresh of all projects (interval: {} ms)", refreshInterval);
        
        try {
            List<String> processIds = googleSheetsService.refreshAllProjects(); // Changed from refreshAllSheets() to refreshAllProjects()
            logger.info("Scheduled refresh completed. Initiated {} refresh processes", processIds.size());
        } catch (Exception e) {
            logger.error("Scheduled refresh failed", e);
        }
    }
    
    // Manual trigger for testing
    public void triggerManualRefresh() {
        logger.info("Manual refresh triggered");
        scheduledRefreshAllSheets();
    }
}