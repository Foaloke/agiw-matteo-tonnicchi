package com.tonnicchi.matteo.corium.ruleextractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class RuleExtractor<T extends Comparable<T>> {

	protected List<List<T>> samples = new ArrayList<>();

	public void addSample(List<T> sample) {
		this.samples.add(sample);
	}

	public void addSamples(List<List<T>> samples) {
		this.samples.addAll(samples);
	}
	
	public void reset(){
		this.samples.clear();
	}

	public abstract Set<String> extractRules();

}
