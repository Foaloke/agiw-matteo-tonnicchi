package com.tonnicchi.matteo.corium.ruleextractor;

public class CaptureGroup {

	private final String opening;
	private final String closing;
	private final String group;

	public CaptureGroup(String opening, String closing){
		this.opening = opening;
		this.closing = closing;
		this.group = opening + closing;
	}

	public String getOpening() {
		return opening;
	}

	public String getClosing() {
		return closing;
	}

	public String getGroup() {
		return group;
	}

}
