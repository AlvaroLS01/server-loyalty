package com.comerzzia.unide.api.services.triggers.ui;

import com.comerzzia.api.loyalty.service.triggers.actions.types.tags.ActionTagsData;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UNIDEActionTagsData extends ActionTagsData {

	protected PushData pushData;

	public UNIDEActionTagsData(PushData pushData) {
		super();
		this.pushData = pushData;
	}
}
