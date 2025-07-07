package com.comerzzia.unide.api.services.customers.access;

import java.util.Date;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.core.service.exception.NotFoundException;
import com.comerzzia.api.loyalty.persistence.customers.access.LoyalCustomerAccessDTO;
import com.comerzzia.api.loyalty.persistence.customers.access.LoyalCustomerAccessEntity;
import com.comerzzia.api.loyalty.persistence.customers.access.LoyalCustomerAccessExample;
import com.comerzzia.api.loyalty.persistence.customers.access.LoyalCustomerAccessExample.Criteria;
import com.comerzzia.api.loyalty.service.customers.access.LoyalCustomerAccessServiceImpl;
import com.comerzzia.core.servicios.sesion.IDatosSesion;


@Service
@Primary
public class UnideLoyalCustomerAccessServiceImpl extends LoyalCustomerAccessServiceImpl{
	
	@Override
	public void updateCustomerAccessPassword(IDatosSesion datosSesion, LoyalCustomerAccessDTO previousLoyalCustomer) throws ApiException {
		log.debug("updateCustomerAccessPassword() - Actualizando la contraseña de acceso del fidelizado: " + previousLoyalCustomer.getLoyalCustomerId());
		
		LoyalCustomerAccessExample example = new LoyalCustomerAccessExample();
		Criteria criteria = example.or();
		criteria.andInstanceUidEqualTo(datosSesion.getUidInstancia())
					.andLoyalCustomerIdEqualTo(previousLoyalCustomer.getLoyalCustomerId());
		
		List<LoyalCustomerAccessEntity> lstLoyalCustomerAccess = mapper.selectByExample(example);

		if(lstLoyalCustomerAccess.isEmpty()){
			log.warn("No se encontró el acceso del fidelizado: " + previousLoyalCustomer.getLoyalCustomerId());
			throw new NotFoundException();
		}
		else{
			LoyalCustomerAccessEntity postLoyalCustomer = lstLoyalCustomerAccess.get(0);
			postLoyalCustomer.setPassword(previousLoyalCustomer.getNewPassword());
			postLoyalCustomer.setUpdatePasswordDate(new Date());
			criteria.andPasswordEqualTo(previousLoyalCustomer.getPassword());
			if(mapper.updateByExample(postLoyalCustomer, example) == 0){
				log.error("La contraseña actual no coincide con la introducia para el fidelizado: " + previousLoyalCustomer.getLoyalCustomerId());
				throw new ApiException(ApiException.STATUS_RESPONSE_ERROR_CONFLICT_STATE, "La contraseña no ha sido modificada");
			}
			log.info("Contraseña de acceso del fidelizado: " + previousLoyalCustomer.getLoyalCustomerId() + " ha sido actualizada correctamente.");
		}

	}

}
