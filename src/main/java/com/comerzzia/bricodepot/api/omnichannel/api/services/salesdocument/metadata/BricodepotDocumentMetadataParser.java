package com.comerzzia.bricodepot.api.omnichannel.api.services.salesdocument.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.omnichannel.domain.entity.document.DocumentEntity;
import com.comerzzia.omnichannel.service.salesdocument.metadata.DocumentMetadata;
import com.comerzzia.omnichannel.service.salesdocument.metadata.DocumentMetadataParser;

@Component
@Primary
public class BricodepotDocumentMetadataParser extends DocumentMetadataParser {

    private static final Map<String, String> TEMPLATE_ALIASES;

    static {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("FT", "ventas/facturas/facturaA4");
        aliases.put("FS", "ventas/facturas/facturaA4");
        aliases.put("NC", "ventas/facturas/facturaA4");
        aliases.put("VC", "ventas/facturas/facturaA4");
        TEMPLATE_ALIASES = Collections.unmodifiableMap(aliases);
    }

    @Override
    public DocumentMetadata getMetadata(IDatosSesion datosSesion, DocumentEntity document) {
        DocumentMetadata metadata = super.getMetadata(datosSesion, document);
        applyTemplateOverride(metadata);
        return metadata;
    }

    @Override
    public DocumentMetadata getMetadata(IDatosSesion datosSesion, byte[] documentContent) {
        DocumentMetadata metadata = super.getMetadata(datosSesion, documentContent);
        applyTemplateOverride(metadata);
        return metadata;
    }

    private void applyTemplateOverride(DocumentMetadata metadata) {
        if (metadata == null || StringUtils.isNotBlank(metadata.getPrintTemplate())) {
            return;
        }

        String docTypeCode = metadata.getDocTypeCode();
        String template = TEMPLATE_ALIASES.get(docTypeCode);

        if (StringUtils.isBlank(template) && StringUtils.isNotBlank(docTypeCode) && docTypeCode.startsWith("FT")) {
            template = "ventas/facturas/facturaA4";
        }

        if (StringUtils.isNotBlank(template)) {
            metadata.setPrintTemplate(template);
        }
    }
}
