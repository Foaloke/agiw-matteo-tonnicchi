package com.tonnicchi.matteo.corium.ruleextractor.sources;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class Source {

	private final String key;
	private final List<String> addresses;

	public Source(String key) {
		this.key = key;
		this.addresses = new ArrayList<>();
	}

	public String getKey() {
		return key;
	}

	public List<String> getAddresses() {
		return addresses;
	}

	public void addAddress(Object address) {
		this.addresses.add(address.toString());
	}

	@Override
	public String toString() {
		return this.key
				+ "\n"
				+ Joiner.on("\n").join(Lists.transform(this.addresses, address -> address.substring(0, 20) + "..."));
	}

}
