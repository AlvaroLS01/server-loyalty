package com.comerzzia.unide.api.services.coupons.links;

import java.util.List;

import com.comerzzia.api.core.service.exception.ApiException;

public interface UNIDECouponsLinksService {

	void deleteByClassIds(List<String> classIds) throws ApiException;

}
