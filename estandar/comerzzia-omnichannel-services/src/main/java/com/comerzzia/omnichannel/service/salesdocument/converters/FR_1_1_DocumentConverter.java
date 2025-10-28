package com.comerzzia.omnichannel.service.salesdocument.converters;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.comerzzia.omnichannel.model.documents.sales.FR_1_1_Document;

@Component("FR_1_1_DocumentConverter")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FR_1_1_DocumentConverter extends AbstractTicketVentaAbonoConverter<FR_1_1_Document> {
}
