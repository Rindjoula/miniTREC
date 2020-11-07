package org.apache.lucene.demo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

public class ModelsOpenNLP {
		
	public static String[] getNames (String text_query, String name_model) 
			throws FileNotFoundException, IOException {
		
		String[] tokens = null;
		try (InputStream modelIn = new FileInputStream(name_model)){
			TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
			NameFinderME nameFinder = new NameFinderME(model);
			String[] list_words = text_query.split("\\s+|(?=\\p{Punct})|(?<=\\p{Punct})");
			ArrayList<String> array_words = new ArrayList<String>(Arrays.asList(list_words));
			//System.out.println(sentence);
			/*for (String w: list_words) {
				System.out.println(w);
			}*/
			
			Span nameSpans[] = nameFinder.find(list_words);
			
			for (int i=0; i<nameSpans.length; i++) {
    			System.out.println(nameSpans[i]);
    		}
			
			tokens = new String[nameSpans.length];
					
			for (int i=0; i<nameSpans.length; i++) {
				int start = nameSpans[i].getStart();
				int end = nameSpans[i].getEnd();
				tokens[i] = String.join(" ", array_words.subList(start, end));
				System.out.println(tokens[i]);
			}		
		}
		return tokens;
	}
	
	public static String[] getLocations (String text_query, String name_model) 
			throws FileNotFoundException, IOException {
		String[] tokens = null;
		try (InputStream modelIn = new FileInputStream(name_model)){
			TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
			NameFinderME nameFinder = new NameFinderME(model);
			String[] list_words = text_query.split("\\s+|(?=\\p{Punct})|(?<=\\p{Punct})");
			ArrayList<String> array_words = new ArrayList<String>(Arrays.asList(list_words));
Span nameSpans[] = nameFinder.find(list_words);
			
			for (int i=0; i<nameSpans.length; i++) {
    			System.out.println(nameSpans[i]);
    		}
			
			tokens = new String[nameSpans.length];
					
			for (int i=0; i<nameSpans.length; i++) {
				int start = nameSpans[i].getStart();
				int end = nameSpans[i].getEnd();
				tokens[i] = String.join(" ", array_words.subList(start, end));
				System.out.println(tokens[i]);
			}		
		}
		
		return tokens;
	}
}
