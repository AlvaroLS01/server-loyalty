package com.comerzzia.omnichannel.domain.dto.fiscal;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FiscalData {
        protected List<FiscalDataProperty> properties;

        public FiscalDataProperty getProperty(String name) {
                if (properties == null || properties.isEmpty()) {
                        return null;
                }

                String normalized = normalize(name);
                if (normalized == null) {
                        return null;
                }

                for (FiscalDataProperty property : properties) {
                        if (property == null) {
                                continue;
                        }

                        String propertyName = normalize(property.getName());
                        if (propertyName != null && propertyName.equalsIgnoreCase(normalized)) {
                                return property;
                        }
                }

                return null;
        }

        public String getPropertyValue(String name) {
                FiscalDataProperty property = getProperty(name);
                return property != null ? property.getValue() : null;
        }

        private String normalize(String value) {
                if (value == null) {
                        return null;
                }

                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }

}
