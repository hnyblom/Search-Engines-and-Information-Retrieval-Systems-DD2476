package pagerank;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class PageRankSparse {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;
    final static double COMPLEMENT = 0.85;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

       
    /* --------------------------------------------- */


    public PageRankSparse( String filename ) {
	int noOfDocs = readDocs( filename );
	iterate( noOfDocs, 1000 );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. 
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
		int index = line.indexOf( ";" );
		String title = line.substring( 0, index );
		Integer fromdoc = docNumber.get( title );
		//  Have we seen this document before?
		if ( fromdoc == null ) {	
		    // This is a previously unseen doc, so add it to the table.
		    fromdoc = fileIndex++;
		    docNumber.put( title, fromdoc );
		    docName[fromdoc] = title;
		}
		// Check all outlinks.
		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    String otherTitle = tok.nextToken();
		    Integer otherDoc = docNumber.get( otherTitle );
		    if ( otherDoc == null ) {
			// This is a previousy unseen doc, so add it to the table.
			otherDoc = fileIndex++;
			docNumber.put( otherTitle, otherDoc );
			docName[otherDoc] = otherTitle;
		    }
		    // Set the probability to 0 for now, to indicate that there is
		    // a link from fromdoc to otherDoc.
		    if ( link.get(fromdoc) == null ) {
			link.put(fromdoc, new HashMap<Integer,Boolean>());
		    }
		    if ( link.get(fromdoc).get(otherDoc) == null ) {
			link.get(fromdoc).put( otherDoc, true );
			out[fromdoc]++;
		    }
		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		System.err.print( "done. " );
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }


    /* --------------------------------------------- */

    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate( int numberOfDocs, int maxIterations ) {

		//G = cP + (1-c)J

		double linkProb = 0;
		double docProb = 1.0/numberOfDocs;

		double[] a = new double[numberOfDocs];
		double[] oldA;
		double diff = 1;
		double probability=0;
		int iterations = 0;
		double sum;
		a[0]=1.0;
		Arrays.fill(a, 1, numberOfDocs, 0.0);

		while((iterations<maxIterations) && (diff>EPSILON)) {
			System.out.println("Iterations: "+ iterations);
			oldA = Arrays.copyOf(a, a.length);

			for(int i=0; i<numberOfDocs; i++){
				sum = 0;
				for(int j=0; j<numberOfDocs; j++){
					double nrOutLinks = out[i];
					if(nrOutLinks>0){
						linkProb = 1.0/nrOutLinks;
					}
					if(nrOutLinks==0){
						probability = docProb;
					}else if(link.get(i).get(j)!=null){
						probability = (COMPLEMENT*linkProb)+(BORED*docProb);
					}else{
						probability = (BORED*docProb);
					}
					sum += (oldA[j] * probability);
				}
				a[i] = sum;
			}

			//a = Arrays.copyOf(matrixVectorMult(oldA, numberOfDocs), a.length);

			diff = compareVectors(oldA, a);

			iterations++;
		}
		System.out.println("Iterations " + iterations);

		if(diff<=EPSILON){
			System.out.println("Diffout "+ diff);
		}
		//Max interations is 100
		printRankings(a);

    }

	double compareVectors(double[] oldV, double[] newV){
		double answer = 0.0;
		for(int i=0;i<oldV.length;i++){
			answer += Math.abs(oldV[i]-newV[i]);
		}
		return answer;
	}

	void printRankings(double[] a){
		//Convert array to hashmap
		HashMap<Integer, Double> rankings = new HashMap<>();
		for(int i=0;i<a.length;i++){
			rankings.put(i,a[i]);
		}
		//Sort rankings
		Map<Integer, Double> sortedMap = rankings.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2)->e1, LinkedHashMap::new));
		//Map<Integer, Double> sortedMap = rankings.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2)->e1, LinkedHashMap::new));

		//Get only 30 highest rankings
		Map<Integer, Double> subMap = sortedMap.entrySet().stream().limit(30).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2)->e1, LinkedHashMap::new));

		//Print 30 highest ranked
		subMap.forEach((key, value) -> {
			System.out.println("Key : " + docName[key] + " Value : " + value);
		});

	}
    /* --------------------------------------------- */


    public static void main( String[] args ) {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRankSparse( args[0] );
	}
    }
}