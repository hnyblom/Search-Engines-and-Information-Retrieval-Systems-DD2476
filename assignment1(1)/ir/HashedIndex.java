/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, LinkedList<Integer> offsets ) {
        if(index.containsKey(token)) {
            PostingsList postList = index.get(token);
            PostingsEntry postEntr = new PostingsEntry();
            postEntr.docID = docID;
            postEntr.offsets = offsets;
            postEntr.token = token;
            postList.add(postEntr);
            index.put(token, postList);

        }else {
            PostingsList postListNew = new PostingsList();
            PostingsEntry postEntr2 = new PostingsEntry();
            postEntr2.docID = docID;
            postEntr2.offsets = offsets;
            postEntr2.token = token;
            postListNew.add(postEntr2);
            index.put(token, postListNew);
        }
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        PostingsList postlist = index.get(token);
        return postlist;
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
