package com.comerzzia.bricodepot.api.omnichannel.api.services.salesdocument;

import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.bricodepot.api.omnichannel.api.domain.salesdocument.BricodepotPrintedDocument;
import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.omnichannel.domain.dto.saledoc.PrintDocumentDTO;
import com.comerzzia.omnichannel.service.salesdocument.SaleDocumentService;

@Service
public class BricodepotSaleDocumentPrintServiceImpl implements BricodepotSaleDocumentPrintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BricodepotSaleDocumentPrintServiceImpl.class);

    private final SaleDocumentService saleDocumentService;

    @Autowired
    public BricodepotSaleDocumentPrintServiceImpl(SaleDocumentService saleDocumentService) {
        this.saleDocumentService = saleDocumentService;
    }

    @Override
    public BricodepotPrintedDocument printDocument(IDatosSesion datosSesion, String documentUid, PrintDocumentDTO printRequest) throws ApiException {
        LOGGER.debug("printDocument() - Generating sales document '{}' with mime type '{}'", documentUid, printRequest.getMimeType());

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
}
