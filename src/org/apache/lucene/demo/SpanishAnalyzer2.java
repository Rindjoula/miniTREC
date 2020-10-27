package org.apache.lucene.demo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class SpanishAnalyzer2 extends Analyzer {
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source = new StandardTokenizer();
		TokenStream result = new LowerCaseFilter(source);
		result = new SnowballFilter(result, "Spanish"); 
		return new TokenStreamComponents(source, result);
	}
}
