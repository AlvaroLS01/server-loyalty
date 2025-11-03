package com.comerzzia.bricodepot.backoffice.services.ventas.facturas;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import com.comerzzia.bricodepot.backoffice.persistence.pagos.mediospago.MedioPagoRow;
import com.comerzzia.bricodepot.backoffice.persistence.pagos.mediospago.MediosPagosMapper;

@Service
public class CargarFacturaA4ServicioImpl implements CargarFacturaA4Servicio {

        private static final Logger LOGGER = LoggerFactory.getLogger(CargarFacturaA4ServicioImpl.class);
        private static final String PROPERTY_CABECERA = "Cabecera";
        private static final String PROPERTY_PAGOS = "Pagos";

        private final MediosPagosMapper mediosPagosMapper;

        @Autowired
        public CargarFacturaA4ServicioImpl(MediosPagosMapper mediosPagosMapper) {
                this.mediosPagosMapper = mediosPagosMapper;
        }

        @Override
        public void generarMediosPago(Object ticketVenta) {
                if (ticketVenta == null) {
                        LOGGER.debug("generarMediosPago() - Ticket is null, skipping payment enrichment");
                        return;
                }

                Object cabecera = invokeGetter(ticketVenta, PROPERTY_CABECERA);
                if (cabecera == null) {
                        LOGGER.debug("generarMediosPago() - Ticket '{}' has no cabecera", ticketVenta.getClass().getName());
                        return;
                }

                String uidActividad = stringValue(invokeGetter(cabecera, "UidActividad"));
                String uidTicket = stringValue(invokeGetter(cabecera, "UidTicket"));

                if (StringUtils.isBlank(uidActividad) || StringUtils.isBlank(uidTicket)) {
                        LOGGER.debug("generarMediosPago() - Missing activity '{}' or ticket '{}' identifiers", uidActividad,
                                        uidTicket);
                        return;
                }

                List<MedioPagoRow> mediosPago = mediosPagosMapper.getMediosPago(uidActividad, uidTicket);
                if (CollectionUtils.isEmpty(mediosPago)) {
                        LOGGER.debug("generarMediosPago() - No payment rows found for activity '{}' ticket '{}'", uidActividad,
                                        uidTicket);
                        clearExistingPayments(ticketVenta);
                        return;
                }

                Map<String, Map<String, Object>> metadata = extractPaymentMetadata(ticketVenta);
                List<PagoTicketLegacy> reconstruidos = new ArrayList<>(mediosPago.size());

                for (MedioPagoRow row : mediosPago) {
                        Map<String, Object> base = metadata.getOrDefault(normalizeKey(row.getCodMedioPago()),
                                        Collections.emptyMap());
                        reconstruidos.add(new PagoTicketLegacy(row, base));
                }

                replaceTicketPayments(ticketVenta, reconstruidos);
        }

        private void clearExistingPayments(Object ticketVenta) {
                Collection<?> pagos = toCollection(invokeGetter(ticketVenta, PROPERTY_PAGOS));
                if (pagos != null) {
                        try {
                                pagos.clear();
                        }
                        catch (UnsupportedOperationException exception) {
                                LOGGER.debug("clearExistingPayments() - Unable to clear payment collection", exception);
                        }
                }
        }

        private Map<String, Map<String, Object>> extractPaymentMetadata(Object ticketVenta) {
                Map<String, Map<String, Object>> result = new LinkedHashMap<>();
                Collection<?> pagos = toCollection(invokeGetter(ticketVenta, PROPERTY_PAGOS));
                if (CollectionUtils.isEmpty(pagos)) {
                        return result;
                }

                for (Object pago : pagos) {
                        if (pago == null) {
                                continue;
                        }
                        String codigo = normalizeKey(stringValue(invokeGetter(pago, "CodMedioPago")));
                        if (codigo == null) {
                                continue;
                        }

                        Map<String, Object> values = result.computeIfAbsent(codigo, key -> new HashMap<>());
                        copyIfPresent(pago, "getPaymentId", values, "paymentId");
                        copyIfPresent(pago, "getPaymentMethodId", values, "paymentMethodId");
                        copyIfPresent(pago, "getDatosRespuestaPagoTarjeta", values, "datosRespuestaPagoTarjeta");
                        mergeCollections(pago, "getGiftcards", values, "giftcards");
                        copyIfPresent(pago, "getExtendedData", values, "extendedData");
                        copyIfPresent(pago, "isEliminable", values, "eliminable");
                        copyIfPresent(pago, "getEliminable", values, "eliminable");
                        copyIfPresent(pago, "isIntroducidoPorCajero", values, "introducidoPorCajero");
                        copyIfPresent(pago, "isMovimientoCajaInsertado", values, "movimientoCajaInsertado");
                        copyIfPresent(pago, "getCreationDate", values, "creationDate");
                        copyIfPresent(pago, "getFormatUtil", values, "formatUtil");
                }

                return result;
        }

        private void copyIfPresent(Object target, String methodName, Map<String, Object> destination, String key) {
                Object value = invokeMethod(target, methodName);
                if (value != null) {
                        destination.put(key, value);
                }
        }

        @SuppressWarnings("unchecked")
        private void mergeCollections(Object target, String methodName, Map<String, Object> destination, String key) {
                Object value = invokeMethod(target, methodName);
                if (!(value instanceof Collection)) {
                        return;
                }

                Collection<Object> collection = (Collection<Object>) value;
                if (CollectionUtils.isEmpty(collection)) {
                        return;
                }

                List<Object> aggregated = (List<Object>) destination.computeIfAbsent(key, unused -> new ArrayList<>());
                aggregated.addAll(collection);
        }

        private void replaceTicketPayments(Object ticketVenta, List<PagoTicketLegacy> pagos) {
                Object existing = invokeGetter(ticketVenta, PROPERTY_PAGOS);
                if (existing instanceof Collection) {
                        Collection<Object> collection = castCollection(existing);
                        try {
                                collection.clear();
                                collection.addAll(pagos);
                                return;
                        }
                        catch (UnsupportedOperationException exception) {
                                LOGGER.debug("replaceTicketPayments() - Payment collection is immutable, switching to setter", exception);
                        }
                }

                Method setter = ReflectionUtils.findMethod(ticketVenta.getClass(), "set" + PROPERTY_PAGOS, List.class);
                if (setter != null) {
                        invokeSetter(ticketVenta, setter, new ArrayList<>(pagos));
                        return;
                }

                Field field = ReflectionUtils.findField(ticketVenta.getClass(), PROPERTY_PAGOS.toLowerCase());
                if (field != null) {
                        ReflectionUtils.makeAccessible(field);
                        try {
                                field.set(ticketVenta, new ArrayList<>(pagos));
                                return;
                        }
                        catch (IllegalAccessException exception) {
                                LOGGER.warn("replaceTicketPayments() - Unable to set pagos field via reflection", exception);
                        }
                }

                LOGGER.warn("replaceTicketPayments() - Unable to inject reconstructed payments into ticket class '{}'", ticketVenta.getClass().getName());
        }

        private Collection<Object> castCollection(Object value) {
                if (value instanceof Collection) {
                        @SuppressWarnings("unchecked")
                        Collection<Object> casted = (Collection<Object>) value;
                        return casted;
                }
                return null;
        }

        private Object invokeGetter(Object target, String property) {
                if (target == null || StringUtils.isBlank(property)) {
                        return null;
                }

                String methodName = "get" + property;
                Object value = invokeMethod(target, methodName);
                if (value != null || hasMethod(target.getClass(), methodName)) {
                        return value;
                }

                methodName = "is" + property;
                return invokeMethod(target, methodName);
        }

        private boolean hasMethod(Class<?> type, String methodName) {
                return ReflectionUtils.findMethod(type, methodName) != null;
        }

        private Object invokeMethod(Object target, String methodName) {
                if (target == null || StringUtils.isBlank(methodName)) {
                        return null;
                }

                Method method = ReflectionUtils.findMethod(target.getClass(), methodName);
                if (method == null) {
                        return null;
                }

                ReflectionUtils.makeAccessible(method);
                try {
                        return method.invoke(target);
                }
                catch (Exception exception) {
                        LOGGER.debug("invokeMethod() - Unable to execute '{}' on '{}'", methodName,
                                        target.getClass().getName(), exception);
                        return null;
                }
        }

        private void invokeSetter(Object target, Method setter, Object value) {
                ReflectionUtils.makeAccessible(setter);
                try {
                        setter.invoke(target, value);
                }
                catch (Exception exception) {
                        LOGGER.warn("invokeSetter() - Unable to execute '{}' on '{}'", setter.getName(),
                                        target.getClass().getName(), exception);
                }
        }

        private String stringValue(Object value) {
                return value != null ? Objects.toString(value, null) : null;
        }

        private Collection<?> toCollection(Object value) {
                if (value instanceof Collection) {
                        return (Collection<?>) value;
                }
                return null;
        }

        private String normalizeKey(String value) {
                if (StringUtils.isBlank(value)) {
                        return null;
                }
                return value.trim();
        }
}
