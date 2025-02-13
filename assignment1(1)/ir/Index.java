/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *  Defines some common data structures and methods that all types of
 *  index should implement.
 */
public interface Index {

    /** Mapping from document identifiers to document names. */
    public HashMap<Integer,String> docNames = new HashMap<Integer,String>();
    
    /** Mapping from document identifier to document length. */
    public HashMap<Integer,Integer> docLengths = new HashMap<Integer,Integer>();

    /** Inserts a token into the index. */
    public void insert( String token, int docID, LinkedList<Integer> offset );

    /** Returns the postings for a given term. */
    public PostingsList getPostings( String token );

    public HashMap<String,PostingsEntry> getTokens(int docID);

    /** This method is called on exit. */
    public void cleanup();

}

