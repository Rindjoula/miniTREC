package org.apache.lucene.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.demo.SpanishAnalyzer2;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.glassfish.jersey.internal.util.Tokenizer;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

public class SearchFiles {
	private SearchFiles() {}

	  /** Simple command-line based search demo. */
	  public static void main(String[] args) throws Exception {
	    String usage =
	      "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
	    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
	      System.out.println(usage);
	      System.exit(0);
	    }

	    String index = "index";
	    String field = "contents";
	    String infoNeeds = null;
	    String output = null;
	    String queries = null;
	    	    
	    int repeat = 0;
	    boolean raw = false;
	    String queryString = null;
	    int hitsPerPage = 10;
	    
	    BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
	    
	    /** Parse all the parameters **/
	    for(int i = 0;i < args.length;i++) {
	      if ("-index".equals(args[i])) {
	        index = args[i+1];
	        i++;
	      } else if ("-infoNeeds".equals(args[i])) {
	    	infoNeeds = args[i+1];
	    	i++;
	      } else if ("-output".equals(args[i])) {
	    	  output = args[i+1];
	    	  i++;
	      } else if ("-field".equals(args[i])) {
	        field = args[i+1];
	        i++;
	      } else if ("-queries".equals(args[i])) {
	        queries = args[i+1];
	        i++;
	      } else if ("-query".equals(args[i])) {
	        queryString = args[i+1];
	        i++;
	      } else if ("-repeat".equals(args[i])) {
	        repeat = Integer.parseInt(args[i+1]);
	        i++;
	      } else if ("-raw".equals(args[i])) {
	        raw = true;
	      } else if ("-paging".equals(args[i])) {
	        hitsPerPage = Integer.parseInt(args[i+1]);
	        if (hitsPerPage <= 0) {
	          System.err.println("There must be at least 1 hit per page.");
	          System.exit(1);
	        }
	        i++;
	      }
	    }
	    
	    if (infoNeeds == null) {
	    	System.err.println("Must provide infoNeeds file with -infoNeeds");
		    System.exit(1);
	    }
	    
	    if (output == null) {
	    	System.err.println("Must provide output file with -output");
		    System.exit(1);
	    }
	    
	    /** Creation of the output file if does not exist **/
	    File output_file = new File(output);
	    
	    System.out.println("PARSING XML INFONEEDS");
	    
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Load the input XML document, parse it and return an instance of the
        // Document class.
        org.w3c.dom.Document document = builder.parse(new File(infoNeeds));
        NodeList nodeList = document.getDocumentElement().getChildNodes();
        
        ArrayList<ObjectQuery> informationNeeds = new ArrayList<ObjectQuery> ();
        
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                 Element elem = (Element) node;

  
                 // Get the value of all sub-elements.
                 String identifier = elem.getElementsByTagName("identifier")
                                     .item(0).getChildNodes().item(0).getNodeValue();

                 String text = elem.getElementsByTagName("text").item(0)
                                     .getChildNodes().item(0).getNodeValue();


                 System.out.println("ID: " + identifier + ", text: " + text);
                 
                 informationNeeds.add(new ObjectQuery(identifier, text));
                 
            }
        }
        
        // TODO: replace by the number of queries of the xml_query_file
	    ArrayList<ArrayList<Results>> results = new ArrayList<ArrayList<Results>>();
        
	    
	    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
	    IndexSearcher searcher = new IndexSearcher(reader);
	    Analyzer analyzer = new SpanishAnalyzer2();

	    QueryParser parser = new QueryParser(field, analyzer);
	    for (ObjectQuery obj_q: informationNeeds) {
	    	Query query = parser.parse(obj_q.text_query);
	    	//System.out.println(query.toString());
	    	System.out.println("Searching for: " + query.toString(field));
	      	      
	    	// TODO: set the argument query of obj_q --> readable by lucene, after the transformations...
	    	// Query q = ...
	    	// obj_q.setQuery(q);
	    	
	    	
	    	// TODO: replace by a for loop on all the queries of xml_query_file
	    	results.add(doPagingSearch(in, searcher, obj_q, hitsPerPage, raw, queries == null && queryString == null));
	      
	      //break;
	      /*if (queryString != null) {
	        break;
	      }*/
	    }
	    
	    /** Write on the result file all the results **/
	    FileWriter writer = new FileWriter(output);
	    for (ArrayList<Results> res: results) {
	    	for (Results r: res) {
	    		String file_result = r.id_query + "\t" 
	    				+ r.id_doc;
	    		writer.write(file_result);
	    		writer.write("\n");
	    	}
	    	
	    }
	    writer.close();
	    //reader.close();
	  }

	  /**
	   * This demonstrates a typical paging search scenario, where the search engine presents 
	   * pages of size n to the user. The user can then go to the next page if interested in
	   * the next hits.
	   * 
	   * When the query is executed for the first time, then only enough results are collected
	   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
	   * is executed another time and all hits are collected.
	   * 
	   * 
	   */
	  public static ArrayList<Results> doPagingSearch(BufferedReader in, IndexSearcher searcher, ObjectQuery obj_query, 
	                                     int hitsPerPage, boolean raw, boolean interactive) throws IOException {

		  Query query = obj_query.query;
		  // Collect enough docs to show 5 pages
		  TopDocs results = searcher.search(query, 5 * hitsPerPage);
		  ScoreDoc[] hits = results.scoreDocs;
	    
		  int numTotalHits = Math.toIntExact(results.totalHits.value);
		  System.out.println(numTotalHits + " total matching documents");
	    
		  ArrayList<Results> results_query = new ArrayList<Results>();
	    
		  for (int i=0; i<numTotalHits; i++) {

			  Document doc = searcher.doc(hits[i].doc);
		      String path = doc.get("path");
		      if (path != null) {
		    	  System.out.println((i+1) + ". " + path);
		    	  Results r = new Results(obj_query.id_query, path);   
		    	  results_query.add(r);
		      }
	        
		      // explaining results
		      //System.out.println(searcher.explain(query, hits[i].doc));
	                  
	      }
	  
	  return results_query;
	  
	  }
}
