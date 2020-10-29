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
	    
	    /** Creation of the file and directory results **/
	    Path results_folder = Paths.get("results/");
	    Files.createDirectory(results_folder);
	    Date date_now = new Date(System.currentTimeMillis());
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(date_now);
	    String results_file_name = "results_" + Integer.toString(calendar.get(Calendar.YEAR))
	    	+ Integer.toString(calendar.get(Calendar.MONTH))
	    	+ Integer.toString(calendar.get(Calendar.DAY_OF_MONTH))
	    	+ "_"
	    	+ Integer.toString(calendar.get(Calendar.HOUR_OF_DAY))
	    	+ Integer.toString(calendar.get(Calendar.MINUTE))
	    	+ Integer.toString(calendar.get(Calendar.SECOND))
	    	+ ".txt";
	    
	    File results_file = new File(results_folder.toString() + results_file_name);
	    System.out.println("Results file name : " + results_file.toString());
	    
	    // TODO: replace by the number of queries of the xml_query_file
	    Results[][] results = new Results[5][];
	    	    
	    int repeat = 0;
	    boolean raw = false;
	    String queryString = null;
	    int hitsPerPage = 10;
	    
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
	    
	    System.out.println("PARSING XML INFONEEDS");
	    
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Load the input XML document, parse it and return an instance of the
        // Document class.
        org.w3c.dom.Document document = builder.parse(new File(infoNeeds));
        NodeList nodeList = document.getDocumentElement().getChildNodes();
        
        ArrayList<String> informationNeeds = new ArrayList<String> ();
        
        
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
                 
                 informationNeeds.add(text);
                 
            }
       }
        
	    
	    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
	    IndexSearcher searcher = new IndexSearcher(reader);
	    Analyzer analyzer = new SpanishAnalyzer2();

	    BufferedReader in = null;
	    if (queries != null) {
	      in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
	    } else {
	      in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
	    }
	    QueryParser parser = new QueryParser(field, analyzer);
	    for (int i=0; i<queries.length(); i++) {
	      if (queries == null && queryString == null) {                        // prompt the user
	        System.out.println("Enter query: ");
	      }

	      // todo: change for the XML queries
	      String query_string = "title:Análisis de la evolucion económica de España desde la crisis de 2008 hasta 2019 en relacion con los diferentes partidos politicos que han gobernado el país durante este periodo o con cualquier otro aspecto de relevancia social."
	    		  	+ " " + "subject:Análisis de la evolucion económica de España desde la crisis de 2008 hasta 2019 en relacion con los diferentes partidos politicos que han gobernado el país durante este periodo o con cualquier otro aspecto de relevancia social.";
	      
	      
	      //String query_string = "title:evolucion";
	      System.out.println(query_string);
	            
	      Query query = parser.parse(query_string);
	      System.out.println(query.toString());
	      System.out.println("Searching for: " + query.toString(field));
	            
	      /*if (repeat > 0) {                           // repeat & time as benchmark
	        Date start = new Date();
	        for (int i = 0; i < repeat; i++) {
	          searcher.search(query, 100);
	        }
	        Date end = new Date();
	        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
	      }*/
	      
	      // TODO: replace query_1 by id_query
	      ObjectQuery obj_query = new ObjectQuery("query_1", query);
	      
	      // TODO: replace by a for loop on all the queries of xml_query_file
	      results[0] = doPagingSearch(in, searcher, obj_query, hitsPerPage, raw, queries == null && queryString == null);
	      
	      //break;
	      /*if (queryString != null) {
	        break;
	      }*/
	    }
	    
	    /** Write on the result file all the results **/
	    FileWriter writer = new FileWriter(results_folder.toString() + results_file_name);
	    for (int i=0; i < results.length; i++) {
	    	for (int j=0; j<results[i].length; j++) {
	    		String r = results[i][j].id_query + "\t" 
	    				+ results[i][j].id_doc;
	    		writer.write(r);
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
	  public static Results[] doPagingSearch(BufferedReader in, IndexSearcher searcher, ObjectQuery obj_query, 
	                                     int hitsPerPage, boolean raw, boolean interactive) throws IOException {

		Query query = obj_query.query;
	    // Collect enough docs to show 5 pages
	    TopDocs results = searcher.search(query, 5 * hitsPerPage);
	    ScoreDoc[] hits = results.scoreDocs;
	    
	    int numTotalHits = Math.toIntExact(results.totalHits.value);
	    System.out.println(numTotalHits + " total matching documents");

	    int start = 0;
	    int end = Math.min(numTotalHits, hitsPerPage);
	        
	    while (true) {
	      if (end > hits.length) {
	        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
	        System.out.println("Collect more (y/n) ?");
	        String line = in.readLine();
	        if (line.length() == 0 || line.charAt(0) == 'n') {
	          break;
	        }

	        hits = searcher.search(query, numTotalHits).scoreDocs;
	      }
	      
	      end = Math.min(hits.length, start + hitsPerPage);
	      
	      for (int i = start; i < end; i++) {
	        if (raw) {                              // output raw format
	          System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
	          continue;
	        }

	        Document doc = searcher.doc(hits[i].doc);
	        String path = doc.get("path");
	        if (path != null) {
	          System.out.println((i+1) + ". " + path);
	          long modified = Long.parseLong(doc.get("modified"));
	          if(raw == false & modified != 0) {
	        	  System.out.println("modified: " + new Date(modified));
	          }
	          
	          
	          
	        } else {
	          System.out.println((i+1) + ". " + "No path for this document");
	        }
	        
	        // explaining results
	        System.out.println(searcher.explain(query, hits[i].doc));
	                  
	      }

	      if (!interactive || end == 0) {
	        break;
	      }

	      if (numTotalHits >= end) {
	        boolean quit = false;
	        while (true) {
	          System.out.print("Press ");
	          if (start - hitsPerPage >= 0) {
	            System.out.print("(p)revious page, ");  
	          }
	          if (start + hitsPerPage < numTotalHits) {
	            System.out.print("(n)ext page, ");
	          }
	          System.out.println("(q)uit or enter number to jump to a page.");
	          
	          String line = in.readLine();
	          if (line.length() == 0 || line.charAt(0)=='q') {
	            quit = true;
	            break;
	          }
	          if (line.charAt(0) == 'p') {
	            start = Math.max(0, start - hitsPerPage);
	            break;
	          } else if (line.charAt(0) == 'n') {
	            if (start + hitsPerPage < numTotalHits) {
	              start+=hitsPerPage;
	            }
	            break;
	          } else {
	            int page = Integer.parseInt(line);
	            if ((page - 1) * hitsPerPage < numTotalHits) {
	              start = (page - 1) * hitsPerPage;
	              break;
	            } else {
	              System.out.println("No such page");
	            }
	          }
	        }
	        if (quit) break;
	        end = Math.min(numTotalHits, start + hitsPerPage);
	      }
	    }
	    Results results_1 = new Results("query_1", "doc_1");
	    Results[] results_query = new Results[numTotalHits];
	    results_query[0] = results_1;
	    return results_query;
	  }
}
