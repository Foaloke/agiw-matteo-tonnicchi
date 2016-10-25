package com.tonnicchi.matteo.corium;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import com.tonnicchi.matteo.corium.downloader.Downloader;
import com.tonnicchi.matteo.corium.ruleextractor.CaptureGroup;
import com.tonnicchi.matteo.corium.ruleextractor.RuleExtractor;
import com.tonnicchi.matteo.corium.ruleextractor.impl.RuleExtractor_Levenshtein;
import com.tonnicchi.matteo.corium.ruleextractor.sources.Source;
import com.tonnicchi.matteo.corium.ruleextractor.sources.SourcesReader;
import com.tonnicchi.matteo.corium.ruleextractor.wrapperElement.WrapperElement;
import com.tonnicchi.matteo.corium.ruleextractor.wrapperElement.WrapperElement.Type;
import com.tonnicchi.matteo.corium.xpathextractor.XPathExtractor;

public class Main 
{
	private static final String OUT_PATH = "out";
	private static final String OUT_JSON_PATH = "json";
	private static final String JSON_PATH = "xpath.json";
	private static final String OUT_DATA_PATH = "data";
	private static final String DATA_PATH = "data.json";
	
	private static final String SOURCE_FILE = "sources.json";
	private static final int START_INDEX = 251;
	private static final int END_INDEX = 500;
	
	private static final RuleExtractor<WrapperElement> RULE_EXTRACTOR
		= new RuleExtractor_Levenshtein<>(
				new CaptureGroup("//","*"),
				elementToPrint -> printWrapperElement(elementToPrint));
	
    public static void main( String[] args )
    {

		//downloadAll();

    	//checkXPath(args[0],args[1]);

    	buildRulesJson();

    	//buildDataJson();

    }

	private static void buildRulesJson() {
		try {

			JSONObject rootObject = new JSONObject();
			
			SourcesReader
			.filterSources(readResource(SOURCE_FILE), START_INDEX, END_INDEX)
			.forEach( (rootId, sources) -> {

				JSONObject sourcesObject = new JSONObject();
				
				sources.stream().forEach(
					source -> {
						
						JSONArray sourceData = new JSONArray();
						
						getSourceRuleData(source).stream().forEach(
								sourceRuleData -> {
									JSONObject sourceRuleObj = new JSONObject();
									sourceRuleObj.put("rule", sourceRuleData[1]);
									sourceRuleObj.put("attribute_name", sourceRuleData[0]);
									sourceRuleObj.put("page_id", isInAddress(source, sourceRuleData[1]));
									sourceData.add(sourceRuleObj);
								}
						);
						
						sourcesObject.put(source.getKey().split("-")[1], sourceData);
						
					}
				);

				rootObject.put(rootId, sourcesObject);

			});
			
			Downloader.writeFile(OUT_PATH, OUT_JSON_PATH, JSON_PATH, rootObject.toJSONString());
			
		} catch (IOException e) {
			throw new IllegalStateException("Can not read resource file "+SOURCE_FILE, e);
		}		
		
	}
	

	private static void buildDataJson() {
		try {

			JSONObject rootObject = new JSONObject();
			
			SourcesReader
			.filterSources(readResource(SOURCE_FILE), START_INDEX, END_INDEX)
			.forEach( (rootId, sources) -> {

				JSONObject sourcesObject = new JSONObject();
				sources.stream().forEach(
						source -> {

							JSONObject resultObj = new JSONObject();

							for(String[] sourceRuleData : getSourceRuleData(source)){
								
								String attributeName = sourceRuleData[0];
								String rule = sourceRuleData[1];
																		
								JSONArray resultsArray = new JSONArray();
								for(String address : source.getAddresses()){
										List<String> extracted
											= XPathExtractor
												.extract(rule, Downloader.downloadLazy(source.getKey(), address));

										if(!extracted.isEmpty()){
											JSONObject singleResultObj = new JSONObject();
											singleResultObj.put(address, extracted.get(0));
											resultsArray.add(singleResultObj);					
										}

										
								}
		
								resultObj.put(attributeName, resultsArray);
								
							}

							sourcesObject.put(source.getKey().split("-")[1], resultObj);
							
						}
				);

				rootObject.put(rootId, sourcesObject);
				
			});
			
			Downloader.writeFile(OUT_PATH, OUT_DATA_PATH, DATA_PATH, rootObject.toJSONString());
			
		} catch (IOException e) {
			throw new IllegalStateException("Can not read resource file "+SOURCE_FILE, e);
		}		
		
	}

	private static boolean isInAddress(Source source, String rule){
		return source.getAddresses().stream().map(
			address ->
				XPathExtractor
					.extract(rule, Downloader.downloadLazy(source.getKey(), address) )
					.stream().anyMatch(address::contains)
		 ).anyMatch(Boolean.TRUE::equals);
		
	}
	
	private static List<String[]> getSourceRuleData(Source source){
		List<String[]> ruleData = new ArrayList<>();
		try {
			String text = Downloader.load(Downloader.REGEXES_DIR_PATH, source.getKey(), source.getKey());
			for( String line : text.split("\n") ){
				ruleData.add(line.split("§"));
			}
		} catch (IOException e) {
			// Skip
		}
		return ruleData;
	}

	private static void downloadAll() {
		consumeSources(
			source ->
				source.getAddresses().stream()
				.forEach(
					address -> Downloader.downloadLazy(source.getKey(), address))
			);
	}
	
	private static void checkXPath(String filterKey, String xpath){
		consumeSources(actionOnSourceKeyFiltered(filterKey, testXPathOnAllAddressesInSource(xpath)));
	}

	private static Consumer<Source> actionOnSourceKeyFiltered(String filterKey, Consumer<Source> actionOnSource) {
		return source -> {
			if(source.getKey().startsWith(filterKey)){
				actionOnSource.accept(source);
			}
		};
	}
	
	private static Consumer<Source> testXPathOnAllAddressesInSource(String xpath) {
		return source ->
			source.getAddresses().stream()
				.forEach(address -> {
					if(!Downloader.isKnownUnreachable(address)){
						List<String> results
							= XPathExtractor.extract(
								xpath,
								Downloader.downloadLazy(source.getKey(), address));
						if(results.isEmpty()){
							System.out.println("NO RESULT!");
						}else{
							System.out.println(results);
						}
					}
				});
	}

	private static void consumeSources(Consumer<Source> sourceConsumer) {
		try {
			SourcesReader
			.filterSources(readResource(SOURCE_FILE), START_INDEX, END_INDEX)
			.forEach( (rootId, sources) -> sources.stream().forEach(sourceConsumer::accept));
		} catch (IOException e) {
			throw new IllegalStateException("Can not read resource file "+SOURCE_FILE, e);
		}		
	}

	private static Consumer<? super String> testRegexOnAllFilesIn(String sourceKey) {
		return regex -> Downloader.testRegexOnAllFilesIn(regex, sourceKey).stream().forEach(System.out::println);
	}

	private static Map<String, Set<String>> sourceListToRegexIndex(List<Source> sources){
    	return sources.stream()
				.collect(Collectors.toMap(
					Source::getKey,
					source -> structuresToRegex(generateStructures(source))));
    }

    private static List<List<WrapperElement>> generateStructures(Source source) {
    	return source.getAddresses().stream()
				.map(address -> addressToWrapperElements(source.getKey(), address))
				.collect(Collectors.toList());
	}
    
    private static Set<String> structuresToRegex(List<List<WrapperElement>> structures) {
    	RULE_EXTRACTOR.reset();
		RULE_EXTRACTOR.addSamples(structures);
		return RULE_EXTRACTOR.extractRules();
    }
    
	private static List<WrapperElement> addressToWrapperElements(String sourceKey, String address){
  
		String file = Downloader.downloadLazy(sourceKey, address);
		
		return Jsoup.parse(file).getAllElements().stream()
			.flatMap(element -> 
				element.text().isEmpty() ?
						Stream.of(new WrapperElement(Type.TAG, element.tagName()))
					: 	Stream.of(	new WrapperElement(Type.TAG, element.tagName()),
									new WrapperElement(Type.TEXT, element.text()))
			).collect(Collectors.toList());

    }

    private static String printWrapperElement(WrapperElement elementToPrint) {
		return elementToPrint.getType() == Type.TEXT ?
					""
				: 	elementToPrint.getName().equals("#root") ?
							""
						: 	"/"+elementToPrint.getName() ;
	}

	private static String readResource(String sourceFile) throws IOException {
		return Resources.toString(Resources.getResource(SOURCE_FILE), Charsets.UTF_8);
	}
}
