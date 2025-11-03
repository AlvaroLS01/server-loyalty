package com.comerzzia.bricodepot.api.omnichannel.api.services.documentprint.datasource;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

public class MediosPagoDataSource extends JRBeanCollectionDataSource {

        private static final Logger LOGGER = LoggerFactory.getLogger(MediosPagoDataSource.class);
        private static final String MEDIO_PAGO_PREFIX = "medioPago";
        private static final Map<Class<?>, Map<String, Method>> READ_METHOD_CACHE = new ConcurrentHashMap<>();
        private static final Field CURRENT_BEAN_FIELD = resolveCurrentBeanField();

        public MediosPagoDataSource(Collection<?> beanCollection) {
                super(beanCollection, true);
        }

        public MediosPagoDataSource(Collection<?> beanCollection, boolean useFieldDescription) {
                super(beanCollection, useFieldDescription);
        }

        @Override
        public Object getFieldValue(JRField field) throws JRException {
                if (field == null) {
                        return null;
                }

                String description = field.getDescription();
                if (StringUtils.isNotBlank(description) && description.startsWith(MEDIO_PAGO_PREFIX)) {
                        return resolveMedioPagoField(description);
                }

                if (MEDIO_PAGO_PREFIX.equals(field.getName())) {
                        Object bean = getCurrentBean();
                        return resolveNestedProperty(bean, "desMedioPago");
                }

                return super.getFieldValue(field);
        }

        private Object resolveMedioPagoField(String description) {
                Object bean = getCurrentBean();
                if (bean == null) {
                        return null;
                }

                if (MEDIO_PAGO_PREFIX.equals(description)) {
                        return resolveNestedProperty(bean, "desMedioPago");
                }

                String remainder = StringUtils.substringAfter(description, MEDIO_PAGO_PREFIX + ".");
                Object medioPago = resolveNestedProperty(bean, MEDIO_PAGO_PREFIX);
                if (medioPago == null) {
                        medioPago = bean;
                }

                if (StringUtils.isBlank(remainder)) {
                        return medioPago;
                }

                return resolveNestedProperty(medioPago, remainder);
        }

        private Object getCurrentBean() {
                if (CURRENT_BEAN_FIELD == null) {
                        return null;
                }

                try {
                        return CURRENT_BEAN_FIELD.get(this);
                }
                catch (IllegalAccessException exception) {
                        LOGGER.debug("getCurrentBean() - Unable to access current bean", exception);
                        return null;
                }
        }

        private Object resolveNestedProperty(Object target, String path) {
                if (target == null || StringUtils.isBlank(path)) {
                        return null;
                }

                String[] segments = StringUtils.split(path, '.');
                Object current = target;

                for (String segment : segments) {
                        if (current == null) {
                                return null;
                        }

                        Method readMethod = findReadMethod(current.getClass(), segment);
                        if (readMethod == null) {
                                return null;
                        }

                        try {
                                if (!readMethod.isAccessible()) {
                                        readMethod.setAccessible(true);
                                }
                                current = readMethod.invoke(current);
                        }
                        catch (Exception exception) {
                                LOGGER.debug("resolveNestedProperty() - Unable to read '{}' from '{}'", segment,
                                                current.getClass().getName(), exception);
                                return null;
                        }
                }

                return current;
        }

        private Method findReadMethod(Class<?> type, String property) {
                Map<String, Method> methods = READ_METHOD_CACHE.computeIfAbsent(type, MediosPagoDataSource::inspectType);
                return methods.get(property);
        }

        private static Field resolveCurrentBeanField() {
                try {
                        Field field = JRBeanCollectionDataSource.class.getDeclaredField("currentBean");
                        field.setAccessible(true);
                        return field;
                }
                catch (NoSuchFieldException exception) {
                        LOGGER.error("resolveCurrentBeanField() - JasperReports JRBeanCollectionDataSource no longer exposes 'currentBean'", exception);
                        return null;
                }
        }

        private static Map<String, Method> inspectType(Class<?> type) {
                Map<String, Method> result = new ConcurrentHashMap<>();
                try {
                        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                                Method readMethod = descriptor.getReadMethod();
                                if (readMethod != null) {
                                        result.put(descriptor.getName(), readMethod);
                                }
                        }
                }
                catch (IntrospectionException exception) {
                        LOGGER.debug("inspectType() - Unable to introspect '{}'", type.getName(), exception);
                }
                return result;
        }
}
