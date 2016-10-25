package com.tonnicchi.matteo.corium.ruleextractor.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tonnicchi.matteo.corium.ruleextractor.CaptureGroup;
import com.tonnicchi.matteo.corium.ruleextractor.ElementPrinter;
import com.tonnicchi.matteo.corium.ruleextractor.RuleExtractor;

public class RuleExtractor_Levenshtein<T extends Comparable<T>> extends RuleExtractor<T> {

	private enum EditStep {
		INSERTION, SUBSTITUTION, DELETION, NO_CHANGE;
	}

	private CaptureGroup captureGroup;
	private ElementPrinter<T> elementPrinter;

	public RuleExtractor_Levenshtein(CaptureGroup captureGroup, ElementPrinter<T> elementPrinter) {
		this.captureGroup = captureGroup;
		this.elementPrinter = elementPrinter;
	}
	
	@Override
	public Set<String> extractRules() {

		Set<String> rulePool = new HashSet<>();

		List<T> sampleA;
		List<T> sampleB;

		try {
			for (int i = 0; i < this.samples.size(); i++) {
				for (int j = i + 1; j < this.samples.size(); j++) {
					sampleA = this.samples.get(i);
					sampleB = this.samples.get(j);
					rulePool.addAll(extractRegex(sampleA, sampleB));
					rulePool.addAll(extractRegex(sampleB, sampleA));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return rulePool;

	}

	private Set<String> extractRegex(List<T> sampleA, List<T> sampleB) throws Exception {

		Set<String> pool = new HashSet<>();
		StringBuffer regexA = new StringBuffer();
		StringBuffer regexB = new StringBuffer();

		List<EditStep> editSteps = calculateEditSteps(sampleA, sampleB);

		boolean capturingGroup = false;
		int index = 0;
		int insertions = 0;
		int deletions = 0;
		for (EditStep editStep : editSteps) {
			switch (editStep) {
			case DELETION: {
				capturingGroup = closeCaptouringGroupIfNeeded(regexA, regexB, capturingGroup);
				if(index!=sampleA.size()){
					regexA.append(elementPrinter.print(sampleA.get(index)));
				}
				deletions++;
				break;
			}
			case INSERTION: {
				capturingGroup = closeCaptouringGroupIfNeeded(regexA, regexB, capturingGroup);
				regexB.append(elementPrinter.print(sampleB.get(index)));
				insertions++;
				break;
			}
			case NO_CHANGE: {
				capturingGroup = openCaptouringGroupIfNeeded(regexA, regexB, capturingGroup);
				break;
			}
			case SUBSTITUTION: {
				capturingGroup = closeCaptouringGroupIfNeeded(regexA, regexB, capturingGroup);
				regexA.append(elementPrinter.print(sampleA.get(index - insertions)));
				regexB.append(elementPrinter.print(sampleB.get(index - deletions)));
				break;
			}
			default: {
				break;
			}

			}
			index++;
		}

		closeCaptouringGroupIfNeeded(regexA, regexB, capturingGroup);

		pool.add(regexA.toString());
		pool.add(regexB.toString());

		return pool;

	}

	private boolean openCaptouringGroupIfNeeded(StringBuffer regexA, StringBuffer regexB, boolean capturingGroup) {
		if (!capturingGroup) {
			appendIfNotEndingWithCapturingGroup(regexA, captureGroup.getOpening());
			appendIfNotEndingWithCapturingGroup(regexB, captureGroup.getOpening());
			capturingGroup = true;
		}
		return capturingGroup;
	}

	private boolean closeCaptouringGroupIfNeeded(StringBuffer regexA, StringBuffer regexB, boolean capturingGroup) {
		if (capturingGroup) {
			appendIfNotEndingWithCapturingGroup(regexA, captureGroup.getClosing());
			appendIfNotEndingWithCapturingGroup(regexB, captureGroup.getClosing());
			capturingGroup = false;
		}
		return capturingGroup;
	}

	private void appendIfNotEndingWithCapturingGroup(StringBuffer regex, String stringToAppend) {
		if (!regex.toString().endsWith(captureGroup.getGroup())) {
			regex.append(stringToAppend);
		}
	}

	private List<EditStep> calculateEditSteps(List<T> sampleA, List<T> sampleB) {

		List<EditStep> editSteps = new ArrayList<>();

		int[][] levenshteinTable = computeLevenshteinTable(sampleA, sampleB);
		int tableSize = levenshteinTable.length;

		int i = levenshteinTable.length - 1;
		int j = levenshteinTable[tableSize - 1].length - 1;

		int currentCost;
		int deletionCost;
		int insertionCost;
		int substitutionCost;
		while (i > 0 || j > 0) {

			currentCost = calculateCost(levenshteinTable, i, j);

			substitutionCost = calculateCost(levenshteinTable, i - 1, j - 1);
			deletionCost = calculateCost(levenshteinTable, i - 1, j);
			insertionCost = calculateCost(levenshteinTable, i, j - 1);

			if (substitutionCost <= deletionCost && substitutionCost <= insertionCost) {
				editSteps.add(0, currentCost == substitutionCost ? EditStep.NO_CHANGE : EditStep.SUBSTITUTION);
				i = i - 1;
				j = j - 1;
			} else if (deletionCost <= insertionCost) {
				editSteps.add(0, EditStep.DELETION);
				i = i - 1;
			} else {
				editSteps.add(0, EditStep.INSERTION);
				j = j - 1;
			}

		}

		return editSteps;

	}

	private int calculateCost(int[][] levenshteinTable, int i, int j) {
		if (i >= 0 && j >= 0) {
			return levenshteinTable[i][j];
		}
		return Integer.MAX_VALUE;
	}

	private int[][] computeLevenshteinTable(List<T> sequenceA, List<T> sequenceB) {

		int[][] distanceMatrix = new int[sequenceA.size() + 1][sequenceB.size() + 1];

		for (int i = 0; i <= sequenceA.size(); i++) {
			distanceMatrix[i][0] = i;
		}
		for (int j = 1; j <= sequenceB.size(); j++) {
			distanceMatrix[0][j] = j;
		}

		for (int i = 1; i <= sequenceA.size(); i++) {
			for (int j = 1; j <= sequenceB.size(); j++) {
				distanceMatrix[i][j]
					= minimum(
						distanceMatrix[i - 1][j] + 1,
						distanceMatrix[i][j - 1] + 1,
						distanceMatrix[i - 1][j - 1] + (sequenceA.get(i - 1).compareTo(sequenceB.get(j - 1)) == 0 ? 0 : 1));
			}
		}

		return distanceMatrix;

	}

	private static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}

	private void printLevenshteinTable(List<T> sampleA, List<T> sampleB, int[][] levenshteinTable) {

		System.out.println("A: " + sampleA);
		System.out.println("B: " + sampleB);

		System.out.print("\t\t");

		for (int k = 0; k < sampleB.size(); k++) {
			System.out.print("\t" + sampleB.get(k));
		}

		System.out.println();

		for (int i = 0; i < levenshteinTable.length; i++) {
			System.out.print("\t");
			if (i - 1 >= 0) {
				System.out.print(sampleA.get(i - 1));
			}
			for (int j = 0; j < levenshteinTable[i].length; j++) {
				System.out.print("\t" + levenshteinTable[i][j]);
			}

			System.out.println();
		}

	}

}
