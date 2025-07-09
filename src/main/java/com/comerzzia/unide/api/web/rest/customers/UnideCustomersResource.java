package com.comerzzia.unide.api.web.rest.customers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.core.service.util.ComerzziaDatosSesion;
import com.comerzzia.api.loyalty.persistence.cards.CardEntity;
import com.comerzzia.api.loyalty.persistence.customers.LyCustomerDTO;
import com.comerzzia.api.loyalty.persistence.customers.access.LoyalCustomerAccessEntityDTO;
import com.comerzzia.api.loyalty.persistence.customers.collectives.LoyalCustomerCollectiveDTO;
import com.comerzzia.api.loyalty.persistence.customers.contacttypes.LoyalCustomerContactEntity;
import com.comerzzia.api.loyalty.persistence.customers.links.LoyalCustomerLinkEntity;
import com.comerzzia.api.loyalty.web.model.customer.NewCustomer;
import javax.ws.rs.BadRequestException;
import com.comerzzia.api.loyalty.web.model.customertag.FidelizadoEtiquetaBeanConverter;
import com.comerzzia.unide.api.services.customers.UnideLyCustomersService;
import com.comerzzia.unide.api.web.model.customer.AssociateCustomerRequest;
import com.comerzzia.unide.api.web.model.customer.DeactivateCustomer;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	protected ModelMapper modelMapper;
	
	@Autowired
	protected FidelizadoEtiquetaBeanConverter fidelizadoEtiquetaBeanConverter;
	
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
	
	@PUT
	@Path("/associateCustomer")
	public LyCustomerDTO associateCustomer(@Valid AssociateCustomerRequest record) throws ApiException {
		// 1) Validación de tarjeta obligatoria
		if (record.getCards() == null || record.getCards().isEmpty() || StringUtils.isBlank(record.getCards().get(0).getCardNumber())) {
			throw new BadRequestException("Card number is mandatory");
		}

		try {
			// 2) Mappea el JSON a tu DTO de persistencia
			LyCustomerDTO dto = mapNewCustomerToFidelizadoBean(record);

			// 3) Mapea el acceso si viene
			if (record.getNewCustomerAccess() != null) {
				ObjectMapper mapper = new ObjectMapper();
				LoyalCustomerAccessEntityDTO access = mapper.convertValue(record.getNewCustomerAccess(), LoyalCustomerAccessEntityDTO.class);
				dto.setAccess(access);
			}

			// 4) Añade la lista de tarjetas (sólo la primera, obligatoria)
			List<CardEntity> cards = new ArrayList<>();
			for (AssociateCustomerRequest.Card c : record.getCards()) {
				CardEntity card = new CardEntity();
				card.setCardNumber(c.getCardNumber());
				cards.add(card);
			}
			dto.setCards(cards);

			// 5) Llama al servicio transaccional
			return service.associateCustomer(dto, datosSesionRequest.getDatosSesionBean());
		}
		catch (ApiException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ApiException(e.getMessage(), e);
		}
	}

	public LyCustomerDTO mapNewCustomerToFidelizadoBean(NewCustomer newCustomer) {
		LyCustomerDTO fidelizado = modelMapper.map(newCustomer, LyCustomerDTO.class);

		if (StringUtils.isNotBlank(fidelizado.getCountryCode())) {
			fidelizado.setCountryCode(fidelizado.getCountryCode().toUpperCase());
		}

		if (newCustomer.getContacts() != null) {
			modelMapper.map(newCustomer.getContacts(), new TypeToken<List<LoyalCustomerContactEntity>>(){
			}.getType());
			fidelizado.setContacts(modelMapper.map(newCustomer.getContacts(), new TypeToken<List<LoyalCustomerContactEntity>>(){
			}.getType()));
		}
		if (newCustomer.getCollectives() != null) {

			fidelizado.setCollectives(modelMapper.map(newCustomer.getCollectives(), new TypeToken<List<LoyalCustomerCollectiveDTO>>(){
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
