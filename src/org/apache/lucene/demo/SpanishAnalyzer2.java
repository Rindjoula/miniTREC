package org.apache.lucene.demo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class SpanishAnalyzer2 extends Analyzer {
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source = new StandardTokenizer();
		CharArraySet stopSet = SpanishAnalyzer.getDefaultStopSet();
		stopSet.add("desde");
		TokenStream filter1 = new StopFilter(source, stopSet);
		TokenStream filter2 = new LowerCaseFilter(filter1);
		filter2 = new SnowballFilter(filter2, "Spanish");
		return new TokenStreamComponents(source, filter2);
	}
}
