package com.tonnicchi.matteo.corium.ruleextractor.sources;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class SourcesReader {

	public static Map<String, List<Source>> filterSources(String sourcesString, int startIndex, int endIndex){

		if (startIndex >= endIndex) {
			throw new IllegalArgumentException("The start index must be minor than the end index");
		}

		JSONObject mainNode = new JSONObject(sourcesString);

		return mainNode.keySet().stream()				
			.collect(Collectors.toMap(					
					Function.identity(), extractSourcesInInterval(mainNode, startIndex, endIndex)					
			)).entrySet().stream()
			.filter(entry -> !entry.getValue().isEmpty())
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		
	}
	
	private static Function<String, List<Source>> extractSourcesInInterval(JSONObject mainNode, int startIndex, int endIndex){
		return rootKey -> {
			JSONObject sourceNode = mainNode.getJSONObject(rootKey);
			return sourceNode.keySet().stream()
					.filter(keyIsInInterval(startIndex, endIndex))
					.map(extractSources(sourceNode))
					.collect(Collectors.toList());
		};
	}

	private static Function<String, Source> extractSources(JSONObject sourceNode){
		return sourceKey -> {
			Source filteredSource = new Source(sourceKey);
			sourceNode.getJSONArray(sourceKey).forEach(filteredSource::addAddress);
			return filteredSource;
		};
	}

	private static Predicate<String> keyIsInInterval(int start, int end) {
		return sourceKey -> {
			int sourceIndex = extractSourceIndex(sourceKey);
			return sourceIndex >= start && sourceIndex <= end;
		};
	}
	
	private static int extractSourceIndex(String sourceId) {
		try {
			return Integer.parseInt(sourceId.split("-")[0]);
		}catch (NumberFormatException e){
			//System.out.println("Can not parse an index from sourceId " + sourceId);
			return Integer.MAX_VALUE;
		}
	}

}
