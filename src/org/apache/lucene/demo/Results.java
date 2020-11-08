package org.apache.lucene.demo;

public class Results {
	/**
	 * id_query: id of the query
	 * id_doc: id of the result document for the query id_query
	*/
	
	String id_query;
	String id_doc;
	
	public Results() {}
	
	public Results(String id_q, String id_d) {
		id_query = id_q;
		id_doc = id_d;
	}
}
