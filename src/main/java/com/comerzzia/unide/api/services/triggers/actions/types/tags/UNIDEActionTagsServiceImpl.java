package com.comerzzia.unide.api.services.triggers.actions.types.tags;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.loyalty.persistence.accounts.AccountEntity;
import com.comerzzia.api.loyalty.persistence.cards.CardEntity;
import com.comerzzia.api.loyalty.persistence.cards.CardExample;
import com.comerzzia.api.loyalty.persistence.customers.LyCustomerEntity;
import com.comerzzia.api.loyalty.persistence.triggers.actions.TriggerActionBean;
import com.comerzzia.api.loyalty.persistence.triggers.executions.data.TriggerExecutionDataKey;
import com.comerzzia.api.loyalty.service.accounts.AccountsService;
import com.comerzzia.api.loyalty.service.cards.CardsService;
import com.comerzzia.api.loyalty.service.triggers.TriggersService;
import com.comerzzia.api.loyalty.service.triggers.actions.types.tags.ActionTagsServiceImpl;
import com.comerzzia.core.model.etiquetas.categorias.EtiquetaBean;
import com.comerzzia.core.model.etiquetas.enlaces.EtiquetaEnlaceBean;
import com.comerzzia.core.model.etiquetas.enlaces.EtiquetaEnlaceExample;
import com.comerzzia.core.model.etiquetas.enlaces.EtiquetaEnlaceExample.Criteria;
import com.comerzzia.core.servicios.etiquetas.EtiquetasConstraintViolationException;
import com.comerzzia.core.servicios.etiquetas.EtiquetasException;
import com.comerzzia.core.servicios.etiquetas.EtiquetasService;
import com.comerzzia.core.servicios.etiquetas.enlaces.EtiquetasEnlacesConstraintViolationException;
import com.comerzzia.core.servicios.etiquetas.enlaces.EtiquetasEnlacesException;
import com.comerzzia.core.servicios.etiquetas.enlaces.EtiquetasEnlacesService;
import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.core.servicios.variables.VariablesService;
import com.comerzzia.core.util.base.Estado;
import com.comerzzia.unide.api.services.customers.UnideLyCustomersServiceImpl;
import com.comerzzia.unide.api.services.triggers.ui.PushData;
import com.comerzzia.unide.api.services.triggers.ui.UNIDEActionTagsData;

@Component("UNIDEActionTagsService")
@Scope("prototype")
public class UNIDEActionTagsServiceImpl extends ActionTagsServiceImpl {

	private final Logger log = LoggerFactory.getLogger(UNIDEActionTagsServiceImpl.class);

	public static final String ONESIGNAL_APIKEY = "ONESIGNAL.APIKEY";
	public static final String ONESIGNAL_APIURL = "ONESIGNAL.APIURL";
	public static final String ONESIGNAL_IDAPP = "ONESIGNAL.IDAPP";
	protected final String LOYAL_CUSTOMER_CARD_TYPE = "A";

	protected UNIDEActionTagsData data;

	@Autowired
	protected TriggersService triggerService;

	@Autowired
	protected EtiquetasService etiquetasService;

	@Autowired
	protected VariablesService variableService;

	@Autowired
	protected EtiquetasEnlacesService etiquetasEnlacesService;

	@Autowired
	protected UnideLyCustomersServiceImpl unideCustomerService;

	@Autowired
	protected AccountsService accountService;

	@Autowired
	protected CardsService cardService;

	@Override
	public void setTriggerAction(TriggerActionBean triggerAction) {
		super.setTriggerAction(triggerAction);
		data = createDataObject(UNIDEActionTagsData.class);
	}

	public Object getDataObject() {
		return data;
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeAction(IDatosSesion sessionData) throws ApiException {
		log.info("Executing action: " + triggerAction.getActionUid() + " Trigger: " + triggerAction.getTriggerUid());
		if (data == null) {
			throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_INTERNAL, LY_TRIGGER_ACTIION_TYPE_NULL_DATA, new String[] {});
		}
		StringBuilder errors = new StringBuilder();
		String apiUrlOneSignal, apiKeyOneSignal, idAppOneSignal;
		try {
			apiUrlOneSignal = variableService.consultarValor(sessionData, ONESIGNAL_APIURL);
			apiKeyOneSignal = variableService.consultarValor(sessionData, ONESIGNAL_APIKEY);
			idAppOneSignal = variableService.consultarValor(sessionData, ONESIGNAL_IDAPP);
		}
		catch (Exception e) {
			log.error("Error consultando variables de OneSignal", e);
			throw new ApiException("Error obteniendo variables de OneSignal", e);
		}
		// query customers from trigger execution data
		List<TriggerExecutionDataKey> triggerExecutionData = getTriggerExecutionData(sessionData);
		try {
			if (data.isOperationDeassignTags()) {
				deassignTags(triggerExecutionData, sessionData, apiUrlOneSignal, apiKeyOneSignal, idAppOneSignal, errors);
			}
			else if (data.isOperationAssignTags()) {
				assignTags(triggerExecutionData, sessionData, apiUrlOneSignal, apiKeyOneSignal, idAppOneSignal, errors);
			}
		}
		catch (Exception e) {
			throw new ApiException(e.getMessage(), e);
		}
	}

	protected void assignTags(List<TriggerExecutionDataKey> triggerExecutionData, IDatosSesion sessionData, String apiUrlOneSignal, String apiKeyOneSignal, String idAppOneSignal, StringBuilder errors)
	        throws EtiquetasException, EtiquetasConstraintViolationException, EtiquetasEnlacesConstraintViolationException, EtiquetasEnlacesException, ApiException {
		for (String stringTag : data.getTags()) {
			EtiquetaBean tagBean = etiquetasService.consultarEtiquetaEnlazada(sessionData, stringTag);
			if (tagBean.getEtiqueta() == null) {
				String uidTag = UUID.randomUUID().toString();

				tagBean.setEstadoBean(Estado.NUEVO);
				tagBean.setUidEtiqueta(uidTag);
				tagBean.setEtiqueta(stringTag);

				etiquetasService.salvar(tagBean, sessionData);
			}
			for (TriggerExecutionDataKey triggerExecutionDataKey : triggerExecutionData) {

				EtiquetaEnlaceExample example = new EtiquetaEnlaceExample();
				Criteria criteria = example.or();
				criteria.andIdClaseEqualTo("F_FIDELIZADOS_TBL.ID_FIDELIZADO");
				criteria.andIdObjetoEqualTo(triggerExecutionDataKey.getLyCustomerId().toString());

				if (etiquetasEnlacesService.consultar(example).isEmpty()) {
					EtiquetaEnlaceBean etiquetaEnlace = new EtiquetaEnlaceBean();
					etiquetaEnlace.setIdClase("F_FIDELIZADOS_TBL.ID_FIDELIZADO");
					etiquetaEnlace.setEstadoBean(Estado.NUEVO);
					etiquetaEnlace.setUidEtiqueta(tagBean.getUidEtiqueta());
					etiquetaEnlace.setIdObjeto(triggerExecutionDataKey.getLyCustomerId().toString());
					etiquetasEnlacesService.salvar(etiquetaEnlace, sessionData);
				}
				try {
					CardExample cardExample = new CardExample(sessionData);
					cardExample.or().andCodTipoTarjEqualTo(LOYAL_CUSTOMER_CARD_TYPE).andIdFidelizadoEqualTo(triggerExecutionDataKey.getLyCustomerId()).andFechaBajaIsNull();
					List<CardEntity> cards = cardService.selectByExample(sessionData, cardExample);
					if (cards.size() < 1) {
						// The loyal customer doesn't have any eligible card
						continue;
					}
					CardEntity card = cards.get(0);
					LyCustomerEntity customer = unideCustomerService.selectByPrimaryKey(sessionData, triggerExecutionDataKey.getLyCustomerId());
					AccountEntity account = accountService.selectById(customer.getLyCustomerId(), sessionData);

					sendPushNotification(sessionData, apiUrlOneSignal, apiKeyOneSignal, idAppOneSignal, customer, card, account, errors);
				}
				catch (Exception e) {
					log.error("Error al enviar notificación push a OneSignal: " + e.getMessage(), e);
				}
			}
		}
		if (errors.length() > 0) {
			throw new ApiException(errors.toString());
		}
	}

	protected void deassignTags(List<TriggerExecutionDataKey> triggerExecutionData, IDatosSesion sessionData, String apiUrlOneSignal, String apiKeyOneSignal, String idAppOneSignal,
	        StringBuilder errors) throws EtiquetasException, EtiquetasEnlacesConstraintViolationException, EtiquetasEnlacesException, ApiException {
		for (String stringTag : data.getTags()) {
			EtiquetaBean tagBean = etiquetasService.consultarEtiquetaEnlazada(sessionData, stringTag);
			if (tagBean.getEtiqueta() != null) {
				for (TriggerExecutionDataKey triggerExecutionDataKey : triggerExecutionData) {
					EtiquetaEnlaceBean etiquetaEnlace = new EtiquetaEnlaceBean();
					etiquetaEnlace.setUidEtiqueta(tagBean.getUidEtiqueta());
					etiquetaEnlace.setIdClase("F_FIDELIZADOS_TBL.ID_FIDELIZADO");
					etiquetaEnlace.setIdObjeto(triggerExecutionDataKey.getLyCustomerId().toString());
					etiquetasEnlacesService.eliminar(etiquetaEnlace, sessionData);
					try {
						CardExample cardExample = new CardExample(sessionData);
						cardExample.or().andCodTipoTarjEqualTo(LOYAL_CUSTOMER_CARD_TYPE).andIdFidelizadoEqualTo(triggerExecutionDataKey.getLyCustomerId()).andFechaBajaIsNull();
						List<CardEntity> cards = cardService.selectByExample(sessionData, cardExample);
						if (cards.size() < 1) {
							// The loyal customer doesn't have any eligible card
							continue;
						}
						CardEntity card = cards.get(0);
						LyCustomerEntity customer = unideCustomerService.selectByPrimaryKey(sessionData, triggerExecutionDataKey.getLyCustomerId());
						AccountEntity account = accountService.selectById(card.getCardAccountId(), sessionData);

						sendPushNotification(sessionData, apiUrlOneSignal, apiKeyOneSignal, idAppOneSignal, customer, card, account, errors);
					}
					catch (Exception e) {
						log.error("Error al enviar notificación push a OneSignal: " + e.getMessage(), e);
					}
				}
			}
		}
	}

	private void sendPushNotification(IDatosSesion sessionData, String apiUrl, String apiKey, String appId, LyCustomerEntity customer, CardEntity card, AccountEntity account, StringBuilder errors) {
		try {
			PushData pushData = data.getPushData();
			if (pushData == null) {
				return;
			}
			String title = replaceVariables(pushData.getTitleOneSignal(), customer, card, account);
			String subtitle = replaceVariables(pushData.getSubtitleOneSignal(), customer, card, account);
			String message = replaceVariables(pushData.getMessageOneSignal(), customer, card, account);

			if (StringUtils.isBlank(title) || StringUtils.isBlank(message)) {
				return;
			}
			
			JSONObject notification = new JSONObject();
			notification.put("app_id", appId);
			notification.put("target_channel", "push");
			notification.put("include_aliases", new JSONObject().put("external_id", new JSONArray().put(customer.getLyCustomerCode())));

			JSONObject headings = new JSONObject();
			headings.put("en", title);
			notification.put("headings", headings);

			JSONObject subtitleJson = new JSONObject();
			subtitleJson.put("en", subtitle);
			notification.put("subtitle", subtitleJson);

			JSONObject content = new JSONObject();
			content.put("en", message);
			notification.put("contents", content);

			if (pushData.getImageOneSignal() != null) {
				notification.put("big_picture", pushData.getImageOneSignal());
			}
			if (pushData.getLaunchOneSignal() != null) {
				notification.put("url", pushData.getLaunchOneSignal());
			}
			URL urlConnection = new URL(apiUrl);
			HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", "Basic " + apiKey);
			connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
			writer.write(notification.toString());
			writer.close();

			int statusCode = connection.getResponseCode();
			InputStream is = (statusCode >= 200 && statusCode < 400) ? connection.getInputStream() : connection.getErrorStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder jsonString = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				jsonString.append(line);
			}
			// Procesar la respuesta de la API
			JSONObject result = new JSONObject(jsonString.toString());
			log.info(result.toString());
			br.close();
			connection.disconnect();
			StringBuilder customerErrors = new StringBuilder();
			if (statusCode != 200) {
				String connectionError = ": Error en la conexión con One Signal, código de estado: " + statusCode + ", " + result.toString() + "; ";
				if (!errors.toString().contains(connectionError)) {
					errors.append(connectionError);
				}
			}
			else {
				if (result.has("errors")) {
					JSONArray errorsArray = result.getJSONArray("errors");
					for (int i = 0; i < errorsArray.length(); i++) {
						customerErrors.append("Cliente ").append(customer.getLyCustomerId()).append(": ").append(errorsArray.getString(i)).append("; ");
					}
				}
				if (customerErrors.length() > 0) {
					errors.append(customerErrors);
					log.error("Error en la notificación OneSignal para el cliente: " + customer.getLyCustomerId() + " -> " + customerErrors.toString());
				}
				else {
					log.info("Notificación enviada a OneSignal para el usuario: " + customer.getLyCustomerId());
				}
			}

		}
		catch (MalformedURLException e) {
			String malformedUrlError = ": URL de conexión errónea - " + e.getMessage();
			if (!errors.toString().contains(malformedUrlError)) {
				errors.append(malformedUrlError);
			}
			log.error("Error enviando notificación a OneSignal: URL mal formada", e);
		}
		catch (Exception e) {
			String genericError = "Error de conexión con OneSignal: " + e.getMessage();
			if (!errors.toString().contains(genericError)) {
				errors.append(genericError);
			}
			log.error("Error enviando notificación a OneSignal", e);
		}
	}

	private String replaceVariables(String text, LyCustomerEntity customer, CardEntity card, AccountEntity account) {
		if (text == null) {
			return null;
		}

		SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
		// Reemplazo de variables de fidelizado
		text = text.replace("#fidelizados.nombre#", customer.getName() != null ? customer.getName() : "")
		        .replace("#fidelizados.apellidos#", customer.getLastName() != null ? customer.getLastName() : "")
		        .replace("#fidelizados.sexo#", formatGender(customer.getGenderName()) != null ? formatGender(customer.getGenderName()) : "")
		        .replace("#fidelizados.codestcivil#", customer.getMaritalStatusCode() != null ? customer.getMaritalStatusCode() : "")
		        .replace("#fidelizados.fecha_nacimiento#", formatDate(customer.getDateOfBirth(), outputFormat) != null ? formatDate(customer.getDateOfBirth(), outputFormat) : "")
		        .replace("#fidelizados.fecha_alta#", formatDate(customer.getCreationDate(), outputFormat) != null ? formatDate(customer.getCreationDate(), outputFormat) : "")
		        .replace("#fidelizados.fecha_ultimo_login#", formatDate(customer.getLastUpdate(), outputFormat) != null ? formatDate(customer.getLastUpdate(), outputFormat) : "")
		        .replace("#fidelizados.provincia#", customer.getProvince() != null ? customer.getProvince() : "")
		        .replace("#fidelizados.domicilio#", customer.getAddress() != null ? customer.getAddress() : "")
		        .replace("#fidelizados.localidad#", customer.getCity() != null ? customer.getCity() : "");

		// Reemplazo de variables de tarjeta
		if (card != null) {
			text = text.replace("#tarjeta.fecha_activacion#", formatDate(card.getActivationDate(), outputFormat) != null ? formatDate(card.getActivationDate(), outputFormat) : "")
			        .replace("#tarjeta.fecha_ultimo_uso#", formatDate(card.getLastUseDate(), outputFormat) != null ? formatDate(card.getLastUseDate(), outputFormat) : "");
		}
		else {
			text = text.replace("#tarjeta.fecha_activacion#", "");
		}

		// Reemplazo de variables de cuenta
		if (account != null) {
			text = text.replace("#cuentas.saldo#", String.valueOf(account.getBalance() != null ? account.getBalance() : 0));
		}
		else {
			text = text.replace("#cuentas.saldo#", "");
		}

		Pattern pattern = Pattern.compile("#([^#]+)#");
		Matcher matcher = pattern.matcher(text);
		String[] validVariables = { "fidelizados.nombre", "fidelizados.apellidos", "fidelizados.sexo", "fidelizados.codestcivil", "fidelizados.fecha_nacimiento", "fidelizados.fecha_alta",
		        "fidelizados.fecha_ultimo_login", "fidelizados.provincia", "fidelizados.domicilio", "fidelizados.localidad", "tarjeta.fecha_activacion", "tarjeta.fecha_ultimo_uso", "cuentas.saldo" };

		while (matcher.find()) {
			String foundVariable = matcher.group(1);
			boolean isValid = false;
			for (String validVariable : validVariables) {
				if (validVariable.equals(foundVariable)) {
					isValid = true;
					break;
				}
			}
			if (!isValid) {
				text = text.replace("#" + foundVariable + "#", "");
			}
		}

		return text;
	}

	private String formatDate(Date date, SimpleDateFormat outputFormat) {
		if (date == null)
			return "";
		return outputFormat.format(date);
	}

	private String formatGender(String gender) {
		if ("H".equalsIgnoreCase(gender)) {
			return "Hombre";
		}
		else if ("M".equalsIgnoreCase(gender)) {
			return "Mujer";
		}
		return (gender != null) ? gender : "";
	}

}
