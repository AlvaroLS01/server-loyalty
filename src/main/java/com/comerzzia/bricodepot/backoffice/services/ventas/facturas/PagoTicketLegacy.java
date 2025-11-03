package com.comerzzia.bricodepot.backoffice.services.ventas.facturas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.comerzzia.bricodepot.backoffice.persistence.pagos.mediospago.MedioPagoRow;

public final class PagoTicketLegacy {

        private final String codMedioPago;
        private final BigDecimal importe;
        private final MedioPago medioPago;

        private final Object paymentId;
        private final Object paymentMethodId;
        private final Object datosRespuestaPagoTarjeta;
        private final Collection<?> giftcards;
        private final Object extendedData;
        private final Boolean eliminable;
        private final Boolean introducidoPorCajero;
        private final Boolean movimientoCajaInsertado;
        private final Date creationDate;
        private final Object formatUtil;

        PagoTicketLegacy(MedioPagoRow row, Map<String, Object> baseProperties) {
                this.codMedioPago = row != null ? row.getCodMedioPago() : null;
                this.importe = row != null ? row.getImporteTotal() : null;
                this.medioPago = new MedioPago(row != null ? row.getCodMedioPago() : null,
                                row != null ? row.getDesMedioPago() : null);

                this.paymentId = baseProperties.get("paymentId");
                this.paymentMethodId = baseProperties.get("paymentMethodId");
                this.datosRespuestaPagoTarjeta = baseProperties.get("datosRespuestaPagoTarjeta");
                Object giftcardsValue = baseProperties.get("giftcards");
                if (giftcardsValue instanceof Collection) {
                        Collection<?> collection = (Collection<?>) giftcardsValue;
                        if (CollectionUtils.isEmpty(collection)) {
                                this.giftcards = Collections.emptyList();
                        }
                        else {
                                this.giftcards = Collections.unmodifiableCollection(new ArrayList<>(collection));
                        }
                }
                else {
                        this.giftcards = Collections.emptyList();
                }
                this.extendedData = baseProperties.get("extendedData");
                this.eliminable = (Boolean) baseProperties.get("eliminable");
                this.introducidoPorCajero = (Boolean) baseProperties.get("introducidoPorCajero");
                this.movimientoCajaInsertado = (Boolean) baseProperties.get("movimientoCajaInsertado");
                this.creationDate = (Date) baseProperties.get("creationDate");
                this.formatUtil = baseProperties.get("formatUtil");
        }

        public String getCodMedioPago() {
                return codMedioPago;
        }

        public String getDesMedioPago() {
                return medioPago.getDesMedioPago();
        }

        public BigDecimal getImporte() {
                return importe;
        }

        public MedioPago getMedioPago() {
                return medioPago;
        }

        public Object getPaymentId() {
                return paymentId;
        }

        public Object getPaymentMethodId() {
                return paymentMethodId;
        }

        public Object getDatosRespuestaPagoTarjeta() {
                return datosRespuestaPagoTarjeta;
        }

        public Collection<?> getGiftcards() {
                return giftcards;
        }

        public Object getExtendedData() {
                return extendedData;
        }

        public Boolean isEliminable() {
                return eliminable;
        }

        public Boolean isIntroducidoPorCajero() {
                return introducidoPorCajero;
        }

        public Boolean isMovimientoCajaInsertado() {
                return movimientoCajaInsertado;
        }

        public Date getCreationDate() {
                return creationDate;
        }

        public Object getFormatUtil() {
                return formatUtil;
        }

        public static final class MedioPago {

                private final String codMedioPago;
                private final String desMedioPago;

                public MedioPago(String codigo, String descripcion) {
                        this.codMedioPago = codigo;
                        this.desMedioPago = descripcion;
                }

                public String getCodMedioPago() {
                        return codMedioPago;
                }

                public String getDesMedioPago() {
                        return desMedioPago;
                }
        }
}
