package com.comerzzia.bricodepot.api.omnichannel.api.services.salesdocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.bricodepot.api.omnichannel.api.domain.salesdocument.BricodepotPrintedDocument;
import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.omnichannel.domain.dto.saledoc.PrintDocumentDTO;
import com.comerzzia.omnichannel.domain.entity.document.DocumentEntity;
import com.comerzzia.omnichannel.service.document.DocumentService;
import com.comerzzia.omnichannel.service.salesdocument.SaleDocumentService;

@Service
public class BricodepotSaleDocumentPrintServiceImpl implements BricodepotSaleDocumentPrintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BricodepotSaleDocumentPrintServiceImpl.class);
    private static final String PARAM_FISCAL_DATA_ATCUD = "fiscalData_ACTUD";
    private static final String TAG_FISCAL_DATA = "fiscal_data";
    private static final String TAG_PROPERTY = "property";
    private static final String TAG_NAME = "name";
    private static final String TAG_VALUE = "value";
    private static final String ATCUD = "ATCUD";

    private final SaleDocumentService saleDocumentService;
    private final DocumentService documentService;

    @Autowired
    public BricodepotSaleDocumentPrintServiceImpl(SaleDocumentService saleDocumentService,
                                                  DocumentService documentService) {
        this.saleDocumentService = saleDocumentService;
        this.documentService = documentService;
    }

    @Override
    public BricodepotPrintedDocument printDocument(IDatosSesion datosSesion, String documentUid, PrintDocumentDTO printRequest) throws ApiException {
        LOGGER.debug("printDocument() - Generating sales document '{}' with mime type '{}'", documentUid, printRequest.getMimeType());

        populateFiscalData(datosSesion, documentUid, printRequest);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            saleDocumentService.printDocument(outputStream, datosSesion, documentUid, printRequest);

            String outputDocumentName = printRequest.getOutputDocumentName();
            if (!StringUtils.hasText(outputDocumentName)) {
                outputDocumentName = documentUid;
            }

            return new BricodepotPrintedDocument(documentUid, outputDocumentName, printRequest.getMimeType(), outputStream.toByteArray());
        } catch (Exception exception) {
            LOGGER.error("printDocument() - Error generating sales document '{}'", documentUid, exception);
            if (exception instanceof ApiException) {
                throw (ApiException) exception;
            }
            throw new ApiException(exception.getMessage(), exception);
        }
    }

    private void populateFiscalData(IDatosSesion datosSesion, String documentUid, PrintDocumentDTO printRequest) {
        if (printRequest == null || printRequest.getCustomParams().containsKey(PARAM_FISCAL_DATA_ATCUD)) {
            return;
        }

        try {
            DocumentEntity documentEntity = documentService.findById(datosSesion, documentUid);
            if (documentEntity == null) {
                LOGGER.debug("populateFiscalData() - Document '{}' not found, skipping fiscal data extraction", documentUid);
                return;
            }

            byte[] content = documentEntity.getDocumentContent();
            if (content == null || content.length == 0) {
                LOGGER.debug("populateFiscalData() - Document '{}' has no content, skipping fiscal data extraction", documentUid);
                return;
            }

            String atcud = extractAtcud(content);
            if (StringUtils.hasText(atcud)) {
                printRequest.getCustomParams().put(PARAM_FISCAL_DATA_ATCUD, atcud);
            }
        } catch (Exception exception) {
            LOGGER.warn("populateFiscalData() - Unable to extract fiscal data for document '{}'", documentUid, exception);
        }
    }

    private String extractAtcud(byte[] xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlContent)) {
            Document document = builder.parse(inputStream);
            Element root = document.getDocumentElement();
            if (root == null) {
                return null;
            }

            NodeList fiscalNodes = root.getElementsByTagName(TAG_FISCAL_DATA);
            if (fiscalNodes == null || fiscalNodes.getLength() == 0) {
                return null;
            }

            Element fiscalDataElement = (Element) fiscalNodes.item(0);
            NodeList propertyNodes = fiscalDataElement.getElementsByTagName(TAG_PROPERTY);
            if (propertyNodes == null || propertyNodes.getLength() == 0) {
                return null;
            }

            for (int i = 0; i < propertyNodes.getLength(); i++) {
                Node node = propertyNodes.item(i);
                if (!(node instanceof Element)) {
                    continue;
                }

                Element propertyElement = (Element) node;
                String name = getChildTextContent(propertyElement, TAG_NAME);
                if (!ATCUD.equalsIgnoreCase(name)) {
                    continue;
                }

                return getChildTextContent(propertyElement, TAG_VALUE);
            }
        }

        return null;
    }

    private String getChildTextContent(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }

        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }

        Node node = nodes.item(0);
        if (node == null) {
            return null;
        }

        String text = node.getTextContent();
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
