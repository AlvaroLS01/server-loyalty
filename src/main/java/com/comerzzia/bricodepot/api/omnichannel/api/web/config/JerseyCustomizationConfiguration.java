package com.comerzzia.bricodepot.api.omnichannel.api.web.config;

import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.comerzzia.api.omnichannel.web.rest.salesdoc.SalesDocumentResource;
import com.comerzzia.bricodepot.api.omnichannel.api.web.rest.salesdoc.BricodepotSalesDocumentResource;
import com.comerzzia.bricodepot.api.omnichannel.api.web.salesdocument.DocumentoVentaImpresionFilter;
import com.comerzzia.bricodepot.api.omnichannel.api.web.salesdocument.DocumentoVentaImpresionServicio;

@Configuration
public class JerseyCustomizationConfiguration {

    @Bean
    public DocumentoVentaImpresionFilter documentoVentaImpresionFilter(
            DocumentoVentaImpresionServicio servicioImpresion) {
        return new DocumentoVentaImpresionFilter(servicioImpresion);
    }

    @Bean
    public ResourceConfigCustomizer documentoVentaImpresionFilterCustomizer(
            DocumentoVentaImpresionFilter documentoVentaImpresionFilter) {
        return resourceConfig -> resourceConfig.register(documentoVentaImpresionFilter);
    }

    @Bean
    public ResourceConfigCustomizer bricodepotSalesDocumentResourceCustomizer(
            BricodepotSalesDocumentResource bricodepotSalesDocumentResource) {
        return resourceConfig -> {
            resourceConfig.getClasses().removeIf(SalesDocumentResource.class::equals);
            resourceConfig.getSingletons().removeIf(instance -> SalesDocumentResource.class.equals(instance.getClass()));
            resourceConfig.register(bricodepotSalesDocumentResource);
        };
    }
}
