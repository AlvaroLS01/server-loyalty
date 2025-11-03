package com.comerzzia.bricodepot.api.omnichannel.api.domain.salesdocument;

import com.comerzzia.omnichannel.model.documents.sales.ticket.pagos.PagoTicket;

/**
 * Lightweight extension of {@link PagoTicket} that exposes a {@code medioPago}
 * descriptor so Jasper reports can resolve expressions such as
 * {@code medioPago.desMedioPago} without altering the original JRXML templates.
 */
public class PagoTicketWithMedioPago extends PagoTicket {

    private MedioPago medioPago;

    public PagoTicketWithMedioPago() {
    }

    public PagoTicketWithMedioPago(PagoTicket source) {
        copyFrom(source);
    }

    public void copyFrom(PagoTicket source) {
        if (source == null) {
            return;
        }

        setCodMedioPago(source.getCodMedioPago());
        setPaymentId(source.getPaymentId());
        setDesMedioPago(source.getDesMedioPago());
        setPaymentMethodId(source.getPaymentMethodId());
        super.setImporte(source.getImporte());
        super.setDatosRespuestaPagoTarjeta(source.getDatosRespuestaPagoTarjeta());
        super.setGiftcards(source.getGiftcards());
        super.setExtendedData(source.getExtendedData());
        super.setEliminable(source.getEliminable());
        super.setIntroducidoPorCajero(source.getIntroducidoPorCajero());
        super.setMovimientoCajaInsertado(source.getMovimientoCajaInsertado());
        super.setCreationDate(source.getCreationDate());
        super.setFormatUtil(source.getFormatUtil());

        MedioPago descriptor = getMedioPago();
        descriptor.setCodMedioPago(source.getCodMedioPago());
        descriptor.setDesMedioPago(source.getDesMedioPago());
    }

    public MedioPago getMedioPago() {
        if (medioPago == null) {
            medioPago = new MedioPago();
        }
        return medioPago;
    }

    public void setMedioPago(MedioPago medioPago) {
        this.medioPago = medioPago;
    }

    /**
     * Simple bean mirroring the information exposed by
     * {@link com.comerzzia.omnichannel.model.documents.sales.generic.util.MapAdapter}
     * in the legacy backoffice so the same JRXML definitions can be reused.
     */
    public static class MedioPago {

        private String codMedioPago;
        private String desMedioPago;

        public String getCodMedioPago() {
            return codMedioPago;
        }

        public void setCodMedioPago(String codMedioPago) {
            this.codMedioPago = codMedioPago;
        }

        public String getDesMedioPago() {
            return desMedioPago;
        }

        public void setDesMedioPago(String desMedioPago) {
            this.desMedioPago = desMedioPago;
        }
    }

}
