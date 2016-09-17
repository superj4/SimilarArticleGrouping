import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class P2 
{
	static final int shingleSize = 9;
	static final int perm = 8000;
	//for permulation number of 8000, band number is 400, row per band is 20
	static final int bandNum = 400;
	//this bucketNum is used as divisor in LSH calculation
	static final int bucketNum = 888887;
	public static void main(String args[])
	{
		//read original input
		List<String> docName = new ArrayList<String>();
		Scanner sc = new Scanner(System.in);	
		String input;
		while(sc.hasNextLine()) {
			input = sc.nextLine();
			String[] tokens = input.split("\\s+");
			for (String token : tokens) {
				docName.add(token);	
				}
			}
			
		sc.close();
		
		List<List<Integer>> docs = new ArrayList<List<Integer>>();
		for (int i = 0; i < docName.size(); i++)
		{
			
			String fileTitle = docName.get(i);
			BufferedReader br = null;
			try
			{
				String sCurrentLine;
				br = new BufferedReader(new FileReader("/home/mwang2/test/coen281/" + fileTitle));
				StringBuilder sb = new StringBuilder();
				while ((sCurrentLine = br.readLine()) != null) 
				{
					sb.append(sCurrentLine);
				}
				String aFile = sb.toString();
				aFile = aFile.replaceAll("\\d","");
				List<String> docSet = shingleGenerator(aFile,shingleSize);
				List<Integer>compactDocSet = StringToInt(docSet);
				
				docs.add(compactDocSet);
			}catch (Exception e){//Catch exception if any
			      System.err.println("Failed to read file: " + fileTitle);
			}finally{
			     try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  
			}	
		}
		// all unique shingles
		List<Integer> shingleList = shingleSet(docs);		
		List<List<Integer>> signature = minHash(docs, shingleList, perm);
		List<List<Integer>> buckets = LSH(signature, bandNum, docName);
				
		for(int i = 0; i < buckets.size(); i++)
		{
			System.out. println("Group " + i + ": ");
			StringBuilder sb = new StringBuilder();
			for (int docIndex : buckets.get(i)) {
				sb.append(' ');
				sb.append(docName.get(docIndex));
			}
			System.out.println(sb.toString());
		}

	}
	//this method convert a file into a list of shingles
	public static List<String> shingleGenerator(String file, int k)
	{
		int len = file.length();
		List<String> kShingle = new ArrayList<String>();
		for (int i = 0; i < len - k + 1; i++)
		{
			String shingle = file.substring(i, i+k);
			kShingle.add(shingle);
		}
		return kShingle;
	}
	//this method hash the shingles from strings into integers
	public static List<Integer> StringToInt (List<String> docSet)
	{
		List<Integer> compactSet = new ArrayList<Integer>();		
		for(int i = 0; i < docSet.size(); i++) {
			compactSet.add(docSet.get(i).hashCode());
		}
		
		return compactSet;
	}
	//this method find all unique shingles from a list of shingle sets
	public static List<Integer> shingleSet(List<List<Integer>> docs)
	{
		Set<Integer> shingleSet = new HashSet<Integer>();
		for (int i = 0; i < docs.size(); i++)
		{
			for (int j = 0; j < docs.get(i).size(); j++)
			{
				int value = docs.get(i).get(j);
				shingleSet.add(value);
			}
		}
		List<Integer> shingleList = new ArrayList<Integer>(shingleSet);
		return shingleList;
	}
	//this method does the minHash function and output signatures
	public static List<List<Integer>> minHash(List<List<Integer>> docs, 
			List<Integer> shingleList, int permutationNum)
	{
		int docNum = docs.size();
		int shingleNum = shingleList.size();
		List<List<Integer>> signature = initiateSignature(docNum, permutationNum);
		List<List<Integer>> permutations = permGenerator(shingleNum, permutationNum);
		for (int shingIndex = 0; shingIndex < shingleNum; shingIndex++)
		{
			for (int permIndex = 0; permIndex < permutationNum; permIndex++)
			{
				int reference = permutations.get(permIndex).get(shingIndex);
				for (int docIndex = 0; docIndex < docNum; docIndex++)
				{
					if (docs.get(docIndex).contains(shingleList.get(shingIndex)))
					{
						if (signature.get(permIndex).get(docIndex) > reference)
						{
							signature.get(permIndex).set(docIndex, reference);
						}
					}
				}
			}
		}
		return signature;
	}	
	//this function construct a signature with infinite value for each cell
	public static List<List<Integer>> initiateSignature(int docSize, int permutation)
	{
		List<List<Integer>> signature = new ArrayList<List<Integer>>();
		for (int j = 0; j < permutation; j++)
		{
			List<Integer> row = new ArrayList<Integer>();
			for (int i = 0; i < docSize; i++)
			{
				row.add(Integer.MAX_VALUE);
			}
			signature.add(row);
		}
		return signature;
	}
	//this function generate a list of permutations
	public static List<List<Integer>> permGenerator (int shingleSize, int permulation)
	{
		List<List<Integer>> permutations = new ArrayList<List<Integer>>();
		for (int i = 0; i < permulation; i++)
		{
			List<Integer> perm = new ArrayList<Integer>();
			for (int j = 0; j < shingleSize; j++)
			{
				perm.add(j);
			}
			shuffleList(perm);
			permutations.add(perm);
		}
		return permutations;	
	}
	//this function do random shuffle for a list of integers
	static void shuffleList(List<Integer> ar)
	{
		Random rnd = ThreadLocalRandom.current();
		for (int i = ar.size() - 1; i > 0; i--)
		{
			int index = rnd.nextInt(i + 1);
			// Simple swap
			int a = ar.get(index);
			int temp = ar.get(i);
			ar.set(index, temp);
			ar.set(i,a);
		}
	}

	//this function do local sensitive hash, and candidate pairs are hashed into the same bucket
	public static List<List<Integer>> LSH(List<List<Integer>> signature, int numBands, List<String> docName)
	{
		int row = signature.size()/numBands;
		List<List<Integer>> allBuckets = new ArrayList<List<Integer>>();
		
		for (int i = 0; i < numBands; i++)
		{
			List<List<Integer>> chunck = new ArrayList<List<Integer>>();
			for (int j = 0; j < row; j++)
			{
				int rowIndex = i*row+j;
				List<Integer> line = signature.get(rowIndex);
				chunck.add(line);
			}
			Map<Integer, List<Integer>> table = HashTable(chunck, bucketNum);
	
			for(int key : table.keySet()) {
				List<Integer> aBucket = table.get(key);
				if(aBucket.size()>=2 && !containBucket(allBuckets, aBucket))
				{
					allBuckets.add(aBucket);			
				}	
			}
		}
		return allBuckets;
	}
	//this method hashes the signature of one band into a hash map
	public static Map<Integer, List<Integer>> HashTable(List<List<Integer>> chunck, int bucket)
	{
		int row = chunck.size();
		int col = chunck.get(0).size();
		Map<Integer, List<Integer>> hashTable = new HashMap<Integer, List<Integer>>();
		for (int key = 0; key < bucket; key++)
		{
			hashTable.put(key, new ArrayList<Integer>());
		}
		for(int docIndex = 0; docIndex < col; docIndex++)
		{
			int value = 0;	
			StringBuilder sb = new StringBuilder();
			for( int i = 0; i < row; i++)
			{
				value = chunck.get(i).get(docIndex);
				sb.append(value);
			}
			String s = sb.toString();
			int hash = s.hashCode();
			int bucketIndex = Math.abs(hash%bucket);
			if(!hashTable.get(bucketIndex).contains(docIndex))
			{
				hashTable.get(bucketIndex).add(docIndex);
			}
		}
		return hashTable;		
	}
	//this method calculates the threshold, false positive probability, and true positive probability
	public static void thresProb (int bandNum, int rowNum)
	{
		for (int i = 0; i < 10; i++)
		{
			double threshold = (double)(i)/10;
			double falsePositive = Math.pow((1-Math.pow(threshold, rowNum)), bandNum);
			double prob = 1-falsePositive;
			System.out.print(threshold+" "+falsePositive+" "+prob);
			System.out.println(" ");
		}
	}
	
	public static boolean containBucket(List<List<Integer>> allBuckets, List<Integer> bucket) {
		Collections.sort(bucket);
		for (List<Integer> oneBucket : allBuckets) {
			if (bucket.size() == oneBucket.size()) {
				Collections.sort(oneBucket);
				boolean yes = true;
				for (int i = 0; i < bucket.size(); i++) {
					if (oneBucket.get(i) != bucket.get(i)) {
						yes = false;
					}
				}
				if (yes) {
					return true;
				}
			}
		}
		return false;
	}
}
