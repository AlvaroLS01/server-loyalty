package com.comerzzia.unide.api.services.triggers.ui;

import com.comerzzia.api.loyalty.service.triggers.actions.types.coupons.ActionCouponsData;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UNIDEActionCouponsData extends ActionCouponsData {

	protected PushData pushData;

	public UNIDEActionCouponsData(PushData pushData) {
		super();
		this.pushData = pushData;
	}

}
