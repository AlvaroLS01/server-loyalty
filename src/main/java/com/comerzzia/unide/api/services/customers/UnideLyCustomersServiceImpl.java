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
import com.comerzzia.unide.api.web.model.customer.AssociateCustomerRequest;

@Service
@Primary
public class UnideLyCustomersServiceImpl extends LyCustomersServiceImpl implements UnideLyCustomersService {

    private static final String LOYAL_CUSTOMER_CARD_TYPE = "A";
    public static final String COD_COLECTIVO_REGISTRO = "X_FIDELIZADOS.COD_COLECTIVO_REGISTRO";
	
	@Autowired
	protected LoyalCustomerContactMapper mapperContact;
	
    @Autowired
    private CollectiveMapper collectiveMapper;

    @Autowired
    private NewCustomerToFidelizadoConverter newCustomerToFidelizadoConverter;

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
                                                msgResponse = "USER/EMAIL Alredy exist";
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
                        log.info("deactivateLoyalCustomer() - Dando de baja al fidelizado con ID " + loyalCustomer.getLyCustomerId());

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
        public LyCustomerDTO associateCustomer(AssociateCustomerRequest record, IDatosSesion datosSesion) throws ApiException {
                LyCustomerDTO dto = newCustomerToFidelizadoConverter.convert(record);
                List<CardEntity> cards = new ArrayList<>();
                if (record.getCards() != null) {
                        for (AssociateCustomerRequest.Card c : record.getCards()) {
                                CardEntity card = new CardEntity();
                                card.setCardNumber(c.getCardNumber());
                                cards.add(card);
                        }
                }
                dto.setCards(cards);
                return associateCustomer(dto, datosSesion);
        }

		@Override
                @Transactional(rollbackFor = Exception.class)
                public LyCustomerDTO associateCustomer(LyCustomerDTO fidelizado, IDatosSesion datosSesion) throws ApiException {
                        validarPeticion(fidelizado);
                        String numeroTarjeta = fidelizado.getCards().get(0).getCardNumber();
                        Long idAnonimo = obtenerIdAnonimo(numeroTarjeta, datosSesion);
                        LyCustomerDTO clienteAnonimo = selectDTOByPrimaryKey(idAnonimo, datosSesion);
                        comprobarClienteAnonimo(clienteAnonimo);
                        validarDuplicados(fidelizado, datosSesion, idAnonimo);
                        asignarColectivoRegistro(fidelizado, datosSesion);
                        normalizarUsuarioAcceso(fidelizado);
                        actualizarDatosPrincipales(fidelizado, clienteAnonimo, datosSesion);
                        reemplazarContactos(fidelizado, idAnonimo, datosSesion);
                        actualizarInfoComplementaria(fidelizado, idAnonimo, datosSesion);

                        fidVersionControlService.checkLoyalCustomersVersion(datosSesion, new LoyalCustomerVersion(idAnonimo));
                        log.info("associateCustomer - finalizado para cliente " + idAnonimo);

                        return fidelizado;
                }

                private void validarPeticion(LyCustomerDTO fidelizado) {
                        if (fidelizado == null || fidelizado.getCards() == null || fidelizado.getCards().isEmpty() || StringUtils.isBlank(fidelizado.getCards().get(0).getCardNumber())) {
                                log.info("associateCustomer - petición inválida: falta número de tarjeta");
                                throw new BadRequestException("El número de tarjeta es obligatorio");
                        }
                }

                private Long obtenerIdAnonimo(String numeroTarjeta, IDatosSesion datosSesion) throws ApiException {
                        log.info("associateCustomer - iniciando para tarjeta " + numeroTarjeta);
                        try {
                                CardDTO tarjetaDto = cardsService.selectByCardNumber(numeroTarjeta, datosSesion);
                                Long idAnonimo = tarjetaDto.getLyCustomer() != null ? tarjetaDto.getLyCustomer().getLyCustomerId() : null;
                                if (idAnonimo == null) {
                                        log.info("associateCustomer - tarjeta " + numeroTarjeta + " sin cliente asociado");
                                        throw new NotFoundException();
                                }
                                return idAnonimo;
                        } catch (NotFoundException e) {
                                log.info("associateCustomer - tarjeta no encontrada: " + numeroTarjeta);
                                throw new NotFoundException();
                        }
                }

                private void comprobarClienteAnonimo(LyCustomerDTO clienteAnonimo) throws ApiException {
                        if (StringUtils.isNotBlank(clienteAnonimo.getVatNumber())) {
                                log.info("associateCustomer - la tarjeta ya tiene asignado el documento " + clienteAnonimo.getVatNumber());
                                throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "Esta tarjeta ya tiene un fidelizado con datos asociado");
                        }
                        log.info("associateCustomer - fidelizado anónimo localizado: id " + clienteAnonimo.getLyCustomerId());
                }

                private void validarDuplicados(LyCustomerDTO fidelizado, IDatosSesion datosSesion, Long idAnonimo) throws ApiException {
                        if (StringUtils.isNotBlank(fidelizado.getVatNumber())) {
                                LyCustomerExample ejemploDni = new LyCustomerExample();
                                ejemploDni.or().andInstanceUidEqualTo(datosSesion.getUidInstancia()).andVatNumberEqualTo(fidelizado.getVatNumber()).andLyCustomerIdNotEqualTo(idAnonimo);
                                List<LyCustomerDTO> clientesConDni = mapper.selectFromViewByExample(ejemploDni);
                                if (!clientesConDni.isEmpty()) {
                                        log.info("associateCustomer - VAT duplicado " + fidelizado.getVatNumber() + " para cliente " + idAnonimo);
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
                                                log.info("associateCustomer - " + tipo + " duplicado " + contactoNuevo.getValue() + " para cliente " + idAnonimo);
                                                if ("EMAIL".equals(contactoNuevo.getContactTypeCode())) {
                                                        throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "USER/EMAIL Alredy exist");
                                                }
                                                throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "El " + tipo + " indicado ya se encuentra registrado en el sistema");
                                        }
                                }
                        }
                }

                private void asignarColectivoRegistro(LyCustomerDTO fidelizado, IDatosSesion datosSesion) {
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
                                        log.info("associateCustomer - colectivo REG añadido: " + codigoColectivo);
                                }
                        } catch (VariableException | VariableNotFoundException ignored) {
                                log.info("associateCustomer - variable COD_COLECTIVO_REGISTRO no encontrada");
                        }
                }

                private void normalizarUsuarioAcceso(LyCustomerDTO fidelizado) {
                        if (fidelizado.getAccess() != null && StringUtils.isNotBlank(fidelizado.getAccess().getUser())) {
                                String usuarioBruto = fidelizado.getAccess().getUser();

                                // Limpiar solo el nombre de usuario
                                String usuarioLimpio = usuarioBruto.replace("_", "").replace("@", "");
                                fidelizado.getAccess().setUser(usuarioLimpio);

                                // Mantener la dirección de correo intacta si coincide con el usuario original
                                if (fidelizado.getContacts() != null) {
                                        for (LoyalCustomerContactEntity c : fidelizado.getContacts()) {
                                                if ("EMAIL".equals(c.getContactTypeCode()) && usuarioBruto.equals(c.getValue())) {
                                                        c.setValue(usuarioBruto);
                                                }
                                        }
                                }

                                log.info("associateCustomer - usuario limpiado de '" + usuarioBruto + "' a '" + usuarioLimpio + "'");
                        }
                }

                private void actualizarDatosPrincipales(LyCustomerDTO fidelizado, LyCustomerDTO clienteAnonimo, IDatosSesion datosSesion) throws ApiException {
                        fidelizado.setLyCustomerId(clienteAnonimo.getLyCustomerId());
                        fidelizado.setLyCustomerCode(clienteAnonimo.getLyCustomerCode());
                        log.info("associateCustomer - asignando datos al cliente " + clienteAnonimo.getLyCustomerId());
                        super.update(modelMapper.map(fidelizado, LyCustomerEntity.class), datosSesion);
                        log.info("associateCustomer - datos principales actualizados para cliente " + clienteAnonimo.getLyCustomerId());
                }

                private void reemplazarContactos(LyCustomerDTO fidelizado, Long idAnonimo, IDatosSesion datosSesion) throws ApiException {
                        LoyalCustomerContactExample ejemploBorrar = new LoyalCustomerContactExample();
                        ejemploBorrar.or().andInstanceUidEqualTo(datosSesion.getUidInstancia()).andLoyalCustomerIdEqualTo(idAnonimo);
                        mapperContact.deleteByExample(ejemploBorrar);
                        log.info("associateCustomer - contactos antiguos borrados para cliente " + idAnonimo);

                        if (!CollectionUtils.isEmpty(fidelizado.getContacts())) {
                                for (LoyalCustomerContactEntity contactoInsertar : fidelizado.getContacts()) {
                                        contactoInsertar.setInstanceUid(datosSesion.getUidInstancia());
                                        contactoInsertar.setLoyalCustomerId(idAnonimo);
                                        contactsService.insert(contactoInsertar, datosSesion);
                                }
                                log.info("associateCustomer - " + fidelizado.getContacts().size() + " contactos nuevos insertados para cliente " + idAnonimo);
                        }
                }

                private void actualizarInfoComplementaria(LyCustomerDTO fidelizado, Long idAnonimo, IDatosSesion datosSesion) throws ApiException {
                        if (fidelizado.getAccess() != null) {
                                LoyalCustomerAccessEntityDTO acceso = fidelizado.getAccess();
                                acceso.setLoyalCustomerId(idAnonimo);
                                try {
                                        accessService.selectByLoyalCustomer(datosSesion, idAnonimo);
                                        accessService.updateCustomerAccessData(datosSesion, acceso);
                                        log.info("associateCustomer - datos de acceso actualizados para cliente " + idAnonimo);
                                } catch (NotFoundException nf) {
                                        accessService.insert(acceso, datosSesion);
                                        log.info("associateCustomer - acceso insertado para cliente " + idAnonimo);
                                }
                        }

                        if (!CollectionUtils.isEmpty(fidelizado.getCollectives())) {
                                for (LoyalCustomerCollectiveDTO colec : fidelizado.getCollectives()) {
                                        colec.setLoyalCustomerId(idAnonimo);
                                        collectivesService.insert(colec, datosSesion);
                                }
                                log.info("associateCustomer - " + fidelizado.getCollectives().size() + " colectivos insertados para cliente " + idAnonimo);
                        }

                        if (!CollectionUtils.isEmpty(fidelizado.getTags())) {
                                for (EtiquetaBean etiqueta : fidelizado.getTags()) {
                                        etiqueta.setIdObjetoEtiquetaEnlazada(idAnonimo.toString());
                                        etiqueta.setIdClaseEtiquetaEnlazada(CustomerTagsResource.CLASS_ID);
                                        customerTagsService.insertTagLink(etiqueta, datosSesion);
                                }
                                log.info("associateCustomer - " + fidelizado.getTags().size() + " etiquetas insertadas para cliente " + idAnonimo);
                        }
                }	}