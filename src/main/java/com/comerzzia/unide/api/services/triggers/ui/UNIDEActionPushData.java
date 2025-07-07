package com.comerzzia.unide.api.services.triggers.ui;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UNIDEActionPushData {

	protected PushData pushData;

	public UNIDEActionPushData(PushData pushData) {
		super();
		this.pushData = pushData;
	}
}