package com.comerzzia.unide.api.services.triggers.actions.types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.loyalty.persistence.triggers.actions.TriggerActionBean;
import com.comerzzia.api.loyalty.service.triggers.actions.types.ActionTypeAbstractService;
import com.comerzzia.api.loyalty.service.triggers.actions.types.TriggerActionsTypeFactoryImpl;
import com.comerzzia.core.servicios.ContextHolder;

@Service
@Primary
public class UNIDETriggerActionsTypeFactoryImpl extends TriggerActionsTypeFactoryImpl {

	private final Logger log = LoggerFactory.getLogger(UNIDETriggerActionsTypeFactoryImpl.class);

	@Override
	public ActionTypeAbstractService getActionImplementation(TriggerActionBean triggerAction) throws ApiException {
		ActionTypeAbstractService service;
		try {
			service = getService(triggerAction);
			service.setTriggerAction(triggerAction);
		}
		catch (ClassNotFoundException e) {
			throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_INTERNAL, LY_TRIGGER_ACTION_CLASS_NOT_FOUND, new String[] { triggerAction.getActionType().toString(), e.getMessage() });
		}

		return service;
	}

	// method separation for custom factories inheritance
	protected ActionTypeAbstractService getService(TriggerActionBean triggerAction) throws ClassNotFoundException {
		switch (triggerAction.getActionType()) {
			case 1:
				if (ContextHolder.getBean("UNIDEActionCouponsService") != null) {
					log.debug("Bean 'UNIDEActionCouponsService' encontrado y asignado.");
					ActionTypeAbstractService action = (ActionTypeAbstractService) ContextHolder.getBean("UNIDEActionCouponsService");
					action.setTriggerAction(triggerAction);
					return action;
				}
				else {
					log.error("Bean 'UNIDEActionCouponsService' no encontrado en el contexto de Spring.");
					return (ActionTypeAbstractService) ContextHolder.getBean("ActionCouponsService");
				}
			case 2:
				if (ContextHolder.getBean("UNIDEActionPointsService") != null) {
					log.debug("Bean 'UNIDEActionCouponsService' encontrado y asignado.");
					ActionTypeAbstractService action = (ActionTypeAbstractService) ContextHolder.getBean("UNIDEActionPointsService");
					action.setTriggerAction(triggerAction);
					return action;
				}
				else {
					log.error("Bean 'UNIDEActionPointsService' no encontrado en el contexto de Spring.");
					return (ActionTypeAbstractService) ContextHolder.getBean("ActionPointsService");
				}
			case 3:
				if (ContextHolder.getBean("UNIDEActionTagsService") != null) {
					log.debug("Bean 'UNIDEActionTagsService' encontrado y asignado.");
					ActionTypeAbstractService action = (ActionTypeAbstractService) ContextHolder.getBean("UNIDEActionTagsService");
					action.setTriggerAction(triggerAction);
					return action;
				}
				else {
					log.error("Bean 'UNIDEActionTagsService' no encontrado en el contexto de Spring.");
					return (ActionTypeAbstractService) ContextHolder.getBean("ActionTagsService");
				}
			case 4:
				if (ContextHolder.getBean("UNIDEActionPushService") != null) {
					log.debug("Bean 'UNIDEActionPushService' encontrado y asignado.");
					ActionTypeAbstractService action = (ActionTypeAbstractService) ContextHolder.getBean("UNIDEActionPushService");
					action.setTriggerAction(triggerAction);
					return action;
				}
				else {
					log.error("Bean 'UNIDEActionPushService' no encontrado en el contexto de Spring.");
					return null;
				}
			default:
				return null;
		}
	}
}
