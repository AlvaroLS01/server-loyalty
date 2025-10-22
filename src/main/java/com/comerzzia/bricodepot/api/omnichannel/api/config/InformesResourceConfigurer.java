package com.comerzzia.bricodepot.api.omnichannel.api.config;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Re-applies the resolved report directory once the Spring context is ready so
 * that late initialising components within the Comerzzia stack see the
 * expected configuration.
 */
@Component
public class InformesResourceConfigurer {

    private static final Logger log = LoggerFactory.getLogger(InformesResourceConfigurer.class);

    @PostConstruct
    public void configure() {
        try {
            InformesResourceInitializer.applyRutaBase();
        } catch (Exception ex) {
            log.warn("Unable to propagate Comerzzia report directory: {}", ex.getMessage());
        }
    }
}
