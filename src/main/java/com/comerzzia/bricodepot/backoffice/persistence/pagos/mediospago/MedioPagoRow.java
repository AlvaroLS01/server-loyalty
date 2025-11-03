package com.comerzzia.bricodepot.backoffice.persistence.pagos.mediospago;

import java.math.BigDecimal;

public class MedioPagoRow {

        private String codMedioPago;
        private String desMedioPago;
        private BigDecimal importeTotal;

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

        public BigDecimal getImporteTotal() {
                return importeTotal;
        }

        public void setImporteTotal(BigDecimal importeTotal) {
                this.importeTotal = importeTotal;
        }
}
