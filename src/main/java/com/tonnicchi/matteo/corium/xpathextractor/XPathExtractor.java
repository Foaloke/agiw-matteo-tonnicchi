package com.tonnicchi.matteo.corium.xpathextractor;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableList;

public class XPathExtractor {

	private static final String TEXT_FLAG = "text()";
	
	public static List<String> extract(String xpath, String html) {

		List<String> results = new ArrayList<>();
		
		Document document = Jsoup.parse(html);
		document.outputSettings().syntax(Document.OutputSettings.Syntax.html);
		document.outputSettings().escapeMode(EscapeMode.xhtml);
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpathObj = xPathfactory.newXPath();

		try {
			
			if(xpath.endsWith(TEXT_FLAG)){
				

				XPathExpression expr = xpathObj.compile(xpath);
				NodeList nl = (NodeList) expr.evaluate(new W3CDom().fromJsoup(document), XPathConstants.NODESET);
				for(int i = 0; i<nl.getLength(); i++){
					results.add(nl.item(i).getTextContent());
				}
				
			}else{

				XPathExpression expr = xpathObj.compile(xpath);
				results.add((String) expr.evaluate(new W3CDom().fromJsoup(document), XPathConstants.STRING));
			}
			
			
			
		} catch (XPathExpressionException e) {
			throw new IllegalStateException("Could not parse the given xpath expression " + xpath, e);
		}

		return results;
		
	}

}
