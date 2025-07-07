package com.comerzzia.unide.api.services.coupons.links;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.api.loyalty.persistence.coupons.links.CouponLinkExample;
import com.comerzzia.api.loyalty.persistence.coupons.links.CouponLinkKey;
import com.comerzzia.api.loyalty.persistence.coupons.links.CouponLinkMapper;
import com.comerzzia.api.loyalty.service.coupons.links.CouponsLinksService;
import com.comerzzia.api.loyalty.service.coupons.links.CouponsLinksServiceImpl;
import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.unide.api.persistence.coupons.links.UNIDECouponLinkExample;
import com.comerzzia.unide.api.persistence.coupons.links.UNIDECouponLinkMapper;

@Service
@Primary
public class UNIDECouponsLinksServiceImpl extends CouponsLinksServiceImpl {

	@Autowired
	private CouponLinkMapper mapper;

	public void deleteByClassIds(List<String> classIds) throws ApiException {
		try {
			if (classIds == null || classIds.isEmpty()) {
				return;
			}
			CouponLinkExample example = new CouponLinkExample();
			CouponLinkExample.Criteria criteria = example.createCriteria();
			criteria.andClassIdIn(classIds);
			mapper.deleteByExample(example);
		}
		catch (Exception e) {
			// Manejar cualquier excepción y lanzar una ApiException personalizada
			throw new ApiException("Error al eliminar registros por classIds: " + classIds, e);
		}
	}
}
