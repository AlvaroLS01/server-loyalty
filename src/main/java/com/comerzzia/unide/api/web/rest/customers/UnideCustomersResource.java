package com.comerzzia.unide.api.web.rest.customers;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.core.service.util.ComerzziaDatosSesion;
import com.comerzzia.unide.api.services.customers.UnideLyCustomersService;
import com.comerzzia.unide.api.web.model.customer.DeactivateCustomer;

import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/customers")
@Tag(name = "Customers", description = "Loyal customers services")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Controller
public class UnideCustomersResource {

	@Resource(name = "datosSesionRequest")
	protected ComerzziaDatosSesion datosSesionRequest;

	@Autowired
	protected UnideLyCustomersService service;

	@PUT
	@Path("/{lyCustomerId}/deactivate")
	public void deleteLoyalCustomer(@Valid DeactivateCustomer record) throws ApiException {
		try {
			service.deactivateLoyalCustomer(record, datosSesionRequest.getDatosSesionBean());
		}
		catch (ApiException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ApiException(e.getMessage(), e);
		}
	}
	
	 @POST
     @Path("/createCustomer")
     public com.comerzzia.api.loyalty.persistence.customers.LyCustomerDTO associateCustomer(
                     com.comerzzia.api.loyalty.persistence.customers.LyCustomerDTO record) throws ApiException {
             try {
                     return service.associateCustomer(record, datosSesionRequest.getDatosSesionBean());
             }
             catch (ApiException e) {
                     throw e;
             }
             catch (Exception e) {
                     throw new ApiException(e.getMessage(), e);
             }
     }
}
