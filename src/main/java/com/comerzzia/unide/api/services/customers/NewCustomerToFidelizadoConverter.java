package com.comerzzia.unide.api.services.customers;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.api.loyalty.persistence.customers.LyCustomerDTO;
import com.comerzzia.api.loyalty.persistence.customers.access.LoyalCustomerAccessEntityDTO;
import com.comerzzia.api.loyalty.persistence.customers.collectives.LoyalCustomerCollectiveDTO;
import com.comerzzia.api.loyalty.persistence.customers.contacttypes.LoyalCustomerContactEntity;
import com.comerzzia.api.loyalty.persistence.customers.links.LoyalCustomerLinkEntity;
import com.comerzzia.api.loyalty.web.model.customer.NewCustomer;
import com.comerzzia.api.loyalty.web.model.customertag.FidelizadoEtiquetaBeanConverter;

@Component
public class NewCustomerToFidelizadoConverter {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FidelizadoEtiquetaBeanConverter fidelizadoEtiquetaBeanConverter;

    public LyCustomerDTO convert(NewCustomer newCustomer) {
        LyCustomerDTO fidelizado = modelMapper.map(newCustomer, LyCustomerDTO.class);

        if (StringUtils.isNotBlank(fidelizado.getCountryCode())) {
            fidelizado.setCountryCode(fidelizado.getCountryCode().toUpperCase());
        }

        if (newCustomer.getContacts() != null) {
            fidelizado.setContacts(modelMapper.map(newCustomer.getContacts(), new TypeToken<List<LoyalCustomerContactEntity>>() {
            }.getType()));
        }
        if (newCustomer.getCollectives() != null) {
            fidelizado.setCollectives(modelMapper.map(newCustomer.getCollectives(), new TypeToken<List<LoyalCustomerCollectiveDTO>>() {
            }.getType()));
        }
        if (newCustomer.getTags() != null) {
            fidelizado.setTags(fidelizadoEtiquetaBeanConverter.toEtiquetaBeanList(newCustomer.getTags()));
        }
        if (newCustomer.getCustomerLink() != null) {
            fidelizado.setCustomerLink(modelMapper.map(newCustomer.getCustomerLink(), LoyalCustomerLinkEntity.class));
        }
        if (newCustomer.getNewCustomerAccess() != null) {
            fidelizado.setAccess(modelMapper.map(newCustomer.getNewCustomerAccess(), LoyalCustomerAccessEntityDTO.class));
        }
        return fidelizado;
    }
}
