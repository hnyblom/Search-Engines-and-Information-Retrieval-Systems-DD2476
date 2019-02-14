/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    Indexer indexer;

    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.indexer = indexer;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType ) {
        PostingsList answer = new PostingsList();

        if (queryType == QueryType.INTERSECTION_QUERY) {
            if (query.size() == 1) {
                answer = unique(index.getPostings(query.queryterm.get(0).term));
            }else {
                answer = intersection(query, queryType);
            }
        }else if (queryType==QueryType.PHRASE_QUERY){
            if (query.size() == 1) {
                answer = unique(index.getPostings(query.queryterm.get(0).term));
            }else {
                //Get list of first word entries in docs with all other query terms
                PostingsList inters = intersection(query, queryType);
                //PostingsList phrase = phrase(inters, query);
                //answer = unique(phrase);
                answer = inters;
            }
        }else if (queryType==QueryType.RANKED_QUERY){
            PostingsList theList = new PostingsList();

            //Number of documents in corpus which contains term
            if(query.size() ==1){
                theList = unique(index.getPostings(query.queryterm.get(0).term));
            }else{
                theList = multiWord(query);
                int size =theList.size();
                if(theList.size()>0) {
                    //Go through all docs in corpus that contains the term
                    for(int i=0; i<size; i++){
                        PostingsEntry entry = theList.get(i);
                        double score = tfIdf(entry, size);
                        entry.score = score;
                    }
                }
                Collections.sort(theList.getList());
                answer = theList;
            }
        }
        return answer;
    }

    //Helpers
    public void cosineScore(Query query){
        ArrayList Length = new ArrayList();
        for(Query.QueryTerm term : query.queryterm){
            //Calculate w(t,q) - weight
            //double weight = tfIdf(term)
            //Fetch postings list
            PostingsList list = index.getPostings(term.term);
            //For each pair(d, tf(t,d)) (document, term-frequency) in postings list
            for(int i=0;i<list.size();i++){
                float score = indexer.getScore(list.get(i).docID);
                //scores[d] += wf(t,d) x w (t,q)
                score += tfIdf(list.get(i), query.size());


            }
        }
    }
    public double tfIdf(PostingsEntry entry, int size){
        //Number of docs in corpus
        int n = 17481;
        double idf = Math.log(n/size);

        //tf.d.t - Check how many times the term occurs in the document
        int duplicateVal = duplicates(entry.token, entry.docID);

        //len.d - Check number of words in the document
        int length = index.docLengths.get(entry.docID);

        return (duplicateVal*idf)/length;

        //entry.score =+ (duplicateVal*idf)/length;
    }

    public int duplicates(String token, int docID){
        PostingsList list = index.getPostings(token);
        int nr=0;
        for(int i=0; i<list.size();i++){
            if(list.get(i).docID == docID){
                nr++;
            }
        }
        return nr;
    }


    public PostingsList multiWord(Query query){
        PostingsList answer = new PostingsList();
        PostingsList oldPostList = new PostingsList();

        for (int i = 0; i < query.size()-1; i = i + 2) {
            PostingsList pi = unique(index.getPostings(query.queryterm.get(i).term));
            PostingsList pi1 = unique(index.getPostings(query.queryterm.get(i + 1).term));

            //No earlier terms.
            if (oldPostList.size() == 0) {
                oldPostList = combine(pi, pi1);
                answer = oldPostList;

                //2 or more earlier joined terms.
            } else {
                PostingsList tempPostList = combine(pi, pi1);
                answer = combine(oldPostList, tempPostList);
                oldPostList = answer;
            }

            //Odd number of terms, last term.
            if (i + 3 == query.size()) {
                PostingsList pOdd = unique(index.getPostings(query.queryterm.get(i + 2).term));
                answer = combine(oldPostList, pOdd);
            }
        }
        return answer;
    }

    public PostingsList intersection(Query query, QueryType queryType){
        System.err.println("Intersection");
        PostingsList answer = new PostingsList();
        PostingsList oldPostList = new PostingsList();

        for (int i = 0; i < query.size()-1; i = i + 2) {
            PostingsList pi = index.getPostings(query.queryterm.get(i).term);
            PostingsList pi1 = index.getPostings(query.queryterm.get(i + 1).term);

            if (queryType == QueryType.INTERSECTION_QUERY) {
                pi = unique(pi);
                pi1 = unique(pi1);

                //No earlier terms.
                if (oldPostList.size() == 0) {
                    oldPostList = intersect(pi, pi1);
                    answer = oldPostList;

                    //2 or more earlier joined terms.
                } else {
                    PostingsList tempPostList = intersect(pi, pi1);
                    answer = intersect(oldPostList, tempPostList);
                    oldPostList = answer;
                }

                //Odd number of terms, last term.
                if (i + 3 == query.size()) {
                    PostingsList pOdd = index.getPostings(query.queryterm.get(i + 2).term);
                    pOdd = unique(pOdd);
                    answer = intersect(oldPostList, pOdd);
                }

            }else if(queryType==QueryType.PHRASE_QUERY){
                answer = phrase(query);
                answer = unique(answer);
                //No earlier terms.
                /*if (oldPostList.size() == 0) {
                    oldPostList = positionalIntersect(pi, pi1, i+1);
                    answer = oldPostList;

                    //2 or more earlier joined terms.
                } else {
                    PostingsList tempPostList = positionalIntersect(pi, pi1, i+1);
                    answer = positionalIntersect(oldPostList, tempPostList, i+2);
                    oldPostList = answer;
                }

                //Odd number of terms, last term.
                if (i + 3 == query.size()) {
                    PostingsList pOdd = index.getPostings(query.queryterm.get(i + 2).term);
                    answer = positionalIntersect(oldPostList, pOdd, i+3);
                }*/
            }
        }
        return answer;
    }

    public PostingsList phrase(Query query){
        PostingsList answer;
        ArrayList<Query.QueryTerm> terms = query.queryterm;
        int termSize = terms.size();
        answer = index.getPostings(terms.get(0).term);
        int k = 1;
        for(int t =1; t<termSize; t++){
            String term = terms.get(t).term;
            PostingsList nextDocs = index.getPostings(term);
            answer = positionalIntersect(answer, nextDocs, k);
            k++;
        }
        return answer;
    }

    public PostingsList combine(PostingsList p1, PostingsList p2) {
        for(int i=0;i<p2.size();i++){
            p1.add(p2.get(i));
        }
        PostingsList answer = unique(p1);
        return answer;
    }

    /**
     * @param p1
     * @param p2
     * @return Postingslist with entries of first word in a document with all other words from the query.
     */
    public PostingsList intersect(PostingsList p1, PostingsList p2) {
        System.err.println("Intersect");
        PostingsList answer = new PostingsList();
        if (p1.size() != 0 && p2.size() != 0) {
            int i = 0;
            int j = 0;
            while (i<p1.size()&&j<p2.size()){
                PostingsEntry e1 = p1.get(i);
                PostingsEntry e2 = p2.get(j);
                if(e1.docID==e2.docID){
                    answer.add(e1);
                    i++;
                    j++;
                }else if(e1.docID<e2.docID){
                    i++;
                }else{
                    j++;
                }
            }
        }
        return answer;
    }

    public PostingsList positionalIntersect(PostingsList p1, PostingsList p2, int posDiff) {
        PostingsList answer = new PostingsList();
        int size1 = p1.size();
        int size2 = p2.size();
        if (size1 != 0 && size2 != 0) {
            int i = 0;
            int j = 0;
            while (i < size1 && j < size2) {
                PostingsEntry e1 = p1.get(i);
                PostingsEntry e2 = p2.get(j);
                int doc1 = e1.docID;
                int doc2 = e2.docID;
                if (doc1 == doc2) {
                    LinkedList<Integer> pl1 = e1.offsets;
                    LinkedList<Integer> pl2 = e2.offsets;
                    int k = 0;
                    int l = 0;
                    int sizepl1 = pl1.size();
                    int sizepl2 = pl2.size();
                    while (k < sizepl1 && l < sizepl2) {
                        int comp1 = pl1.get(k);
                        int comp2 = pl2.get(l);
                        if (comp2 - comp1 == posDiff) {
                            PostingsEntry e = new PostingsEntry();
                            LinkedList<Integer> tl = new LinkedList<>();
                            e.token = e1.token;
                            e.docID = e1.docID;
                            tl.add(pl1.get(k));
                            e.offsets = tl;
                            answer.add(e);
                            k++;
                            l++;
                        } else if (pl2.get(l) - pl1.get(k) > posDiff) {
                            k++;
                        } else {
                            l++;
                        }
                    }
                    i++;
                    j++;
                } else if (e1.docID > e2.docID) {
                    j++;
                } else {
                    i++;
                }
            }
        }
        return answer;
    }

    //Returns a list of entries of a term with only one entry per document.
    public PostingsList unique(PostingsList pList){
        PostingsList answer = new PostingsList();
        Set<Integer> uniqueDoc = new HashSet<>();

        for (int i=0;i<pList.size();i++){
            PostingsEntry entry = pList.get(i);
            int dID = entry.docID;
            if(uniqueDoc.add(dID)){
                answer.add(entry);
            }
        }
        return answer;
    }
}