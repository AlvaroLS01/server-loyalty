package com.comerzzia.bricodepot.api.omnichannel.api.config;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Invokes the {@link ComerzziaHomeInitializer} once the Spring context is ready so that
 * the Comerzzia legacy configuration picks up the location of the bundled report
 * templates.
 */
@Component
public class ComerzziaAppInfoCustomizer {

    private static final Logger log = LoggerFactory.getLogger(ComerzziaAppInfoCustomizer.class);

    @PostConstruct
    public void customizeAppInfo() {
        try {
            ComerzziaHomeInitializer.applyRutaBase();
        } catch (Exception ex) {
            log.warn("Unable to update Comerzzia AppInfo configuration: {}", ex.getMessage());
        }
    }
}
