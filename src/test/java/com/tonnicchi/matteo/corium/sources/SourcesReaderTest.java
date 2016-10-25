package com.tonnicchi.matteo.corium.sources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.tonnicchi.matteo.corium.ruleextractor.sources.Source;
import com.tonnicchi.matteo.corium.ruleextractor.sources.SourcesReader;

public class SourcesReaderTest {

	private String SAMPLE_SOURCE_PATH = "sample_sources.json";
	private String SAMPLE_SOURCE;
	
	@Before
	public void init() throws IOException{
		SAMPLE_SOURCE = Resources.toString(Resources.getResource(SAMPLE_SOURCE_PATH), Charsets.UTF_8);
	}
	
	@Test
	public void filtersSourcesCorrectly(){
		Map<String, List<Source>> sources = SourcesReader.filterSources(SAMPLE_SOURCE, 370, 372);
		assertThat(sources.size(), equalTo(2));
		assertThat(sources.keySet(), containsInAnyOrder("compareindia.ibnlive.com", "comparestores.net"));
		assertThat(sources.get("compareindia.ibnlive.com").size(), equalTo(1));
		assertThat(sources.get("compareindia.ibnlive.com").get(0).getAddresses().size(), equalTo(10));
		assertThat(sources.get("comparestores.net").size(), equalTo(2));
		assertThat(sources.get("comparestores.net").get(0).getAddresses().size(), equalTo(20));
	}
	
}
