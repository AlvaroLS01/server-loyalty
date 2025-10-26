package com.comerzzia.pos.services.fiscaldata;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class FiscalData {

    @XmlAttribute(name = "content_type")
    protected String contentType;

    @XmlValue
    protected String data;

    @XmlElement(name = "property")
    protected List<FiscalDataProperty> properties;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<FiscalDataProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<FiscalDataProperty> properties) {
        this.properties = properties;
    }

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

    public void addProperty(FiscalDataProperty property) {
        if (property == null) {
            return;
        }

        if (this.properties == null) {
            this.properties = new ArrayList<FiscalDataProperty>();
        }

        this.properties.add(property);
    }
}
