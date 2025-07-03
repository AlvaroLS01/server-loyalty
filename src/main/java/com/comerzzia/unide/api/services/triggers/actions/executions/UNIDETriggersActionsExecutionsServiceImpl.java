package com.comerzzia.unide.api.services.triggers.actions.executions;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.loyalty.persistence.triggers.actions.TriggerActionBean;
import com.comerzzia.api.loyalty.persistence.triggers.actions.executions.TriggerActionExecutionBean;
import com.comerzzia.api.loyalty.persistence.triggers.actions.executions.TriggerActionExecutionExample;
import com.comerzzia.api.loyalty.persistence.triggers.actions.executions.TriggerActionExecutionKey;
import com.comerzzia.api.loyalty.persistence.triggers.actions.executions.TriggerActionExecutionMapper;
import com.comerzzia.api.loyalty.persistence.triggers.executions.TriggerExecutionBean;
import com.comerzzia.api.loyalty.service.triggers.actions.TriggersActionsService;
import com.comerzzia.api.loyalty.service.triggers.actions.executions.TriggersActionsExecutionsServiceImpl;
import com.comerzzia.api.loyalty.service.triggers.actions.types.TriggerActionsTypeFactory;
import com.comerzzia.api.loyalty.service.triggers.executions.TriggersExecutionsService;
import com.comerzzia.core.servicios.sesion.IDatosSesion;

@Service
@Primary
public class UNIDETriggersActionsExecutionsServiceImpl extends TriggersActionsExecutionsServiceImpl {

	@Autowired
	protected MessageSourceAccessor messageSourceAccessor;

	@Autowired
	protected TriggerActionExecutionMapper mapper;

	@Autowired
	protected TriggersExecutionsService triggerExecutionsService;

	@Autowired
	protected TriggersActionsService triggersActionsService;

	@Autowired
	protected TriggerActionsTypeFactory triggerActionsTypeFactory;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TriggerActionExecutionBean insert(IDatosSesion datosSesion, TriggerActionExecutionBean newRecord) throws ApiException {
		newRecord.setUidActividad(datosSesion.getUidActividad());
		if (newRecord.getAccExecutionUid() == null) {
			newRecord.setAccExecutionUid(UUID.randomUUID().toString());
		}

		mapper.insert(newRecord);

		return newRecord;
	}

	@Override
	public TriggerActionExecutionBean selectByPrimaryKey(IDatosSesion datosSesion, String accExecutionUid) {
		return mapper.selectByPrimaryKey(new TriggerActionExecutionKey(datosSesion.getUidActividad(), accExecutionUid));
	}

	@Override
	public List<TriggerActionExecutionBean> selectByExample(TriggerActionExecutionExample example) {
		return mapper.selectByExample(example);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int delete(IDatosSesion datosSesion, String accExecutionUid) {
		return mapper.deleteByPrimaryKey(new TriggerActionExecutionKey(datosSesion.getUidActividad(), accExecutionUid));
	}

	@Override
	public int updateByPrimaryKey(IDatosSesion datosSesion, TriggerActionExecutionBean record) {
		record.setUidActividad(datosSesion.getUidActividad());
		return mapper.updateByPrimaryKey(record);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int deleteByActionUid(IDatosSesion datosSesion, String actionUid) {
		TriggerActionExecutionExample example = new TriggerActionExecutionExample(datosSesion);

		example.or().andActionUidEqualTo(actionUid);
		return mapper.deleteByExample(example);
	}

	@Override
	public void executeTriggerAction(IDatosSesion datosSesion, String triggerUid, String actionUid, String triggerExecUid, String newAccExecutionUid) throws ApiException {
		TriggerExecutionBean triggerExecution;

		// execute trigger if the execution uid is null
		if (triggerExecUid == null) {
			triggerExecution = triggerExecutionsService.executeTrigger(datosSesion, triggerUid, null);
			triggerExecUid = triggerExecution.getTriggerExecUid();
		}
		else {
			triggerExecution = triggerExecutionsService.selectByPrimaryKey(datosSesion, triggerExecUid);
		}

		// get trigger action
		TriggerActionBean triggerAction = triggersActionsService.selectByPrimaryKey(datosSesion, actionUid);

		// execute action begin
		TriggerActionExecutionBean triggerActionExecution = new TriggerActionExecutionBean();
		triggerActionExecution.setAccExecutionUid(newAccExecutionUid);
		triggerActionExecution.setActionUid(actionUid);
		triggerActionExecution.setStartDate(new Date());
		triggerActionExecution.setManualExecution(false);
		triggerActionExecution.setTriggerExecUid(triggerExecUid);
		triggerActionExecution.setStatusId(0L);
		triggerActionExecution.setUserId(datosSesion.getUserId());
		triggerActionExecution.setStatusText(messageSourceAccessor.getMessage(LY_TRIGGER_ACTION_EXECUTING));

		triggerActionExecution = insert(datosSesion, triggerActionExecution);

		// update trigger execution last action execution
		triggerExecution.setLastAccExecutionUid(triggerActionExecution.getAccExecutionUid());
		triggerExecutionsService.updateByPrimaryKey(datosSesion, triggerExecution);

		// update trigger last execution
		triggerAction.setLastActionExecUid(triggerActionExecution.getAccExecutionUid());
		triggersActionsService.updateByPrimaryKey(datosSesion, triggerAction);

		// execute action logic
		try {
			// call action type implementation
			triggerActionsTypeFactory.getActionImplementation(triggerAction).executeAction(datosSesion);

			triggerActionExecution.setEndDate(new Date());
			triggerActionExecution.setStatusId(100L);
			triggerActionExecution.setStatusText(messageSourceAccessor.getMessage(LY_TRIGGER_ACTION_ENDED));
		}
		catch (Exception e) {
			triggerActionExecution.setEndDate(new Date());
			String errorMessage = e.getMessage();
			if (errorMessage != null && errorMessage.contains("Cliente ")) {
				triggerActionExecution.setStatusId(0L);
			}
			else if (errorMessage != null && errorMessage.contains("conexión")) {
				triggerActionExecution.setStatusId(200L);
			}
			else {
				triggerActionExecution.setStatusId(200L);
			}
			triggerActionExecution.setStatusText(StringUtils.left("Error: " + e.getMessage(), 2000));
		}

		// update trigger action execution end
		updateByPrimaryKey(datosSesion, triggerActionExecution);
	}

}
