package com.comerzzia.unide.api.services.triggers.ui;

import com.comerzzia.api.loyalty.web.model.triggers.ActionPointsData;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UNIDEActionPointsData extends ActionPointsData {

	protected PushData pushData;

	public UNIDEActionPointsData(PushData pushData) {
		super();
		this.pushData = pushData;
	}
}
