package com.comerzzia.unide.api.services.customers;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.loyalty.service.customers.LyCustomersService;
import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.unide.api.web.model.customer.DeactivateCustomer;

public interface UnideLyCustomersService extends LyCustomersService {

        void deactivateLoyalCustomer(DeactivateCustomer deactivateModel, IDatosSesion datosSesion) throws ApiException;

        /**
         * Associates an anonymous loyal customer with the personal information
         * provided. The JSON structure matches the insert service.
         *
         * @param loyalCustomer full customer data
         * @param datosSesion   session data
         * @return updated customer information
         */
        com.comerzzia.api.loyalty.persistence.customers.LyCustomerDTO associateCustomer(
                        com.comerzzia.api.loyalty.persistence.customers.LyCustomerDTO loyalCustomer,
                        IDatosSesion datosSesion) throws ApiException;

}
