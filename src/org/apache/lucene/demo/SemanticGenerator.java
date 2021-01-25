package org.apache.lucene.demo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.apache.lucene.document.*;
import org.w3c.dom.NodeList;

import openllet.jena.PelletReasonerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.soap.Node;
import org.w3c.dom.Node;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.SKOS; 
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.ReasonerVocabulary;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

import openllet.jena.PelletReasonerFactory;

/** Index all text files under a directory.*/
public class SemanticGenerator {
  
private SemanticGenerator() {}
	
	/** Index all text files under a directory. 
	 * @throws IOException */
	  public static void main(String[] args) throws IOException {
	    String usage = "java org.apache.lucene.demo.IndexFiles"
	                 + " [-rdf RDF_PATH] [-skos SKOS_PATH] [-owl OWL_PATH] [-docs DOCS_PATH]\n\n";
	                 
	    String rdfPath = null;
	    String skosPath = null;
	    String owlPath = null;
	    String docsPath = null;
	    boolean create = true;
	    for(int i=0;i<args.length;i++) {
	      if ("-rdf".equals(args[i])) {
	        rdfPath = args[i+1];
	        i++;
	      } else if ("-skos".equals(args[i])) {
	        skosPath = args[i+1];
	        i++;
	      } else if ("-owl".equals(args[i])) {
	        owlPath = args[i+1];
	        i++;
	      } else if ("-docs".equals(args[i])) {
	    	docsPath = args[i+1];
	    	i++;
	      }
	    }

	    if (rdfPath == null | skosPath == null | owlPath == null | docsPath == null) {
	      System.err.println("Usage: " + usage);
	      System.exit(1);
	    }

	    final File docDir = new File(docsPath);
	    if (!docDir.exists() || !docDir.canRead()) {
	      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
	      System.exit(1);
	    }
	    
	    Date start = new Date();
	    
	    createRDFDocs(docDir);
	    
	    Date end = new Date();
	    System.out.println(end.getTime() - start.getTime() + " total milliseconds");
	    
	    System.out.println("INFERENCIA");
	    start = new Date();
	    	    
	    inferRDFDocs(rdfPath, owlPath, skosPath);
	    
	    end = new Date();
	    System.out.println(end.getTime() - start.getTime() + " total milliseconds");
	    
	    //File tmp = new File("tmp.ttl");
	    //tmp.delete();

	  }
	  
	  static void inferRDFDocs(String rdfPath, String owlPath, String skosPath) throws IOException {
		  Model rdfModel = FileManager.get().loadModel("tmp.ttl");
		  Model owlModel = FileManager.get().loadModel(owlPath);
		  Model skosModel = FileManager.get().loadModel(skosPath);
		  
		  Model union1 = ModelFactory.createUnion(rdfModel, owlModel);
		  Model union = ModelFactory.createUnion(union1, skosModel);
		  
		  /*Reasoner reasoner = PelletReasonerFactory.theInstance().create();
		  reasoner.setParameter(ReasonerVocabulary.PROPsetRDFSLevel, ReasonerVocabulary.RDFS_SIMPLE);*/
		  
		  InfModel inferMod = ModelFactory.createRDFSModel(union);
		  
		  PrintWriter out_file = new PrintWriter(rdfPath);
		  
		  inferMod.write(out_file, "TURTLE");
		  
	  }

	  static void createRDFDocs(File file)
	    throws IOException {
		Model model = ModelFactory.createDefaultModel();
		PrintWriter out_file = new PrintWriter(new FileWriter("tmp.ttl"));
	    // do not try to index files that cannot be read
		if (file.canRead()) {
		   if (file.isDirectory()) {
		      String[] files = file.list();
		      // an IO error could occur
		      if (files != null) {
		      for (int k = 0; k < files.length; k++) {
		    	  try {
			        	
			        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					    DocumentBuilder builder = factory.newDocumentBuilder();
					    System.out.println(file + "/" + files[k]);
					    org.w3c.dom.Document doc_parse = builder.parse(file + "/" + files[k]);

					    // make a new, empty document
					    Document doc = new Document();

				          // Add the path of the file as a field named "path".  Use a
				          // field that is indexed (i.e. searchable), but don't tokenize 
				          // the field into separate words and don't index term frequency
				          // or positional information:
				          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
				          doc.add(pathField);
			
				          // Add the last modified date of the file a field named "modified".
				          // Use a StoredField to return later its value as a response to a query.
				          // This indexes to milli-second resolution, which
				          // is often too fine.  You could instead create a number based on
				          // year/month/day/hour/minutes/seconds, down the resolution you require.
				          // For example the long value 2011021714 would mean
				          // February 17, 2011, 2-3 PM.
				          doc.add(new StoredField("modified", file.lastModified()));
			
				          // Add the contents of the file to a field named "contents".  Specify a Reader,
				          // so that the text of the file is tokenized and indexed, but not stored.
				          // Note that FileReader expects the file to be in UTF-8 encoding.
				          // If that's not the case searching for special characters will fail.
				          NodeList nodeList = doc_parse.getElementsByTagName("*");
				          for (int i=0; i<nodeList.getLength(); i++) {
				        	  org.w3c.dom.Node node = nodeList.item(i);
				        	  if (node.getNodeType() == Node.ELEMENT_NODE) {
				        		  doc.add(new TextField(node.getNodeName(), node.getTextContent(), Field.Store.YES));
				        		  //System.out.println(node.getNodeName() + " " + node.getTextContent());
				        	  }
				          }
				          
				          String title = doc.get("dc:title");
				          
				          String identifier = doc.get("dc:identifier");
				          
				          String[] subjects = doc.getValues("dc:subject");
				          for (int i=0; i<subjects.length; i++) {
				        	  String subject = subjects[i];
				          }         
				          
				          String type = doc.get("dc:type").toLowerCase();
				       
				          String description = doc.get("dc:description");
				          
				          String creator = doc.get("dc:creator");
				          
				          String[] contributors = doc.getValues("dc:contributor");
				          for (int i=0; i<contributors.length; i++) {
				        	  String contributor = contributors[i];
				          } 
				          
				          String relation = doc.get("dc:relation");
				          
				          String rights = doc.get("dc:rights");
				          
				          String publisher = doc.get("dc:publisher");
				          
				          
				          String[] formats = doc.getValues("dc:format");
				          for (int i=0; i<formats.length; i++) {
				        	  String format = formats[i];
				          }
				          
				          /*String[] languages = doc.getValues("dc:language");
				          for (int i=0; i<languages.length; i++) {
				        	  String language = languages[i];
				          }*/
				          
				          String language = doc.get("dc:language");
				          
				          String date = doc.get("dc:date");
				          
				          
				          
				          
				          Resource res = model.createResource(identifier.replace("\n", ""));
				          
				          Resource typeResource;
				          Property typeProperty = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				          
				          if (type.contains("tesis")) {
				        	  typeResource = model.createResource("http://www.example.org/#tesis");
				          } else if (type.contains("tfg")) {
				        	  typeResource = model.createResource("http://www.example.org/#tfg");
				          } else if (type.contains("tfm")) {
				        	  typeResource = model.createResource("http://www.example.org/#tfm");
				          } else {
				        	  typeResource = model.createResource("http://www.example.org/#pfc");
				          }
				          
				          res.addProperty(typeProperty, typeResource);
				          
				          Property titleProperty = model.createProperty("http://www.example.org/#title");
				          res.addProperty(titleProperty, title);
				          
				          Property contributorProperty = model.createProperty("http://www.example.org/#contributor");
				          for (int i=0; i<contributors.length; i++) {
				        	  String contributor = contributors[i];
				        	  Resource contributorResource = model.createResource(StringUtils.stripAccents(contributor).replace(" ", "-").replaceAll("[^a-zA-Z0-9]", ""));
				        	  String[] contributorArray = contributor.split(",");
				        	  String family_name = contributor.split(",")[0];
				        	  if (contributorArray.length > 1) {
				        		  String first_name = contributor.split(",")[1];
				        		  contributorResource.addProperty(FOAF.firstName, first_name);
				        	  }
					          
					          contributorResource.addProperty(FOAF.family_name, family_name);
					          res.addProperty(contributorProperty, contributorResource);
				          }
					      
				          
				          Property creatorProperty = model.createProperty("http://www.example.org/#creator");
				          Resource creatorResource = model.createResource(StringUtils.stripAccents(creator).replace(" ", "-").replaceAll("[^a-zA-Z0-9]", ""));
				          String[] creatorArray = creator.split(","); 
				          String family_name = creator.split(",")[0];
				          if (creatorArray.length > 1) {
				        	  String first_name = creator.split(",")[1];
				        	  creatorResource.addProperty(FOAF.firstName, first_name);
				          }
				          creatorResource.addProperty(FOAF.family_name, family_name);
				          
				          res.addProperty(creatorProperty, creatorResource);
				          
				          Property subjectProperty = model.createProperty("http://www.example.org/#subject");
				          
				          for (int i=0; i<subjects.length; i++) {
				        	  String subject = subjects[i];
				        	  Resource subjectResource = model.createResource(StringUtils.stripAccents(subject).replace(" ", "-").replaceAll("[^a-zA-Z0-9]", ""));
				        	  subjectResource.addProperty(SKOS.prefLabel, subject);
				        	  res.addProperty(subjectProperty, subjectResource);
				          }   
				          
				          Property descriptionProperty = model.createProperty("http://www.example.org/#description");
				          res.addProperty(descriptionProperty, description);
				          
				          if (publisher != null) {
				        	  Property publisherProperty = model.createProperty("http://www.example.org/#publisher");
				        	  res.addProperty(publisherProperty, publisher);
				          }
				          
				          if (date != null) {
				        	  Property dateProperty = model.createProperty("http://www.example.org/#date");
				        	  res.addProperty(dateProperty, date);
				          }
				          
				          if (language != null) {
				        	  Property languageProperty = model.createProperty("http://www.example.org/#language");
				        	  Resource languageResource = model.createResource();
				        	  languageResource.addProperty(DCTerms.language, language);
				        	  res.addProperty(languageProperty, languageResource);
				          }
				          
				          if (relation != null) {
				        	  Property relationProperty = model.createProperty("http://www.example.org/#relation");
				        	  Resource relationResource = model.createResource(StringUtils.stripAccents(relation).replace("\n", ""));
				        	  relationResource.addProperty(DCTerms.relation, relation);
				        	  res.addProperty(relationProperty, relationResource);
				          }
				          
				          if (rights != null) {
				        	  Property rightsProperty = model.createProperty("http://www.example.org/#rights");
				        	  Resource rightsResource = model.createResource(StringUtils.stripAccents(rights).replace("\n", ""));
				        	  rightsResource.addProperty(DCTerms.rights, rights);
				        	  res.addProperty(rightsProperty, rightsResource);
				          }
				          	          
				          
				        		  
				        } catch (Exception e) {
				        	e.printStackTrace();
				          
				        }
		    	  
		      	}
		      model.write(out_file, "TURTLE");
		        }
		      }         
	        
	      }
	  }
}