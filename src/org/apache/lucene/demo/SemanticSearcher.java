package org.apache.lucene.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;


public class SemanticSearcher {
	public static void main(String[] args) throws IOException {
	    String usage = "java org.apache.lucene.demo.IndexFiles"
	                 + " [-rdf RDF_PATH] [-infoNeeds INFO_NEEDS_PATH] [-output OUTPUT_PATH]\n\n";
	                 
	    String rdfPath = null;
	    String infoNeedsPath = null;
	    String outputPath = null;
	    boolean create = true;
	    for(int i=0;i<args.length;i++) {
	      if ("-rdf".equals(args[i])) {
	        rdfPath = args[i+1];
	        i++;
	      } else if ("-infoNeeds".equals(args[i])) {
	        infoNeedsPath = args[i+1];
	        i++;
	      } else if ("-output".equals(args[i])) {
	        outputPath = args[i+1];
	        i++;
	      }
	    }

	    if (rdfPath == null | infoNeedsPath == null | outputPath == null) {
	      System.err.println("Usage: " + usage);
	      System.exit(1);
	    }
	    
	    File infoNeedsFile = new File(infoNeedsPath);
	    Scanner reader = new Scanner(infoNeedsFile);
	    
	    HashMap<String, String> infoNeeds = new HashMap<String, String>();
	    
	    while(reader.hasNextLine()) {
	    	String[] line = reader.nextLine().split(" ");
	    	String infoID = line[0];
	    	String infoQuery = "";
	    	for (int i=1; i < line.length; i++) {
	    		infoQuery += " " + line[i];
	    	}
	    	System.out.println(infoQuery);
	    	infoNeeds.put(infoID, infoQuery);
	    }
	    
	    
	    
	    EntityDefinition entDef = new EntityDefinition("uri", "name", ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/","name"));
	    entDef.set("description", DCTerms.description.asNode());
	    TextIndexConfig config = new TextIndexConfig(entDef);
	    config.setAnalyzer(new SpanishAnalyzer());
	    config.setQueryAnalyzer(new SpanishAnalyzer());
	    config.setMultilingualSupport(true);
	    
	    /*Dataset ds1 = DatasetFactory.createGeneral();
	    @SuppressWarnings("deprecation")
		Directory dir = new RAMDirectory();
	    Dataset ds = TextDatasetFactory.createLucene(ds1, dir, config);
	    
	    RDFDataMgr.read(ds.getDefaultModel(), rdfPath);*/
	    
	    FileUtils.deleteDirectory(new File("repositorio"));
	    Dataset ds1 = TDB2Factory.connectDataset("repositorio/-tdb2");
	    Directory dir = new MMapDirectory(Paths.get("./-repositorio/lucene"));
	    Dataset ds = TextDatasetFactory.createLucene(ds1, dir, config);
	    
	    ds.begin(ReadWrite.WRITE);
	    RDFDataMgr.read(ds.getDefaultModel(), rdfPath);
	    ds.commit();
	    ds.end();
	    
	    //Model rdfModel = FileManager.get().loadModel(rdfPath);    
	    	    
	    
	    HashMap<String, ArrayList<QuerySolution>> infoNeeds_results = new HashMap<String, ArrayList<QuerySolution>>();
	    for (String infoID: infoNeeds.keySet()) {
	    	String q = infoNeeds.get(infoID);
	    	Query query = QueryFactory.create(q);
	    	
	    	ArrayList<QuerySolution> querySolutions = new ArrayList<QuerySolution>();
	    	
	    	ds.begin(ReadWrite.READ);
	    	try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
	    		ResultSet results = qexec.execSelect();
	    			    		
	    		for (; results.hasNext();) {
	    			QuerySolution soln = results.nextSolution();
	    			System.out.println(soln);
	    			querySolutions.add(soln);

	    		}
	    	}
	    	ds.end();
	    	
	    	infoNeeds_results.put(infoID, querySolutions);
	    }
	    
	    
	    FileWriter writer = new FileWriter(outputPath);
	    
	    for(String infoID: infoNeeds.keySet()) {
	    	for (QuerySolution qs: infoNeeds_results.get(infoID)) {
	    		String sol = qs.toString();
	    		sol = sol.split("<")[1].split(">")[0];
	    		String[] tmp = sol.split("/");
	    		sol = tmp[tmp.length-1];
	    		sol = infoID + "\toai_zaguan.unizar.es_" + sol + ".xml";
	    		System.out.println(sol);
	    		writer.write(sol);
	    		writer.write("\n");
	    	}
	    }
	    
	    writer.close();
	    

	  }
}
