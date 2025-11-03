package com.comerzzia.bricodepot.api.omnichannel.api.config;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PagoTicketBeanPropertyConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagoTicketBeanPropertyConfiguration.class);

    @PostConstruct
    public void registerPagoTicketPropertyAdapter() {
        BeanUtilsBean current = BeanUtilsBean.getInstance();
        PropertyUtilsBean propertyUtils = current.getPropertyUtils();
        if (propertyUtils instanceof PagoTicketPropertyUtilsBean) {
            LOGGER.debug("registerPagoTicketPropertyAdapter() - Custom PagoTicket property utils already registered");
            return;
        }

        ConvertUtilsBean convertUtils = current.getConvertUtils();
        if (convertUtils == null) {
            convertUtils = new ConvertUtilsBean();
        }
        PagoTicketPropertyUtilsBean pagoTicketUtils = new PagoTicketPropertyUtilsBean();
        BeanUtilsBean customBeanUtils = new BeanUtilsBean(convertUtils, pagoTicketUtils);
        BeanUtilsBean.setInstance(customBeanUtils);
        LOGGER.debug("registerPagoTicketPropertyAdapter() - Registered custom PagoTicket property resolver for Jasper reports");
    }
}
