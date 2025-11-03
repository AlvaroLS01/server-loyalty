package com.comerzzia.omnichannel.model.documents.sales.ticket.pagos;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.comerzzia.omnichannel.model.documents.sales.generic.util.MapAdapter;
import com.comerzzia.omnichannel.model.documents.sales.ticket.pagos.tarjeta.DatosRespuestaPagoTarjeta;
import com.comerzzia.omnichannel.model.documents.util.FormatUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "pago")
public class PagoTicket {

    @XmlElement(name = "codmedpag")
    private String codMedioPago;

    @XmlElement(name = "payment_id")
    private Integer paymentId;

    @XmlElement(name = "desmedpag")
    private String desMedioPago;

    private Long paymentMethodId;

    private BigDecimal importe;

    private DatosRespuestaPagoTarjeta datosRespuestaPagoTarjeta;

    @XmlElementWrapper(name = "giftcards")
    @XmlElement(name = "giftcard")
    private List<GiftCardBean> giftcards;

    @XmlJavaTypeAdapter(MapAdapter.class)
    private Map<String, Object> extendedData;

    private Boolean eliminable;

    private Boolean introducidoPorCajero;

    private Boolean movimientoCajaInsertado;

    private Date creationDate;

    @XmlTransient
    @JsonIgnore
    private transient FormatUtil formatUtil;

    public PagoTicket() {
        // Default constructor required by JAXB
    }

    public String getCodMedioPago() {
        return codMedioPago;
    }

    public void setCodMedioPago(String codMedioPago) {
        this.codMedioPago = codMedioPago;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public String getDesMedioPago() {
        return desMedioPago;
    }

    public void setDesMedioPago(String desMedioPago) {
        this.desMedioPago = desMedioPago;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Long paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public DatosRespuestaPagoTarjeta getDatosRespuestaPagoTarjeta() {
        return datosRespuestaPagoTarjeta;
    }

    public void setDatosRespuestaPagoTarjeta(DatosRespuestaPagoTarjeta datosRespuestaPagoTarjeta) {
        this.datosRespuestaPagoTarjeta = datosRespuestaPagoTarjeta;
    }

    public List<GiftCardBean> getGiftcards() {
        return giftcards;
    }

    public void setGiftcards(List<GiftCardBean> giftcards) {
        this.giftcards = giftcards;
    }

    public Map<String, Object> getExtendedData() {
        return extendedData;
    }

    public void setExtendedData(Map<String, Object> extendedData) {
        this.extendedData = extendedData;
    }

    public Boolean getEliminable() {
        return eliminable;
    }

    public void setEliminable(Boolean eliminable) {
        this.eliminable = eliminable;
    }

    public Boolean getIntroducidoPorCajero() {
        return introducidoPorCajero;
    }

    public void setIntroducidoPorCajero(Boolean introducidoPorCajero) {
        this.introducidoPorCajero = introducidoPorCajero;
    }

    public Boolean getMovimientoCajaInsertado() {
        return movimientoCajaInsertado;
    }

    public void setMovimientoCajaInsertado(Boolean movimientoCajaInsertado) {
        this.movimientoCajaInsertado = movimientoCajaInsertado;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public FormatUtil getFormatUtil() {
        return formatUtil;
    }

    public void setFormatUtil(FormatUtil formatUtil) {
        this.formatUtil = formatUtil;
    }

    @JsonIgnore
    public String getImporteAsString() {
        if (importe == null) {
            return null;
        }

        if (formatUtil == null) {
            return importe.toPlainString();
        }

        return formatUtil.formatAmount(importe);
    }

    @JsonIgnore
    public PagoTicket getMedioPago() {
        return this;
    }
}
