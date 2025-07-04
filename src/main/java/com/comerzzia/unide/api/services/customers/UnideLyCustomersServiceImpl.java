package com.comerzzia.unide.api.services.customers;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.core.service.exception.BadRequestException;
import com.comerzzia.api.core.service.exception.NotFoundException;
import com.comerzzia.api.loyalty.persistence.cards.CardEntity;
import com.comerzzia.api.loyalty.persistence.customers.LyCustomerDTO;
import com.comerzzia.api.loyalty.persistence.customers.LyCustomerEntity;
import com.comerzzia.api.loyalty.persistence.customers.LyCustomerExample;
import com.comerzzia.api.loyalty.persistence.customers.contacttypes.LoyalCustomerContactEntity;
import com.comerzzia.api.loyalty.persistence.customers.contacttypes.LoyalCustomerContactExample;
import com.comerzzia.api.loyalty.persistence.customers.contacttypes.LoyalCustomerContactMapper;
import com.comerzzia.api.loyalty.service.customers.LyCustomersServiceImpl;
import com.comerzzia.api.loyalty.service.customers.versioning.LoyalCustomerVersion;
import com.comerzzia.api.loyalty.web.rest.customers.CustomerTagsResource;
import com.comerzzia.core.model.etiquetas.categorias.EtiquetaBean;
import com.comerzzia.core.servicios.contadores.ContadorException;
import com.comerzzia.core.servicios.etiquetas.EtiquetasException;
import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.core.servicios.variables.VariableException;
import com.comerzzia.core.servicios.variables.VariableNotFoundException;
import com.comerzzia.core.util.criptografia.CriptoException;
import com.comerzzia.core.util.mybatis.exception.PersistenceExceptionFactory;
import com.comerzzia.unide.api.web.model.customer.DeactivateCustomer;

@Service
@Primary
public class UnideLyCustomersServiceImpl extends LyCustomersServiceImpl implements UnideLyCustomersService {

	private static final String LOYAL_CUSTOMER_CARD_TYPE = "A";
	
	@Autowired
	protected LoyalCustomerContactMapper mapperContact;

	@Override
	public LyCustomerDTO insert(LyCustomerDTO loyalCustomer, IDatosSesion datosSesion)
	        throws ContadorException, CriptoException, VariableException, VariableNotFoundException, ApiException, EtiquetasException {

		String msgNotValidate = validateNewCustomer(loyalCustomer, datosSesion);
		if (StringUtils.isNotBlank(msgNotValidate)) {
			throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, msgNotValidate);

		}
		return super.insert(loyalCustomer, datosSesion);
	}

	private String validateNewCustomer(LyCustomerDTO fidelizado, IDatosSesion datosSesion) {
		String msgResponse = null;
		// Primero validamos documento
		LyCustomerExample exampleDocCustomer = new LyCustomerExample();

		exampleDocCustomer.or().andInstanceUidEqualTo(datosSesion.getUidInstancia()).andVatNumberEqualTo(fidelizado.getVatNumber());
		mapper.selectFromViewByExample(exampleDocCustomer);
		List<LyCustomerDTO> docValidate = null;
		if (StringUtils.isNotBlank(fidelizado.getVatNumber())) {
			docValidate = mapper.selectFromViewByExample(exampleDocCustomer);
		}

		if (docValidate != null && !docValidate.isEmpty()) {
			msgResponse = "El documento indicado ya se encuentra registrado en el sistema";
		}

		if (StringUtils.isBlank(msgResponse)) {

			LoyalCustomerContactExample validateContact = new LoyalCustomerContactExample();

			for (LoyalCustomerContactEntity contacto : fidelizado.getContacts()) {
				if ("EMAIL".equals(contacto.getContactTypeCode())) {
					validateContact.or().andInstanceUidEqualTo(datosSesion.getUidInstancia()).andContactTypeCodeEqualTo("EMAIL").andValueEqualTo(contacto.getValue());
					List<LoyalCustomerContactEntity> validarEmail = mapperContact.selectByExample(validateContact);

					if (validarEmail != null && !validarEmail.isEmpty()) {
						msgResponse = "El E-mail indicado ya se encuentra registrado en el sistema";
						break;
					}

				}
				if ("MOVIL".equals(contacto.getContactTypeCode())) {
					validateContact.or().andInstanceUidEqualTo(datosSesion.getUidInstancia()).andContactTypeCodeEqualTo("MOVIL").andValueEqualTo(contacto.getValue());
					List<LoyalCustomerContactEntity> validarMovil = mapperContact.selectByExample(validateContact);
					if (validarMovil != null && !validarMovil.isEmpty()) {
						msgResponse = "El móvil indicado ya se encuentra registrado en el sistema";
						break;
					}
				}
				if ("TELEFONO1".equals(contacto.getContactTypeCode())) {
					validateContact.or().andInstanceUidEqualTo(datosSesion.getUidInstancia()).andContactTypeCodeEqualTo("TELEFONO1").andValueEqualTo(contacto.getValue());
					List<LoyalCustomerContactEntity> validarMovil = mapperContact.selectByExample(validateContact);
					if (validarMovil != null && !validarMovil.isEmpty()) {
						msgResponse = "El móvil indicado ya se encuentra registrado en el sistema";
						break;
					}
				}
			}
		}

		return msgResponse;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
        public void deactivateLoyalCustomer(DeactivateCustomer deactivateModel, IDatosSesion datosSesion) throws ApiException {
		if(deactivateModel == null) throw new BadRequestException("La peticion es vacía o nula");
		
		LyCustomerEntity loyalCustomer = selectByPrimaryKey(datosSesion, deactivateModel.getLyCustomerId());
		if(loyalCustomer == null) throw new NotFoundException();
		
		try {
			log.debug("deactivateLoyalCustomer() - Dando de baja al fidelizado con ID " + loyalCustomer.getLyCustomerId());

			loyalCustomer.setLastUpdate(new Date());
			loyalCustomer.setDeactivationDate(new Date());

			// Borramos el campo observaciones si viene en la llamada
			if (deactivateModel.getRemarks() != null && deactivateModel.getRemarks()) {
				loyalCustomer.setRemarks(null);
			}

			// Si el fidelizado ya no está activo, borramos el acceso
			if (deactivateModel.getActive() != null && !deactivateModel.getActive()) {
				loyalCustomer.setActive(deactivateModel.getActive());
				try {
					accessService.deactivate(loyalCustomer.getLyCustomerId(), datosSesion);
				}
				catch (NotFoundException ignore) {
				}
			}

			// Borramos las etiquetas que nos indiquen
			for (String tag : deactivateModel.getTags()) {
				EtiquetaBean etiqueta = tagsService.consultar(tag, datosSesion);
				etiqueta.setIdClaseEtiquetaEnlazada(CustomerTagsResource.CLASS_ID);
				etiqueta.setIdObjetoEtiquetaEnlazada(loyalCustomer.getLyCustomerId().toString());
				etiqueta.setUidEtiquetaEnlazada(tag);

				try {
					customerTagsService.deleteTagLink(etiqueta, datosSesion);
				}
				catch (EtiquetasException e) {
					throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "Customer and tag not linked");
				}
			}

			loyalCustomer.setInstanceUid(datosSesion.getUidInstancia());

			mapper.updateByPrimaryKey(loyalCustomer);
			
			LoyalCustomerVersion loyalCustomerVersion = new LoyalCustomerVersion(loyalCustomer.getLyCustomerId());
			fidVersionControlService.checkLoyalCustomersVersion(datosSesion, loyalCustomerVersion);

			List<CardEntity> cardsCustomers = cardsService.selectCustomerCards(datosSesion, loyalCustomer.getLyCustomerId());
			for(CardEntity card:cardsCustomers) {
				if(LOYAL_CUSTOMER_CARD_TYPE.equals(card.getCardTypeCode()) && card.getCardAccountId() != null && card.getDeactivationDate() == null) {
					card.setDeactivationDate(new Date());
					cardsService.updateByPrimaryKey(datosSesion, card);
				}
			}
		}
		catch (PersistenceException e) {

			log.info("deactivateLoyalCustomer() - No se ha podido dar de baja el fidelizado: " + e.getMessage());
			if (PersistenceExceptionFactory.getPersistenceExpception(e).isConstraintViolationException()) {
				throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "El fidelizado contiene un campo erroneo.");
			}
			else if (PersistenceExceptionFactory.getPersistenceExpception(e).isKeyConstraintViolationException()) {
				throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, e.getMessage());
			}
			else {
				throw new ApiException(e.getMessage(), e);
			}
		}
		catch (Exception e) {

			String msg = "Error dando de baja el fidelizado: " + e.getMessage();
			log.error("deactivateLoyalCustomer() - " + msg, e);

			throw new ApiException(msg, e);
                }

        }

        @Override
        @Transactional(rollbackFor = Exception.class)
        public LyCustomerDTO associateCustomer(LyCustomerDTO loyalCustomer, IDatosSesion datosSesion) throws ApiException {
                if (loyalCustomer == null) {
                        throw new BadRequestException("La peticion es vacía o nula");
                }

                if (loyalCustomer.getCards() == null || loyalCustomer.getCards().isEmpty() ||
                                StringUtils.isBlank(loyalCustomer.getCards().get(0).getCardNumber())) {
                        throw new BadRequestException("No se ha indicado la tarjeta de fidelizacion");
                }

                if (loyalCustomer.getAccess() != null &&
                                StringUtils.isNotBlank(loyalCustomer.getAccess().getUser())) {
                        String user = loyalCustomer.getAccess().getUser();
                        user = user.replace("_", "").replace("@", "");
                        loyalCustomer.getAccess().setUser(user);
                }

                try {
                String cardNumber = loyalCustomer.getCards().get(0).getCardNumber();

                com.comerzzia.api.loyalty.persistence.cards.CardExample cardExample =
                                new com.comerzzia.api.loyalty.persistence.cards.CardExample(datosSesion);
                cardExample.or().andNumeroTarjetaEqualTo(cardNumber).andFechaBajaIsNull();

                        List<CardEntity> cards = cardsService.selectByExample(datosSesion, cardExample);
                        if (cards.isEmpty()) {
                                throw new NotFoundException();
                        }

                        CardEntity card = cards.get(0);
                        if (card.getLyCustomerId() == null) {
                                throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE,
                                                "Tarjeta sin fidelizado asociado");
                        }

                        LyCustomerEntity dbCustomer = selectByPrimaryKey(datosSesion, card.getLyCustomerId());
                        if (dbCustomer == null) {
                                throw new NotFoundException();
                        }

                        if (StringUtils.isNotBlank(dbCustomer.getName()) || StringUtils.isNotBlank(dbCustomer.getLastName())) {
                                throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE,
                                                "La tarjeta ya está asociada a un fidelizado");
                        }

                       if (loyalCustomer.getCollectives() == null) {
                               loyalCustomer.setCollectives(new java.util.ArrayList<>());
                        }
                        boolean hasReg = loyalCustomer.getCollectives().stream()
                                        .anyMatch(c -> "REG".equalsIgnoreCase(c.getCollectiveCode()));
                        if (!hasReg) {
                                com.comerzzia.api.loyalty.persistence.customers.collectives.LoyalCustomerCollectiveDTO reg =
                                                new com.comerzzia.api.loyalty.persistence.customers.collectives.LoyalCustomerCollectiveDTO();
                                reg.setCollectiveCode("REG");
                                loyalCustomer.getCollectives().add(reg);
                        }

                        loyalCustomer.setLyCustomerId(dbCustomer.getLyCustomerId());
                        loyalCustomer.setLyCustomerCode(dbCustomer.getLyCustomerCode());

                        mapper.updateByPrimaryKey(loyalCustomer);

                        if (loyalCustomer.getContacts() != null) {
                                for (LoyalCustomerContactEntity contact : loyalCustomer.getContacts()) {
                                        contact.setLoyalCustomerId(loyalCustomer.getLyCustomerId());
                                        contactsService.insert(contact, datosSesion);
                                }
                        }

                        if (loyalCustomer.getCustomerLink() != null) {
                                loyalCustomer.getCustomerLink().setLyCustomerId(loyalCustomer.getLyCustomerId());
                                loyalCustomer.getCustomerLink().setActivityUid(datosSesion.getUidActividad());
                                linksService.insert(loyalCustomer.getCustomerLink(), datosSesion);
                        }

                        if (loyalCustomer.getAccess() != null) {
                                loyalCustomer.getAccess().setLoyalCustomerId(loyalCustomer.getLyCustomerId());
                                accessService.insert(loyalCustomer.getAccess(), datosSesion);
                        }

                        if (loyalCustomer.getCollectives() != null) {
                                for (com.comerzzia.api.loyalty.persistence.customers.collectives.LoyalCustomerCollectiveDTO collective : loyalCustomer.getCollectives()) {
                                        collective.setLoyalCustomerId(loyalCustomer.getLyCustomerId());
                                        collectivesService.insert(collective, datosSesion);
                                }
                        }

                        if (loyalCustomer.getTags() != null) {
                                for (EtiquetaBean tag : loyalCustomer.getTags()) {
                                        tag.setIdClaseEtiquetaEnlazada("F_FIDELIZADOS_TBL.ID_FIDELIZADO");
                                        tag.setIdObjetoEtiquetaEnlazada(loyalCustomer.getLyCustomerId().toString());
                                        customerTagsService.insertTagLink(tag, datosSesion);
                                }
                        }

                        LoyalCustomerVersion loyalCustomerVersion = new LoyalCustomerVersion(loyalCustomer.getLyCustomerId());
                        fidVersionControlService.checkLoyalCustomersVersion(datosSesion, loyalCustomerVersion);

                        return selectDTOByPrimaryKey(loyalCustomer.getLyCustomerId(), datosSesion);
                }
                catch (ApiException e) {
                        throw e;
                }
                catch (PersistenceException e) {
                        if (PersistenceExceptionFactory.getPersistenceExpception(e).isConstraintViolationException()) {
                                throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, e.getMessage());
                        }
                        throw new ApiException(e.getMessage(), e);
                }
                catch (Exception e) {
                        throw new ApiException(e.getMessage(), e);
                }
        }
}
