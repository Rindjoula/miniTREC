package org.apache.lucene.demo;

import org.apache.lucene.search.Query;

public class ObjectQuery {
	/** 
	 * id_query: id of the query
	 * text_query: string containing the query
	 * query: object query for lucene
	*/
	
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
