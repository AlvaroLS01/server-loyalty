package com.comerzzia.bricodepot.api.omnichannel.api.config;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtilsBean;

import com.comerzzia.omnichannel.model.documents.sales.ticket.pagos.PagoTicket;

class PagoTicketPropertyUtilsBean extends PropertyUtilsBean {

    private static final long serialVersionUID = 1L;
    private static final String MEDIO_PAGO_PROPERTY = "medioPago";

    @Override
    public Object getSimpleProperty(Object bean, String name)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (bean instanceof PagoTicket && MEDIO_PAGO_PROPERTY.equalsIgnoreCase(name)) {
            return new MedioPagoDescriptor((PagoTicket) bean);
        }
        return super.getSimpleProperty(bean, name);
    }

    private static final class MedioPagoDescriptor implements Serializable {

        private static final long serialVersionUID = 1L;

        private final PagoTicket delegate;

        private MedioPagoDescriptor(PagoTicket delegate) {
            this.delegate = delegate;
        }

        public String getCodMedioPago() {
            return delegate != null ? delegate.getCodMedioPago() : null;
        }

        public String getDesMedioPago() {
            return delegate != null ? delegate.getDesMedioPago() : null;
        }
    }
}
