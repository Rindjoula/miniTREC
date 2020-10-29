package org.apache.lucene.demo;

import org.apache.lucene.search.Query;

public class ObjectQuery {
	String id_query;
	Query query;
	
	public ObjectQuery(String id, Query q) {
		id_query = id;
		query = q;
	}
}
