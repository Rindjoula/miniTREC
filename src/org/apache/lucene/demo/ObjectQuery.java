package org.apache.lucene.demo;

import org.apache.lucene.search.Query;

public class ObjectQuery {
	String id_query;
	String text_query;
	Query query;
	
	public ObjectQuery(String id, String text) {
		id_query = id;
		text_query = text;
	}
	
	public void setQuery(Query q) {
		query = q;
	}
}
