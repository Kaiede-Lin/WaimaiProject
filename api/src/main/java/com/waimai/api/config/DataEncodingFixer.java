package com.waimai.api.config;

import com.waimai.service.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Fix garbled Chinese characters in system_config.description caused by
 * incorrect JDBC character encoding during initial migration.
 * Safe to run repeatedly — only updates rows whose description contains garbled text.
 */
@Component
public class DataEncodingFixer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataEncodingFixer.class);
    private final SystemConfigService systemConfigService;

    public DataEncodingFixer(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public void run(String... args) {
        try {
            int fixed = systemConfigService.fixEncoding();
            if (fixed > 0) {
                log.info("Fixed {} system_config description(s) with encoding issues", fixed);
            }
        } catch (Exception e) {
            log.warn("DataEncodingFixer skipped (DB may not be ready): {}", e.getMessage());
        }
    }
}
