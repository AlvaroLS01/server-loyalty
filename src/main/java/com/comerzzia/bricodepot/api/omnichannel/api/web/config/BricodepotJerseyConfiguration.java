package com.comerzzia.bricodepot.api.omnichannel.api.web.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.api.omnichannel.web.config.JerseyConfiguration;
import com.comerzzia.api.omnichannel.web.rest.salesdoc.SalesDocumentResource;
import com.comerzzia.bricodepot.api.omnichannel.api.web.rest.salesdoc.BricodepotSalesDocumentResource;
import com.comerzzia.bricodepot.api.omnichannel.api.web.salesdocument.DocumentoVentaImpresionFilter;

/**
 * Custom Jersey configuration that extends the standard omnichannel setup and
 * replaces the default sales document resource with the Bricodepot specific
 * implementation while wiring the filter used to trace print operations.
 */
@Component
@Primary
public class BricodepotJerseyConfiguration extends JerseyConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BricodepotJerseyConfiguration.class);

    public BricodepotJerseyConfiguration(DocumentoVentaImpresionFilter documentoVentaImpresionFilter,
                                         BricodepotSalesDocumentResource bricodepotSalesDocumentResource) {
        super();
        register(documentoVentaImpresionFilter);
        replaceDefaultSalesDocumentResource();
        register(bricodepotSalesDocumentResource);
    }

    private void replaceDefaultSalesDocumentResource() {
        Set<Class<?>> registeredClasses = getClasses();
        boolean removedClass = registeredClasses.remove(SalesDocumentResource.class);
        boolean removedSingleton = getSingletons().removeIf(SalesDocumentResource.class::isInstance);

        if (removedClass || removedSingleton) {
            LOGGER.info("Replacing standard SalesDocumentResource registration with Bricodepot implementation");
        } else {
            LOGGER.debug("Default SalesDocumentResource was not registered, skipping removal");
        }
    }
}
