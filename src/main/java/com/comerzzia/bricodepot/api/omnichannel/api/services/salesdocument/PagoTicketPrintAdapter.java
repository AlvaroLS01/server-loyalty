package com.comerzzia.bricodepot.api.omnichannel.api.services.salesdocument;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;

import com.comerzzia.omnichannel.model.documents.sales.ticket.pagos.PagoTicket;

/**
 * Decorates {@link PagoTicket} instances with the additional structure expected by the
 * Jasper templates (i.e. an embedded {@code medioPago} bean exposing the description).
 */
final class PagoTicketPrintAdapter extends PagoTicket {

    private static final long serialVersionUID = 1L;

    private MedioPagoDescriptor medioPago;

    private PagoTicketPrintAdapter(PagoTicket source) {
        if (source != null) {
            BeanUtils.copyProperties(source, this);
            this.medioPago = new MedioPagoDescriptor(source.getCodMedioPago(), source.getDesMedioPago());
        }
    }

    static PagoTicket wrap(PagoTicket source) {
        if (source == null || source instanceof PagoTicketPrintAdapter) {
            return source;
        }
        return new PagoTicketPrintAdapter(source);
    }

    static List<PagoTicket> wrapList(List<PagoTicket> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        boolean needsWrap = false;
        for (PagoTicket pago : source) {
            if (!(pago instanceof PagoTicketPrintAdapter)) {
                needsWrap = true;
                break;
            }
        }

        if (!needsWrap) {
            return source;
        }

        List<PagoTicket> result = new ArrayList<>(source.size());
        for (PagoTicket pago : source) {
            result.add(wrap(pago));
        }
        return result;
    }

    public MedioPagoDescriptor getMedioPago() {
        if (medioPago == null) {
            medioPago = new MedioPagoDescriptor(getCodMedioPago(), getDesMedioPago());
        }
        return medioPago;
    }

    public static final class MedioPagoDescriptor {

        private final String codMedioPago;
        private final String desMedioPago;

        MedioPagoDescriptor(String codMedioPago, String desMedioPago) {
            this.codMedioPago = codMedioPago;
            this.desMedioPago = desMedioPago;
        }

        public String getCodMedioPago() {
            return codMedioPago;
        }

        public String getDesMedioPago() {
            return desMedioPago;
        }
    }
}
