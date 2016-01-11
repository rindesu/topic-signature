package topic_signature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class Word {
	public String word;
	public double weight;
	public Word(String word, double weight) {
		this.word = word;
		this.weight = weight;
	}
}
public class TS {
    	static String[] filefold = {"relevant", "irrelevant"}; 
    	static int[] numOfDoc = new int[2];//numOfDoc[0]: the number of relevant documents, numOfDoc[1]: irrelevant documents
    	
    	static List<Word> words = new ArrayList<Word>();
	static List<String> dict = new ArrayList<String>();
	static List<String> stopWDict = new ArrayList<String>();
    	
	static int totalNumOfDoc, MAX_DOC_NUM; // totalNumOfDoc = numOfDoc[0] + numOfDoc[1], MAX_DOC_NUM = max(numOfDoc[0], numOfDoc[1])
	static int[][][] f;//f[0][w][d]: the frequency of word w in relevant document d
	
	
	
	public static void loadStopWDict() {
		File readFile = new File("stopWDict.txt");
		BufferedReader reader = null;
		String tempString;
		try {
			reader = new BufferedReader(new FileReader(readFile));
			while ((tempString = reader.readLine()) != null) {
				if (tempString.isEmpty())
					continue;
				stopWDict.add(tempString);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*int i;
		for (i = 0; i < stopWDict.size(); i++)
			System.out.println(stopWDict.get(i));*/
	}
	public static int binarySearch(List<String> list, String w, int beginIndex,int endIndex) {    
	       int midIndex = (beginIndex + endIndex) / 2;    
	       if(w.compareTo(list.get(beginIndex)) < 0 | w.compareTo(list.get(endIndex)) > 0 | beginIndex > endIndex){  
	           return -1;    
	       }  
	       if(w.compareTo(list.get(midIndex)) < 0){    
	           return binarySearch(list, w, beginIndex, midIndex - 1);    
	       }
	       else if(w.compareTo(list.get(midIndex)) > 0){    
	           return binarySearch(list, w, midIndex + 1, endIndex);    
	       }
	       else {    
	           return midIndex;
	       }    
	   }
	public static List<String> preprocess(String filename) {//stemming & stop word removing
		Stemmer.stemming(filename, stopWDict);
		List<String> doc = new ArrayList<String>();
		for (int i = 0; i < Stemmer.doc.size(); i++)
			doc.add(Stemmer.doc.get(i));
		return doc;
	}
	public static void createDict() throws IOException {//creating dictionary of all documents (including relevant and irrelevant), to be called for only once)
		for (int m = 0; m < 2; m++) {
			File file = new File(filefold[m]);
			File[] tempList = file.listFiles();
			numOfDoc[m] = tempList.length;
		
			for (int j = 0; j < tempList.length; j++) {
				//System.out.println(tempList[j].getName());
			    	List<String> doc = preprocess(filefold[m] + "/" + tempList[j].getName());
				for (int k = 0; k < doc.size(); k++) {
					if (!dict.contains(doc.get(k)) && !stopWDict.contains(doc.get(k))) {
						dict.add(doc.get(k));
					}
				}
			}
		}
		Collections.sort(dict, new Comparator<String>() {
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}
		});
		File writeFile = new File("ts_dict.txt");
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(writeFile));
			for (int j = 0; j < dict.size(); j++)
				writer.write(dict.get(j) + " ");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(dict.size());
	}
	public static void loadDict() {
		dict.clear();
		File readFile = new File("ts_dict.txt");
		BufferedReader reader = null;
		String tempString;
		try {
			reader = new BufferedReader(new FileReader(readFile));
			tempString = reader.readLine();
			String[] parts = tempString.split(" ");
			int i;
			for (i = 0; i < parts.length; i++) {
				dict.add(parts[i]);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println("dict_size: " + dict.size());
	}
	   
	public static void calcuF() throws IOException {
	    	MAX_DOC_NUM = -1;
		for (int m = 0; m < 2; m++) {
			File file = new File(filefold[m]);
			File[] tempList = file.listFiles();
			numOfDoc[m] = tempList.length;
			MAX_DOC_NUM = MAX_DOC_NUM < numOfDoc[m] ? numOfDoc[m] : MAX_DOC_NUM;
		}
		f = new int[2][dict.size()][MAX_DOC_NUM];
		totalNumOfDoc = numOfDoc[0] + numOfDoc[1];
		
		for (int m = 0; m < 2; m++) {
			File file = new File(filefold[m]);
			File[] tempList = file.listFiles();
			for (int j = 0; j < tempList.length; j++) {
			    	List<String> doc = preprocess(filefold[m] + "/" + tempList[j].getName());
				for (int k = 0; k < doc.size(); k++) {
				    int tempInt = binarySearch(dict, doc.get(k), 0, dict.size() - 1);
				    if (tempInt >= 0) {
					    //System.out.println(tempInt);
					f[m][tempInt][j]++;
				    }
				}
			}
		}
		
		// f[0][w][0]: the number of relevant documents that contains word w, f[1][w][0]: irrelevant documents
		for (int m = 0; m < 2; m++) {
			for (int j = 0; j < dict.size(); j++) {
			    f[m][j][0] = f[m][j][0] > 0 ? 1 : 0;
				for (int i = 0; i < MAX_DOC_NUM; i++) {
					if (f[m][j][i] > 0) {
						f[m][j][0]++;
					}
				}
			}
		}
	}
	public static double calcuScore(int index) {
		double weight = 0.0;
		int o11 = f[0][index][0], o21 = numOfDoc[0] - o11, o12 = f[1][index][0], o22 = numOfDoc[1] - o12;
		double p1 = (o11 + o22) * 1.0 / totalNumOfDoc, p2 = 1 - p1, p = numOfDoc[0] * 1.0 / totalNumOfDoc;
		//System.out.println(p1 + " " + p2 + " " + p);
		weight = numOfDoc[0] * Math.log(p + 0.0001) / Math.log(2) + numOfDoc[1] * Math.log(1 - p + 0.0001) / Math.log(2);//+ 0.0001: in case of zero 
		weight -= (o11 + o22) * Math.log(p1 + 0.0001) / Math.log(2) + (o12 + o21) * Math.log(p2 + 0.0001) / Math.log(2);
		weight = -2 * weight;
		return weight;
	}
	public static void sortWords() {//sorting topic signatures in the descending order of weights
		Collections.sort(words, new Comparator<Word>() {
			public int compare(Word w1, Word w2) {
				double tempDouble = w2.weight - w1.weight;
				if (tempDouble > 0)
					return 1;
				if (tempDouble < 0)
					return -1;
				return 0;
			}
		});
	}
	public static void outputTS() {
		File writeFile = new File("ts.txt");
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(writeFile));
			int i;
			for (i = 0; i < words.size(); i++) {
				writer.write(words.get(i).word + " " + words.get(i).weight + "\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) throws IOException {
		int i, j;
		loadStopWDict();
		createDict();
		//loadDict();
		calcuF();
		for (i = 0; i < dict.size(); i++) {
			double weight = calcuScore(i);
			words.add(new Word(dict.get(i), weight));
		}
		sortWords();
		outputTS();
	}
}
