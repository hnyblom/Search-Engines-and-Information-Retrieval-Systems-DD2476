/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.lang.reflect.Array;
import java.util.*;
import java.nio.charset.*;
import java.io.*;
import java.util.stream.Collectors;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {
    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;

    double gamma = 0;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
        //TODO: Check if QueryType is ranked query?

        HashMap<String, Double> newWeights = new HashMap<>();
        ArrayList<String> queryStrings = new ArrayList<>();
        for (QueryTerm t:queryterm) {
            queryStrings.add(t.term);
        }
        Searcher searcher = engine.searcher;
        Index index = engine.index;

        //Calculate length of query vector
        double vectorLen = 0.0;
        for(int m=0; m<queryterm.size();m++){
            String term  = queryterm.get(m).term;
            vectorLen += searcher.queryWeights.get(term);
        }
        //Normalize query vector
        for(int l=0; l<queryterm.size();l++){
            String term  = queryterm.get(l).term;
            double normalized = searcher.queryWeights.get(term)/vectorLen;
            searcher.queryWeights.put(term,normalized);
        }
        //Calculate query vector
        double origQuery = 0.0;
        for(int k=0; k<queryterm.size();k++){
            String term  = queryterm.get(k).term;
            origQuery += searcher.queryWeights.get(term);
            newWeights.put(term,(alpha*origQuery));
        }

        //Count number of relevant documents
        int relevant = 0;
        for(int j=0;j<docIsRelevant.length;j++){
            if(docIsRelevant[j]){
                ++relevant;
            }
        }

        //Calculate document vector
        //Sum up document vectors of all relevant documents
        double relevantSum =0;
        Integer[] documents = index.docNames.keySet().toArray(new Integer[0]);
        //For all relevant documents
        for(int n=0;n<docIsRelevant.length;n++){
            if(docIsRelevant[n]){
                int docID = results.get(n).docID;
                //Get all terms in document
                HashMap<String, PostingsEntry> tokensMap = index.getTokens(docID);
                String[] tokens = tokensMap.keySet().toArray(new String[0]);
                int docLength = tokens.length;
                //Sum up weight for each term (given by tf-idf)
                for (String token : tokens) {
                    PostingsEntry pEntr = tokensMap.get(token);
                    int docFrequency = searcher.docFrequency(token);
                    double weight = searcher.tfIdf(pEntr, docFrequency);
                    double normalized = weight / docLength;
                    relevantSum += normalized;
                    double newWeight = beta * (1.0 / Math.abs(relevant) * relevantSum);
                    if (newWeights.containsKey(token)) {
                        newWeights.put(token, newWeights.get(token) + newWeight);
                    } else {
                        newWeights.put(token, newWeight);
                    }
                }
            }
        }
        //Sort hashmap with new weights for new terms
        Map<String, Double> sortedMap = newWeights.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2)->e1, LinkedHashMap::new));
        //Map<String, Double> subMap = sortedMap.entrySet().stream().limit(1).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2)->e1, LinkedHashMap::new));

        String[] newTerm = sortedMap.keySet().toArray(new String[0]);
        /*for(int i=0;i<3;i++){
            QueryTerm newQTerm = new QueryTerm(newTerm[i], sortedMap.get(newTerm[i]));
            queryterm.add(newQTerm);
        }*/
        QueryTerm newQTerm = new QueryTerm(newTerm[0], sortedMap.get(newTerm[0]));
        queryterm.add(newQTerm);
        System.out.println("New Term: " + newTerm[0]);

    }
}


