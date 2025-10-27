package com.comerzzia.bricodepot.api.omnichannel.api.web.config;

import java.util.Optional;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import com.comerzzia.api.omnichannel.web.rest.salesdoc.SalesDocumentResource;
import com.comerzzia.bricodepot.api.omnichannel.api.web.rest.salesdoc.BricodepotSalesDocumentResource;
import com.comerzzia.bricodepot.api.omnichannel.api.web.salesdocument.DocumentoVentaImpresionFilter;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import io.swagger.v3.core.util.Json;

@Configuration
public class BricodepotJerseyConfiguration extends ResourceConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BricodepotJerseyConfiguration.class);
    private static final String STANDARD_RESOURCES_BASE_PACKAGE = "com.comerzzia.api";

    @Autowired
    public BricodepotJerseyConfiguration(ApplicationContext applicationContext,
                                         DocumentoVentaImpresionFilter documentoVentaImpresionFilter) {
        LOGGER.info("Configuring Jersey REST resources");

        Json.mapper().registerModule(new JaxbAnnotationModule());
        packages("io.swagger.v3.jaxrs2.integration.resources");

        register(documentoVentaImpresionFilter);
        registerCustomResource(applicationContext, BricodepotSalesDocumentResource.class);
        registerStandardResources();
    }

    private void registerCustomResource(ApplicationContext applicationContext, Class<?> resourceType) {
        Object resourceBean = applicationContext.getBean(resourceType);
        LOGGER.info("Registering {} in Jersey configuration", resourceType.getName());
        register(resourceBean);
    }

    private void registerStandardResources() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Path.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));

        for (BeanDefinition candidate : scanner.findCandidateComponents(STANDARD_RESOURCES_BASE_PACKAGE)) {
            loadClass(candidate.getBeanClassName())
                    .filter(candidateClass -> !SalesDocumentResource.class.equals(candidateClass))
                    .ifPresent(this::registerStandardClass);
        }
    }

    private void registerStandardClass(Class<?> resourceClass) {
        LOGGER.info("Registering {} in Jersey configuration", resourceClass.getName());
        register(resourceClass);
    }

    private Optional<Class<?>> loadClass(String className) {
        if (!StringUtils.hasText(className)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException exception) {
            LOGGER.warn("Unable to register {} because the class could not be found", className, exception);
            return Optional.empty();
        }
    }
}
