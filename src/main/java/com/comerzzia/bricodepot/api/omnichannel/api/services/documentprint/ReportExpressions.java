package com.comerzzia.bricodepot.api.omnichannel.api.services.documentprint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comerzzia.omnichannel.domain.dto.fiscal.FiscalData;

/**
 * Utility methods exposed to Jasper templates to keep expressions concise and
 * resilient against data shape differences between the standard API and the
 * customised deployment.
 */
public final class ReportExpressions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportExpressions.class);
    private static final String DEFAULT_DATE_PATTERN = "dd/MM/yyyy";
    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss",
            "dd/MM/yyyy",
    };

    private ReportExpressions() {
    }

    public static String formatDate(Object value) {
        return formatDate(value, DEFAULT_DATE_PATTERN);
    }

    public static String formatDate(Object value, String pattern) {
        Date date = toDate(value);
        if (date == null) {
            return "";
        }

        SimpleDateFormat formatter = new SimpleDateFormat(StringUtils.defaultIfBlank(pattern, DEFAULT_DATE_PATTERN));
        formatter.setLenient(false);
        return formatter.format(date);
    }

    public static Date toDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }

        String text = value.toString();
        if (StringUtils.isBlank(text)) {
            return null;
        }

        text = text.trim();

        try {
            return Date.from(OffsetDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME).toInstant());
        } catch (Exception ignored) {
            // Not an ISO offset date time string, continue with other fallbacks.
        }

        try {
            return DatatypeConverter.parseDateTime(text).getTime();
        } catch (IllegalArgumentException ignored) {
            // Continue with pattern fallbacks.
        }

        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                formatter.setLenient(false);
                return formatter.parse(text);
            } catch (ParseException ignored) {
                // Try the next pattern.
            }
        }

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
            formatter.setLenient(false);
            return formatter.parse(text);
        } catch (ParseException ignored) {
            // Fall through to logging.
        }

        LOGGER.debug("Unable to parse date value '{}'", text);
        return null;
    }

    public static BigDecimal toScaledDecimal(Object value, int scale) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal.setScale(scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }

        String text = value.toString();
        if (StringUtils.isBlank(text)) {
            return BigDecimal.ZERO;
        }

        text = text.trim().replace(',', '.');

        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            LOGGER.debug("Unable to convert '{}' to BigDecimal", text, exception);
            return BigDecimal.ZERO;
        }
    }

    public static boolean hasFiscalProperty(Object fiscalData, String name) {
        return StringUtils.isNotBlank(fiscalPropertyValue(fiscalData, name));
    }

    public static String fiscalPropertyValue(Object fiscalData, String name) {
        if (fiscalData == null || StringUtils.isBlank(name)) {
            return null;
        }

        if (fiscalData instanceof FiscalData) {
            return ((FiscalData) fiscalData).getPropertyValue(name);
        }

        Object value = invokeIfPresent(fiscalData, "getPropertyValue", name);
        if (value != null) {
            return value.toString();
        }

        Object property = invokeIfPresent(fiscalData, "getProperty", name);
        if (property != null) {
            Object propertyValue = invokeIfPresent(property, "getValue");
            return propertyValue != null ? propertyValue.toString() : null;
        }

        Object data = invokeIfPresent(fiscalData, "getData");
        return data != null ? data.toString() : null;
    }

    private static Object invokeIfPresent(Object target, String methodName, Object... arguments) {
        try {
            Class<?>[] parameterTypes = new Class<?>[arguments.length];
            for (int index = 0; index < arguments.length; index++) {
                parameterTypes[index] = arguments[index] != null ? arguments[index].getClass() : Object.class;
            }

            Method method = findMethod(target.getClass(), methodName, parameterTypes);
            if (method == null) {
                return null;
            }

            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String methodName, Class<?>[] parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && parametersMatch(method.getParameterTypes(), parameterTypes)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean parametersMatch(Class<?>[] declared, Class<?>[] provided) {
        if (declared.length != provided.length) {
            return false;
        }

        for (int index = 0; index < declared.length; index++) {
            Class<?> declaredType = declared[index];
            Class<?> providedType = provided[index];
            if (providedType == Object.class) {
                continue;
            }
            if (!declaredType.isAssignableFrom(providedType)) {
                return false;
            }
        }

        return true;
    }
}
