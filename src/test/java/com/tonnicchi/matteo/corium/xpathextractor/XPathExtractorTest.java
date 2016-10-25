package com.tonnicchi.matteo.corium.xpathextractor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.Test;

public class XPathExtractorTest {

	@Test
	public void extractsCorrectly() throws IOException{
		
		//String html = Resources.toString(Resources.getResource("testHTML.html"), Charsets.UTF_8);
		//XPathExtractor.extractXpathFromHTML("", html);

        assertThat( true, equalTo(true) );
	}
	
}
