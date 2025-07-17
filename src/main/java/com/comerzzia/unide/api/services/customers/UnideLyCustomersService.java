package com.comerzzia.unide.api.services.customers;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.loyalty.persistence.customers.LyCustomerDTO;
import com.comerzzia.api.loyalty.service.customers.LyCustomersService;
import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.unide.api.web.model.customer.AssociateCustomerRequest;
import com.comerzzia.unide.api.web.model.customer.DeactivateCustomer;

public interface UnideLyCustomersService extends LyCustomersService {

	void deactivateLoyalCustomer(DeactivateCustomer deactivateModel, IDatosSesion datosSesion) throws ApiException;

	LyCustomerDTO associateCustomer(LyCustomerDTO loyalCustomer, IDatosSesion datosSesion) throws ApiException;

	LyCustomerDTO associateCustomer(AssociateCustomerRequest record, IDatosSesion datosSesion) throws ApiException;
}
