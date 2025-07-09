package com.comerzzia.unide.api.services.customers;

import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.util.CollectionUtils;
import java.util.Collections;
import com.comerzzia.api.core.service.exception.NotFoundException;
import com.comerzzia.api.loyalty.persistence.cards.CardDTO;
import com.comerzzia.api.loyalty.persistence.cards.CardEntity;
import com.comerzzia.api.loyalty.persistence.cards.CardUK;
import com.comerzzia.api.loyalty.persistence.collectives.CollectiveKey;
import com.comerzzia.api.loyalty.persistence.collectives.CollectiveMapper;
import com.comerzzia.api.loyalty.persistence.customers.LyCustomerDTO;
import com.comerzzia.api.loyalty.persistence.customers.LyCustomerEntity;
import com.comerzzia.api.loyalty.persistence.customers.LyCustomerExample;
import com.comerzzia.api.loyalty.persistence.customers.access.LoyalCustomerAccessDTO;
import com.comerzzia.api.loyalty.persistence.customers.access.LoyalCustomerAccessEntityDTO;
import com.comerzzia.api.loyalty.persistence.customers.collectives.LoyalCustomerCollectiveDTO;
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
    public static final String COD_COLECTIVO_REGISTRO = "X_FIDELIZADOS.COD_COLECTIVO_REGISTRO";
	
	@Autowired
	protected LoyalCustomerContactMapper mapperContact;
	
    @Autowired
    private CollectiveMapper collectiveMapper;

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
		public LyCustomerDTO associateCustomer(LyCustomerDTO fidelizado, IDatosSesion datosSesion) throws ApiException {
			if (fidelizado == null || fidelizado.getCards() == null || fidelizado.getCards().isEmpty() || StringUtils.isBlank(fidelizado.getCards().get(0).getCardNumber())) {
				log.debug("associateCustomer - petición inválida: falta número de tarjeta");
				throw new BadRequestException("El número de tarjeta es obligatorio");
			}
			String numeroTarjeta = fidelizado.getCards().get(0).getCardNumber();
			log.debug("associateCustomer - iniciando para tarjeta " + numeroTarjeta);

			CardDTO tarjetaDto;
			try {
				tarjetaDto = cardsService.selectByCardNumber(numeroTarjeta, datosSesion);
			}
			catch (NotFoundException e) {
				log.debug("associateCustomer - tarjeta no encontrada: " + numeroTarjeta);
				throw new NotFoundException();
			}
			Long idAnonimo = tarjetaDto.getLyCustomer() != null ? tarjetaDto.getLyCustomer().getLyCustomerId() : null;
			if (idAnonimo == null) {
				log.debug("associateCustomer - tarjeta " + numeroTarjeta + " sin cliente asociado");
				throw new NotFoundException();
			}
			log.debug("associateCustomer - cliente anónimo encontrado: id " + idAnonimo);

			// Verificar que aún no tiene dni o documento
			LyCustomerDTO clienteAnonimo = selectDTOByPrimaryKey(idAnonimo, datosSesion);
			if (StringUtils.isNotBlank(clienteAnonimo.getVatNumber())) {
				log.debug("associateCustomer - cliente " + idAnonimo + " ya tiene documento " + clienteAnonimo.getVatNumber());
				throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "Esta tarjeta ya tiene un fidelizado con datos asociado");
			}

			if (StringUtils.isNotBlank(fidelizado.getVatNumber())) {
				LyCustomerExample ejemploDni = new LyCustomerExample();
				ejemploDni.or().andInstanceUidEqualTo(datosSesion.getUidInstancia()).andVatNumberEqualTo(fidelizado.getVatNumber()).andLyCustomerIdNotEqualTo(idAnonimo);
				List<LyCustomerDTO> clientesConDni = mapper.selectFromViewByExample(ejemploDni);
				if (!clientesConDni.isEmpty()) {
					log.debug("associateCustomer - VAT duplicado " + fidelizado.getVatNumber() + " para cliente " + idAnonimo);
					throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "El documento indicado ya se encuentra registrado en el sistema");
				}
			}

			if (!CollectionUtils.isEmpty(fidelizado.getContacts())) {
				for (LoyalCustomerContactEntity contactoNuevo : fidelizado.getContacts()) {
					LoyalCustomerContactExample ejemploContacto = new LoyalCustomerContactExample();
					ejemploContacto.or().andInstanceUidEqualTo(datosSesion.getUidInstancia()).andContactTypeCodeEqualTo(contactoNuevo.getContactTypeCode()).andValueEqualTo(contactoNuevo.getValue())
					        .andLoyalCustomerIdNotEqualTo(idAnonimo);
					List<LoyalCustomerContactEntity> contactosExistentes = mapperContact.selectByExample(ejemploContacto);
					if (!contactosExistentes.isEmpty()) {
						String tipo = "EMAIL".equals(contactoNuevo.getContactTypeCode()) ? "E-mail" : contactoNuevo.getContactTypeCode().startsWith("MOVIL") ? "Móvil" : "Teléfono";
						log.debug("associateCustomer - " + tipo + " duplicado " + contactoNuevo.getValue() + " para cliente " + idAnonimo);
						throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "El " + tipo + " indicado ya se encuentra registrado en el sistema");
					}
				}
			}

			// Añadir colectivo REG automáticamente
			try {
				String codigoColectivo = variablesService.consultarValor(datosSesion, COD_COLECTIVO_REGISTRO);
				if (StringUtils.isNotBlank(codigoColectivo)) {
					CollectiveKey claveColectivo = new CollectiveKey();
					claveColectivo.setUidInstancia(datosSesion.getUidInstancia());
					claveColectivo.setCodColectivo(codigoColectivo);
					com.comerzzia.api.loyalty.persistence.collectives.Collective maestro = collectiveMapper.selectByPrimaryKey(claveColectivo);
					LoyalCustomerCollectiveDTO colectivoRegistro = new LoyalCustomerCollectiveDTO();
					colectivoRegistro.setCollectiveCode(codigoColectivo);
					colectivoRegistro.setCollectiveDes(maestro != null ? maestro.getDesColectivo() : null);
					fidelizado.setCollectives(Collections.singletonList(colectivoRegistro));
					log.debug("associateCustomer - colectivo REG añadido: " + codigoColectivo);
				}
			}
			catch (VariableException | VariableNotFoundException ignored) {
				log.debug("associateCustomer - variable COD_COLECTIVO_REGISTRO no encontrada");
			}

			if (fidelizado.getAccess() != null && StringUtils.isNotBlank(fidelizado.getAccess().getUser())) {
				String usuarioBruto = fidelizado.getAccess().getUser();
				String usuarioLimpio = usuarioBruto.replace("_", "").replace("@", "");
				fidelizado.getAccess().setUser(usuarioLimpio);
				log.debug("associateCustomer - usuario limpiado de '" + usuarioBruto + "' a '" + usuarioLimpio + "'");
			}

			fidelizado.setLyCustomerId(clienteAnonimo.getLyCustomerId());
			fidelizado.setLyCustomerCode(clienteAnonimo.getLyCustomerCode());
			log.debug("associateCustomer - asignando datos al cliente " + idAnonimo);

			super.update(modelMapper.map(fidelizado, LyCustomerEntity.class), datosSesion);
			log.debug("associateCustomer - datos principales actualizados para cliente " + idAnonimo);

			CardUK claveTarjeta = new CardUK(datosSesion, numeroTarjeta);
			CardEntity entidadTarjeta;
			try {
				entidadTarjeta = cardsService.selectByUniqueKey(datosSesion, claveTarjeta);
			}
			catch (NotFoundException e) {
				log.debug("associateCustomer - tarjeta " + numeroTarjeta + " no encontrada en clave única");
				throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "No se encontró la tarjeta para asignar");
			}
			entidadTarjeta.setLyCustomerId(idAnonimo);
			cardsService.updateByPrimaryKey(datosSesion, entidadTarjeta);
			log.debug("associateCustomer - tarjeta " + numeroTarjeta + " asignada a cliente " + idAnonimo);

			LoyalCustomerContactExample ejemploBorrar = new LoyalCustomerContactExample();
			ejemploBorrar.or().andInstanceUidEqualTo(datosSesion.getUidInstancia()).andLoyalCustomerIdEqualTo(idAnonimo);
			mapperContact.deleteByExample(ejemploBorrar);
			log.debug("associateCustomer - contactos antiguos borrados para cliente " + idAnonimo);

			if (!CollectionUtils.isEmpty(fidelizado.getContacts())) {
				for (LoyalCustomerContactEntity contactoInsertar : fidelizado.getContacts()) {
					contactoInsertar.setInstanceUid(datosSesion.getUidInstancia());
					contactoInsertar.setLoyalCustomerId(idAnonimo);
					contactsService.insert(contactoInsertar, datosSesion);
				}
				log.debug("associateCustomer - " + fidelizado.getContacts().size() + " contactos nuevos insertados para cliente " + idAnonimo);
			}

			if (fidelizado.getAccess() != null) {
				LoyalCustomerAccessEntityDTO acceso = fidelizado.getAccess();
				acceso.setLoyalCustomerId(idAnonimo);
				try {
					accessService.selectByLoyalCustomer(datosSesion, idAnonimo);
					accessService.updateCustomerAccessData(datosSesion, acceso);
					log.debug("associateCustomer - datos de acceso actualizados para cliente " + idAnonimo);
				}
				catch (NotFoundException nf) {
					accessService.insert(acceso, datosSesion);
					log.debug("associateCustomer - acceso insertado para cliente " + idAnonimo);
				}
			}

			if (!CollectionUtils.isEmpty(fidelizado.getCollectives())) {
				for (LoyalCustomerCollectiveDTO colec : fidelizado.getCollectives()) {
					colec.setLoyalCustomerId(idAnonimo);
					collectivesService.insert(colec, datosSesion);
				}
				log.debug("associateCustomer - " + fidelizado.getCollectives().size() + " colectivos insertados para cliente " + idAnonimo);
			}

			if (!CollectionUtils.isEmpty(fidelizado.getTags())) {
				for (EtiquetaBean etiqueta : fidelizado.getTags()) {
					etiqueta.setIdObjetoEtiquetaEnlazada(idAnonimo.toString());
					etiqueta.setIdClaseEtiquetaEnlazada(CustomerTagsResource.CLASS_ID);
					customerTagsService.insertTagLink(etiqueta, datosSesion);
				}
				log.debug("associateCustomer - " + fidelizado.getTags().size() + " etiquetas insertadas para cliente " + idAnonimo);
			}

			fidVersionControlService.checkLoyalCustomersVersion(datosSesion, new LoyalCustomerVersion(idAnonimo));
			log.debug("associateCustomer - finalizado para cliente " + idAnonimo);

			return fidelizado;
		}
	}