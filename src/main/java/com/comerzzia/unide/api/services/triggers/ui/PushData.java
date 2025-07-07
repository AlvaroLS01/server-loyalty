package com.comerzzia.unide.api.services.triggers.ui;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PushData {

	private String titleOneSignal;
	private String subtitleOneSignal;
	private String messageOneSignal;
	private String imageOneSignal;
	private String launchOneSignal;

	// Getters y Setters
	public String getTitleOneSignal() {
		return titleOneSignal;
	}

	public void setTitleOneSignal(String titleOneSignal) {
		this.titleOneSignal = titleOneSignal;
	}

	public String getSubtitleOneSignal() {
		return subtitleOneSignal;
	}

	public void setSubtitleOneSignal(String subtitleOneSignal) {
		this.subtitleOneSignal = subtitleOneSignal;
	}

	public String getMessageOneSignal() {
		return messageOneSignal;
	}

	public void setMessageOneSignal(String messageOneSignal) {
		this.messageOneSignal = messageOneSignal;
	}

	public String getImageOneSignal() {
		return imageOneSignal;
	}

	public void setImageOneSignal(String imageOneSignal) {
		this.imageOneSignal = imageOneSignal;
	}

	public String getLaunchOneSignal() {
		return launchOneSignal;
	}

	public void setLaunchOneSignal(String launchOneSignal) {
		this.launchOneSignal = launchOneSignal;
	}
}
