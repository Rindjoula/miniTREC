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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.soap.Node;
import org.w3c.dom.Node;

/** Index all text files under a directory.*/
public class IndexFiles {
  
private IndexFiles() {}
	
	/** Index all text files under a directory. */
	  public static void main(String[] args) {
	    String usage = "java org.apache.lucene.demo.IndexFiles"
	                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
	                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
	                 + "in INDEX_PATH that can be searched with SearchFiles";
	    String indexPath = "index";
	    String docsPath = null;
	    boolean create = true;
	    for(int i=0;i<args.length;i++) {
	      if ("-index".equals(args[i])) {
	        indexPath = args[i+1];
	        i++;
	      } else if ("-docs".equals(args[i])) {
	        docsPath = args[i+1];
	        i++;
	      } else if ("-update".equals(args[i])) {
	        create = false;
	      }
	    }

	    if (docsPath == null) {
	      System.err.println("Usage: " + usage);
	      System.exit(1);
	    }

	    final File docDir = new File(docsPath);
	    if (!docDir.exists() || !docDir.canRead()) {
	      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
	      System.exit(1);
	    }
	    
	    Date start = new Date();
	    try {
	      System.out.println("Indexing to directory '" + indexPath + "'...");

	      Directory dir = FSDirectory.open(Paths.get(indexPath));
	      Analyzer analyzer = new SpanishAnalyzer2();
	      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

	      iwc.setOpenMode(OpenMode.CREATE);

	      IndexWriter writer = new IndexWriter(dir, iwc);
	      indexDocs(writer, docDir);

	      writer.close();

	      Date end = new Date();
	      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

	    } catch (IOException e) {
	      System.out.println(" caught a " + e.getClass() +
	       "\n with message: " + e.getMessage());
	    }
	  }

	  static void indexDocs(IndexWriter writer, File file)
	    throws IOException {
	    // do not try to index files that cannot be read
	    if (file.canRead()) {
	      if (file.isDirectory()) {
	        String[] files = file.list();
	        // an IO error could occur
	        if (files != null) {
	          for (int i = 0; i < files.length; i++) {
	            indexDocs(writer, new File(file, files[i]));
	          }
	        }
	      } else {

	        FileInputStream fis;
	        try {
	          fis = new FileInputStream(file);
	        } catch (FileNotFoundException fnfe) {
	          // at least on windows, some temporary files raise this exception with an "access denied" message
	          // checking if the file can be read doesn't help
	          return;
	        }

	        try {
	        	
	        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			    DocumentBuilder builder = factory.newDocumentBuilder();
			    org.w3c.dom.Document doc_parse = builder.parse(file);

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
		          doc.add(new TextField("title", title, Field.Store.YES));
		          
		          String identifier = doc.get("dc:identifier");
		          doc.add(new StringField("identifier", identifier, Field.Store.YES));
		          
		          String[] subjects = doc.getValues("dc:subject");
		          for (int i=0; i<subjects.length; i++) {
		        	  String subject = subjects[i];
		        	  doc.add(new TextField("subject", subject, Field.Store.YES));
		          }         
		          
		          String type = doc.get("dc:type");
		          if (type != null){
		        	  doc.add(new StringField("type", type, Field.Store.YES));
		          }
		          
		          String description = doc.get("dc:description");
		          doc.add(new TextField("description", description, Field.Store.YES));
		          
		          String creator = doc.get("dc:creator");
		          if (creator != null) {
		        	  doc.add(new TextField("creator", creator, Field.Store.YES));
		          }
		          
		          String[] contributors = doc.getValues("dc:contributor");
		          for (int i=0; i<contributors.length; i++) {
		        	  String contributor = contributors[i];
		        	  doc.add(new TextField("contributor", contributor, Field.Store.YES));
		          } 
		          
		          String relation = doc.get("relation");
		          if (relation != null) {
		        	  doc.add(new TextField("relation", relation, Field.Store.YES));
		          }
		          
		          String rights = doc.get("rights");
		          if (rights != null) {
		        	  doc.add(new TextField("rights", rights, Field.Store.YES));
		          }
		          
		          String publisher = doc.get("dc:publisher");
		          if (publisher != null) {
		        	  doc.add(new TextField("publisher", publisher, Field.Store.YES));
		          }
		          
		          String[] formats = doc.getValues("dc:format");
		          for (int i=0; i<formats.length; i++) {
		        	  String format = formats[i];
		        	  doc.add(new TextField("format", format, Field.Store.YES));
		        	  //System.out.println("adding " + format);
		          }
		          
		          String[] languages = doc.getValues("dc:language");
		          for (int i=0; i<languages.length; i++) {
		        	  String language = languages[i];
		        	  doc.add(new TextField("language", language, Field.Store.YES));
		          }
		          
		          String date = doc.get("dc:date");
		          if(date != null) {
		        	  doc.add(new StringField("date", date, Field.Store.YES));
		          }
		          
		          
		          String lowerCorner = doc.get("ows:LowerCorner");
		          String upperCorner = doc.get("ows:UpperCorner");
	
		          if (lowerCorner != null && upperCorner != null) {
		        	  String[] lowerCornerSplit = lowerCorner.split(" ");
		        	  double west = Double.parseDouble(lowerCornerSplit[0]);
			          double south = Double.parseDouble(lowerCornerSplit[1]);
			          String[] upperCornerSplit = upperCorner.split(" ");
		        	  double east = Double.parseDouble(upperCornerSplit[0]);
			          double north = Double.parseDouble(upperCornerSplit[1]);
			          DoublePoint westField = new DoublePoint("west", west);
			          DoublePoint eastField = new DoublePoint("east", east);
			          DoublePoint southField = new DoublePoint("south", south);
			          DoublePoint northField = new DoublePoint("north", north);
			          
			          doc.add(westField);
			          doc.add(eastField);
			          doc.add(southField);
			          doc.add(northField);
			          
		          }
		          
		          
		          
		          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
		            // New index, so we just add the document (no old document can be there):
		            System.out.println("adding " + file);
		            writer.addDocument(doc);
		          } else {
		            // Existing index (an old copy of this document may have been indexed) so 
		            // we use updateDocument instead to replace the old one matching the exact 
		            // path, if present:
		            System.out.println("updating " + file);
		            writer.updateDocument(new Term("path", file.getPath()), doc);
		          }
		        } catch (Exception e) {
		        	e.printStackTrace();
		          
		        } finally {
		          fis.close();
		        }
	      }
	    }
	  }
}