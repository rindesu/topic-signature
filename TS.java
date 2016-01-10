package scitimeline;

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
	static int totalNumOfDoc, MAX_DOC_NUM = 343;
	static int[][][] f;
	static String[] filefold = {"summarization", "other"}; 
	static int[] numOfDoc = {303, 341};
	static List<Word> words = new ArrayList<Word>();
	static List<String> dict = new ArrayList<String>();
	
	static double cutoff = 10.83;
	
	public static List<String> preprocess(String filename) {//��һ���ĵ����дʸ���ȡ����ȥ�غ�ȥ���
		//��ȡ�ʸ�
		Stemmer.stemming(filename, Scitimeline.stopWDict);
		List<String> doc = new ArrayList<String>();
		for (int i = 0; i < Stemmer.doc.size(); i++)
			doc.add(Stemmer.doc.get(i));
		return doc;
	}
	public static void createDict(List<String> dict0) throws IOException {//����ĵ������з�ͣ�ôʵĴʸ���ɵĴʵ䣬���ֵ����źã�������dict.txt�С������ٵ���
		for (int m = 0; m < 2; m++) {
			String path = "ts/" + filefold[m];
			File file = new File(path);
			File[] tempList = file.listFiles();
		
			for (int j = 0; j < tempList.length; j++) {
				System.out.println(tempList[j].getName());
				BufferedReader reader = new BufferedReader(new FileReader(tempList[j]));
				String tempString;
				tempString = reader.readLine();
				while (tempString != null) {
					String[] parts = tempString.toLowerCase().split("\\W+");
					for (int k = 0; k < parts.length; k++) {
						if (!dict0.contains(parts[k]) && !Scitimeline.stopWDict.contains(parts[k])) {
							dict0.add(parts[k]);
						}
					}
					tempString = reader.readLine();
				}
				reader.close();
			}
		}
	}
	public static void loadDict() {//����ʵ�
		dict.clear();
		File readFile = new File("dict_ts.txt");
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
		System.out.println("dict_size: " + dict.size());
	}
	   
	
	public static void calcuF() throws IOException {
		for (int m = 0; m < 2; m++) {
			String path = "ts/ed/" + filefold[m];
			//String path = "ts/" + filefold[m];
			File file = new File(path);
			File[] tempList = file.listFiles();
			
			for (int j = 0; j < tempList.length; j++) {
				System.out.println(tempList[j].getName());
				BufferedReader reader = new BufferedReader(new FileReader(tempList[j]));
				//File writeFile = new File("ts/ed/" + filefold[m] + "/" + tempList[j].getName());
				//BufferedWriter writer = new BufferedWriter(new FileWriter(writeFile));
				String tempString = reader.readLine();
				//while (tempString != null) {
					//List<String> doc = preprocess(path + "/" + tempList[j].getName());
					String[] parts = tempString.split(" ");
					//System.out.println(parts[parts.length - 1]);
					
					for (int k = 0; k < parts.length; k++) {
						//writer.write(doc.get(k) + " ");
						//System.out.println(doc.get(k));
						//System.out.println(doc.get(k));
						//if (dict.indexOf(doc.get(k)) >= 0) {
						int tempInt = Scitimeline.binarySearch(dict, parts[k], 0, dict.size() - 1);
						if (tempInt >= 0) {
							f[m][tempInt][j]++;
						}
					}
					//tempString = reader.readLine();
				//}
				reader.close();
				//writer.close();
				if (j % 50 == 0 | j == tempList.length - 1) {
					File writeFile = new File("frequency_" + j + "_" + tempList[j].getName());
					BufferedWriter writer = null;
					try {
						writer = new BufferedWriter(new FileWriter(writeFile));
						int i;
						for (int q = 0; q < MAX_DOC_NUM; q++) {
							for (i = 0; i < dict.size(); i++) {
								writer.write(f[0][i][q] + " ");
							}
							writer.write("\n");
						}
						for (int q = 0; q < MAX_DOC_NUM; q++) {
							for (i = 0; i < dict.size(); i++) {
								writer.write(f[1][i][q] + " ");
							}
							writer.write("\n");
						}
						
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	public static double calcuScore(int index) {
		double weight = 0.0;
		int o11 = f[0][index][0], o21 = numOfDoc[0] - o11, o12 = f[1][index][0], o22 = numOfDoc[1] - o12;
		double p1 = (o11 + o22) * 1.0 / totalNumOfDoc, p2 = 1 - p1, p = numOfDoc[0] * 1.0 / totalNumOfDoc;
		System.out.println(p1 + " " + p2 + " " + p);
		weight = numOfDoc[0] * Math.log(p + 0.0001) / Math.log(2) + numOfDoc[1] * Math.log(1 - p + 0.0001) / Math.log(2);
		weight -= (o11 + o22) * Math.log(p1 + 0.0001) / Math.log(2) + (o12 + o21) * Math.log(p2 + 0.0001) / Math.log(2);
		weight = -2 * weight;
		return weight;
	}
	public static void sortWords() {
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
	public static void getTS() throws IOException {
		int i, j;
		
		///////////
		/*List<String> dict0 = new ArrayList<String>();
		createDict(dict0);
		Collections.sort(dict0, new Comparator<String>() {
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}
		});
		File writeFile = new File("dict_ts.txt");
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(writeFile));
			for (j = 0; j < dict0.size(); j++)
				writer.write(dict0.get(j) + " ");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(dict0.size());*/
		///////////////////
		
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		totalNumOfDoc = numOfDoc[0] + numOfDoc[1];
		double weight;
		loadDict();
		//System.out.println(dict.size());
		/*f = new int[2][dict.size()][MAX_DOC_NUM];
		calcuF();*/
		words.clear();
		
		f = new int[2][dict.size()][MAX_DOC_NUM];
		File readFile = new File("frequency_250_J02-3005.txt.txt");
		BufferedReader reader = null;
		String tempString;
		try {
			reader = new BufferedReader(new FileReader(readFile));
			for (int m = 0; m < 2; m++) {
				for (j = 0; j < MAX_DOC_NUM; j++) {
					tempString = reader.readLine();
					String[] parts = tempString.split(" ");
					for (i = 0; i < parts.length; i++) {
						if (Integer.parseInt(parts[i]) > 0) {
							f[m][i][0]++;
						}
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (i = 0; i < dict.size(); i++) {
			weight = calcuScore(i);
			words.add(new Word(dict.get(i), weight));
		}
		sortWords();
		outputTS();
	}
}
