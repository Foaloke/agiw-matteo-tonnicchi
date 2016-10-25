package com.tonnicchi.matteo.corium.ruleextractor;

@FunctionalInterface
public interface ElementPrinter<T> {
	public String print(T element);
}
