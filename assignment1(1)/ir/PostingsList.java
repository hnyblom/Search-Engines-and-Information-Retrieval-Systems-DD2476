/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
    return list.get( i );
    }

    public void add(int index, PostingsEntry entry) {
        list.add(index, entry);
    }
    public void add(PostingsEntry entry) {
        list.add(entry);
    }
    public void remove(int index){ list.remove(index); }
    public Boolean removeAll(Collection col){return list.removeAll(col);}
    public ArrayList<PostingsEntry> getList() { return list; }

    public void sort() {
        list.stream().sorted(Comparator.comparing(PostingsEntry::getToken)).collect(Collectors.toCollection(ArrayList<PostingsEntry>::new));
    }
}

