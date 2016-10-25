package com.tonnicchi.matteo.corium.ruleextractor.wrapperElement;

public class WrapperElement implements Comparable<WrapperElement> {

	public enum Type {
		TAG, TEXT
	}

	private final Type type;
	private final String name;

	public WrapperElement(Type type, String name) {
		this.type = type;
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	@Override
	public int compareTo(WrapperElement o) {
		Type otherType = o.getType();
		String otherName = o.getName();
		if (otherType != this.getType()) {
			return this.getType().compareTo(otherType);
		} else {
			return this.getName().compareTo(otherName);
		}
	}
	
	@Override
	public String toString() {
		return this.name;
	}

}
