package pagerank;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class PageRank {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory;
     */
    final static int MAX_NUMBER_OF_DOCS = 1000;

    /**
     *   Mapping from document names to document numbers.
     */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   The transition matrix. p[i][j] = the probability that the
     *   random surfer clicks from page i to page j.
     */
    double[][] p = new double[MAX_NUMBER_OF_DOCS][MAX_NUMBER_OF_DOCS];

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
     *   In the initializaton phase, we use a negative number to represent 
     *   that there is a direct link from a document to another.
     */
    final static double LINK = -1.0;
    
    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    
    /* --------------------------------------------- */


    public PageRank( String filename ) {
	int noOfDocs = readDocs( filename );
	if(noOfDocs>MAX_NUMBER_OF_DOCS){noOfDocs=MAX_NUMBER_OF_DOCS;}
	initiateProbabilityMatrix( noOfDocs );
	iterate( noOfDocs, 100 );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. When this method 
     *   finishes executing, <code>p[i][j] = LINK</code> if there is a direct
     *   link from i to j, and <code>p[i][j] = 0</code> otherwise.
     *   <p>
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
		    // Set the probability to LINK for now, to indicate that there is
		    // a link from d to otherDoc.
		    if ( p[fromdoc][otherDoc] >= 0 ) {
			p[fromdoc][otherDoc] = LINK;
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
     *   Initiates the probability matrix. 
     */
    void initiateProbabilityMatrix( int numberOfDocs ) {
    	//G = cP + (1-c)J

		double linkProb = 0;
		double docProb = 1.0/numberOfDocs;

		for(int i=0; i<numberOfDocs; i++){
			double nrOutLinks = out[i];
			if(nrOutLinks>0){
				linkProb = 1.0/nrOutLinks;
			}
			for(int j=0;j<numberOfDocs;j++){
				if(out[i]==0){
					p[i][j]=docProb;
				}else if(p[i][j]==(-1.0)){
					p[i][j]=(COMPLEMENT*linkProb)+(BORED*docProb);
				}else{
					p[i][j]=(BORED*docProb);
				}
			}
		}
    }

    /* --------------------------------------------- */

    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
	void iterate( int numberOfDocs, int maxIterations ) {
		double[] a = new double[numberOfDocs];
		double[] oldA;
		double diff = 1;
		int iterations = 0;
		a[0]=1.0;
		Arrays.fill(a, 1, numberOfDocs, 0.0);

		while((iterations<maxIterations) && (diff>EPSILON)) {
			oldA = Arrays.copyOf(a, a.length);

			a = Arrays.copyOf(matrixVectorMult(oldA, numberOfDocs), a.length);

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


	double[] matrixVectorMult(double[] a, int nrDocs){
		double sum;
		double[] newA = new double[nrDocs];

		for (int i=0;i<nrDocs;i++) {
			sum = 0.0;
			for(int j=0;j<nrDocs;j++){
				if(a[j]!=0) {
					sum += (a[j] * p[j][i]);
				}
			}
			newA[i]=sum;
		}
		return newA;
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
	    new PageRank( args[0] );
	}
    }
}