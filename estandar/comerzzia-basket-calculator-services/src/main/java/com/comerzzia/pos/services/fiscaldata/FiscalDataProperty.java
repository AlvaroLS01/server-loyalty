package com.comerzzia.pos.services.fiscaldata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class FiscalDataProperty {

    @XmlElement(name = "name")
    protected String name;

    @XmlElement(name = "value")
    protected String value;

    public FiscalDataProperty() {
    }

    public FiscalDataProperty(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
