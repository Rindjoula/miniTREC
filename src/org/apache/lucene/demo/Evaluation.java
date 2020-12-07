package org.apache.lucene.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Evaluation {
	private Evaluation() {}
	
	public static void main(String[] args) throws Exception {
		String qrels = null;
		String results = null;
		String output = null;
		
		/** Parse all the parameters **/
		
		for(int i = 0;i < args.length;i++) {
			if ("-qrels".equals(args[i])) {
				qrels = args[i+1];
		        i++;
		    } else if ("-results".equals(args[i])) {
		    	results = args[i+1];
		    	i++;
		    } else if ("-output".equals(args[i])) {
		    	output = args[i+1];
		    	i++;
		    }
		}
		
		if (qrels == null) {
			System.err.println("Must provide qrels file with -qrels");
			System.exit(1);
		}
		 
		if (results == null) {
			System.err.println("Must provide results file with -results");
			System.exit(1);
		}
		    
		if (output == null) {
			System.err.println("Must provide output file with -output");
			System.exit(1);
		}
		
		
		// Reading the qrels file (zaguanRels.txt)
		HashMap<String, ArrayList<HashMap<String, Integer>>> qrels_list = 
				new HashMap<String, ArrayList<HashMap<String, Integer>>>();
		 
		File qrels_file = new File(qrels);
		 
		BufferedReader br = new BufferedReader(new FileReader(qrels_file));
		 
		String line;
		while((line = br.readLine()) != null) {
			String[] split_line = line.split("\t");
			//System.out.println(line);
			String id_info = split_line[0];
			String doc_id = split_line[1];
			int rel = Integer.parseInt(split_line[2]);
			//System.out.println(id);
			//System.out.println(rel);
			if (!qrels_list.containsKey(id_info)) {
				qrels_list.put(id_info, 
						new ArrayList<HashMap<String, Integer>>());
			}
			 
			ArrayList<HashMap<String, Integer>> tmp = qrels_list.get(id_info);
			HashMap<String, Integer> docrel = new HashMap<String, Integer>();
			docrel.put(doc_id, rel);
			tmp.add(docrel);
			 
			qrels_list.replace(id_info, tmp);
		}
		 
		//System.out.println(qrels_list.get(1));
		//System.out.println(qrels_list.get(2));
		 
		br.close();
		 
		
		// Reading the results file of our system (results.txt)
		ArrayList<HashMap<String, String>> results_list = new ArrayList<HashMap<String, String>>();
		 
		File results_file = new File(results);
		 
		br = new BufferedReader(new FileReader (results_file));
		 
		while((line = br.readLine()) != null) {
			String[] split_line = line.split("\t");
			//System.out.println(line);
			String info_id = split_line[0];
			String doc_id = split_line[1];
			//System.out.println(info_id);
			//System.out.println(doc_id);
			HashMap<String, String> tmp = new HashMap<String, String>();
			tmp.put(info_id, doc_id);
			results_list.add(tmp);
		}
		 
		//System.out.println(results_list);
		
		br.close();
		
		int number_queries = qrels_list.size();
		
		PrintWriter writer = new PrintWriter(output, "UTF-8");
		double[] precisions = new double[number_queries];
		double[] recalls = new double[number_queries];
		double[] f1s = new double[number_queries];
		double[] precision_10_tab = new double[number_queries];
		
		int z = 0;
		
		HashMap<String, ArrayList<Double>> info_precisions_list = new HashMap<String, ArrayList<Double>>(); 
		
		for(String info_id: qrels_list.keySet()) {
			int tp = 0;
			int fp = 0;
			int fn = 0;
			int tp_10 = 0;
			int fp_10 = 0;
			double map = 0;
			writer.println("INFORMATION_NEED\t" + info_id);
			System.out.println("INFORMATION_NEED\t" + info_id);
			ArrayList<HashMap<String, Integer>> tmp_list = qrels_list.get(info_id);
			
			// count the number of docs in qrels
			int count_docs = 0;
			
			HashMap<Double, Double> prec_recall_list = new HashMap<Double, Double>();
			ArrayList<Double> precisions_list = new ArrayList<Double>();
			
			for(HashMap<String, Integer> docrel: tmp_list) {
				
				count_docs++;
				
				if (count_docs == 10) {
					tp_10 = tp;
					fp_10 = fp;
				}
				
				for(String doc_id: docrel.keySet()) {
					//System.out.println("DOC " + doc_id + "\tREL " + docrel.get(doc_id));
					
					// if the doc is in the results_list and is relevant in qrels.txt
					HashMap<String, String> k = new HashMap<String, String>();
					k.put(info_id, doc_id);
					if (docrel.get(doc_id) == 1 && results_list.contains(k)) {
						tp++;
						map += (double)tp/count_docs;
						precisions_list.add((double)(tp)/count_docs);
					}
					
					// if the doc is in the results_list but is not relevant in qrels.txt
					if (docrel.get(doc_id) == 0 && results_list.contains(k)) {
						fp++;
					}
					
					// if the doc is not in the results_list but it is relevant in qrels.txt
					if (docrel.get(doc_id) == 1 && !results_list.contains(k)) {
						fn++;
					}
				}
				
			}
			
			int total_docs_rel = tp + fn;
			
			double precision = (double)tp/(tp+fp);
			precisions[z] = precision;
			double recall = (double)tp/(tp+fn);
			recalls[z] = recall;
			double f1 = 2*(double)precision*recall / (precision+recall);
			f1s[z] = f1;
			
			System.out.println("tp = " + tp + "\tfp = " + fp + "\tfn = " + fn);
			
			System.out.println("PRECISION = " + String.format("%.3f", precision) + "\tRECALL = " + String.format("%.3f", recall));
			writer.println("precision\t" + String.format("%.3f", precision));
			writer.println("recall\t" + String.format("%.3f", recall));
			
			System.out.println("F1\t" + String.format("%.3f", f1));
			writer.println("F1\t" + String.format("%.3f", f1));
			
			double precision_10 = (double)tp_10/(tp_10+fp_10);
			precision_10_tab[z] = precision_10;
			System.out.println("prec@10\t" + String.format("%.3f", precision_10));
			writer.println("prec@10\t" + String.format("%.3f", precision_10));
			
			map /= tp;			
			System.out.println("average_precision\t" + String.format("%.3f", map));
			writer.println("average_precision\t" + String.format("%.3f", map));
			
			writer.println("recall_precision");
			System.out.println("recall_precision");
			double k = (double)1 / total_docs_rel; 
			
			for (int i=0; i<precisions_list.size(); i++) {
				double r = (double)(i+1)*k;
				double pr = precisions_list.get(i);
				System.out.println(String.format("%.3f", r) + "\t" + String.format("%.3f", pr));
				writer.println(String.format("%.3f", r) + "\t" + String.format("%.3f", pr));
				
				prec_recall_list.put(r, pr);
			}
			
			//System.out.println(precisions_list);
			System.out.println(prec_recall_list);
			
			writer.println("interpolated_recall_precision");
			System.out.println("interpolated_recall_precision");
			for (int i=0; i<=10; i++) {
				double r = (double) i / 10;
				double max = 0;
				for(double rec: prec_recall_list.keySet()) {
					if (rec > r && prec_recall_list.get(rec) > max) {
						max = prec_recall_list.get(rec);
					}
				}
				System.out.println(r + "\t" + String.format("%.3f", max));
				writer.println(r + "\t" + String.format("%.3f", max));
			}
			
			writer.println();
			z++;
			
			info_precisions_list.put(info_id, precisions_list);
		}
		
		writer.println("TOTAL");
		double global_precision = 0;
		double global_recall = 0;
		double global_f1 = 0;
		double global_prec10 = 0;
				
		for (int i=0; i<number_queries; i++) {
			global_precision += precisions[i];
			global_recall += recalls[i];
			global_f1 += f1s[i];
			global_prec10 += precision_10_tab[i];
		}
		
		global_precision /= number_queries;
		global_recall /= number_queries;
		global_f1 /= number_queries;
		global_prec10 /= number_queries;
			
		writer.println("precision\t" + global_precision);
		writer.println("recall\t" + global_recall);
		writer.println("F1\t" + global_f1);
		writer.println("prec@10\t" + global_prec10);
		
		/* Compute MAP */
		
		writer.println("interpolated_recall_precision");
		
		/* Compute all the interpolated recall precisions */
		
		Double[] global_inter_rec_prec = new Double[10];
		for (String info_id: qrels_list.keySet()) {
			ArrayList<Double> list_prec = info_precisions_list.get(info_id);
			System.out.println("HERE:" + list_prec);
			for (int i=0; i<list_prec.size(); i++) {
				global_inter_rec_prec[i] += list_prec.get(i);
			}
		}
		
		for (int i=0; i<10; i++) {
			double r = (double) i / 10; 
			double pr = global_inter_rec_prec[i] / number_queries;
			writer.println(String.format("%.3f", r) + "\t" + String.format("%.3f", pr));
		}
			 
		writer.close();
	
		 		 
	}
}
