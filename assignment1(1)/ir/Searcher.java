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
import java.util.stream.Collectors;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    Indexer indexer;

    HashMap<Integer, Double> scores = new HashMap(); // Holds the cosinescores for each of the documents

    HashMap<String, Double> queryWeights = new HashMap<>();



    /** Constructor */
    public Searcher( Index index) {
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
                answer = inters;
            }
        }else if (queryType==QueryType.RANKED_QUERY){
            //Compute query vector
            ArrayList<Query.QueryTerm> queryList = query.queryterm;
            HashMap<Object, Integer> dupCount = new HashMap<>();

            //Count duplicates of terms in the query
            for (Query.QueryTerm qTerm : queryList) {
                String term = qTerm.term;
                Object getResult = dupCount.get(term);
                if (getResult!=null) {
                    int j = (int) getResult;
                    dupCount.put(term, j+1);
                }else{
                    dupCount.put(term, 1);
                }
            }
            for(Map.Entry<Object, Integer> entry : dupCount.entrySet()){ //Calculate query tf-idf
                String queryTerm = entry.getKey().toString();//ta bort tostring
                int tf = entry.getValue();
                int df = unique(index.getPostings(queryTerm)).size();
                queryWeights.put(queryTerm, queryTfIdf(tf, df));

            }
            Map<Integer, Double> result = cosineScore(queryWeights);
            PostingsList theList = new PostingsList();
            for(Map.Entry<Integer, Double> resultEntry : result.entrySet()){
                PostingsEntry newPostEntry = new PostingsEntry();
                newPostEntry.docID = resultEntry.getKey();
                newPostEntry.score = resultEntry.getValue();
                theList.add(newPostEntry);
            }
            answer = theList;
            //Number of documents in corpus which contains term
            /*if(query.size() ==1){
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
            }*/
        }
        return answer;
    }

    //Helpers
    public Map cosineScore(HashMap<String, Double> queryWeights){

        for(Map.Entry<String, Double> entry : queryWeights.entrySet()){
            String token = entry.getKey();
            //Fetch postings list
            PostingsList termPostingsList = index.getPostings(token);
            int docFrequency = docFrequency(token);
            //For each pair(d, tf(t,d)) (document, term-frequency) in postings list
            for(int i=0;i<termPostingsList.size();i++){
                PostingsEntry pEntry = termPostingsList.get(i);
                int docID = pEntry.docID;
                //int termFrequency = duplicates(token, docID);
                double termDocWeight = tfIdf(pEntry, docFrequency);
                double termQueryWeight = entry.getValue();
                double score = termDocWeight*termQueryWeight; //scores[d] += w(t,d) x w(t,q)
                int length = Index.docLengths.get(docID);
                score = score/length;
                scores.put(docID, score);

            }
        }
        Map<Integer, Double> sortedMap = scores.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2)->e1, LinkedHashMap::new));
        //Map<Integer, Double> subMap = sortedMap.entrySet().stream().limit(10).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2)->e1, LinkedHashMap::new));
        //return subMap;
        return sortedMap;
    }
    public int docFrequency(String token){
        PostingsList termPostingsList = index.getPostings(token);
        int docFrequency = unique(termPostingsList).size();
        return docFrequency;
    }

    public double tfIdf(PostingsEntry entry, int docFrequency){
        //Number of docs in corpus
        int n = 17481;
        double idf = Math.log(n/docFrequency); //Inverse document frequency.

        //term frequency (tf) - Check how many times the term occurs in the document
        int tf = duplicates(entry.token, entry.docID);

        //len.d - Check number of words in the document
        int length = index.docLengths.get(entry.docID);

        return (tf*(idf/length));

        //entry.score =+ (duplicateVal*idf)/length;
    }
    public double queryTfIdf(int tf, int df){
        //int n = querySize;
        int n = 17481;
        double idf = Math.log(n/df); //Inverse document frequency.
        return (tf*idf);
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
        PostingsList answer = new PostingsList();
        PostingsList oldPostList = new PostingsList();

        for (int i = 0; i < query.size()-1; i = i + 2) {
            PostingsList pi = index.getPostings(query.queryterm.get(i).term);
            PostingsList pi1 = index.getPostings(query.queryterm.get(i + 1).term);

            if (queryType == QueryType.INTERSECTION_QUERY) {
                pi = unique(pi);
                pi1 = unique(pi1);

                //No earlier terms. Don't try to intersect with oldPostList
                if (oldPostList.size() == 0) {
                    oldPostList = intersect(pi, pi1);
                    answer = oldPostList;

                    //2 or more earlier joined terms. Intersect the new intersection with oldPostList.
                } else {
                    PostingsList tempPostList = intersect(pi, pi1);
                    answer = intersect(oldPostList, tempPostList);
                    oldPostList = answer;
                }

                //Odd number of terms, last term. Pick out last term (odd number) and intersect with oldPostList.
                if (i + 3 == query.size()) {
                    PostingsList pOdd = index.getPostings(query.queryterm.get(i + 2).term);
                    pOdd = unique(pOdd);
                    answer = intersect(oldPostList, pOdd);
                }

            }else if(queryType==QueryType.PHRASE_QUERY){
                answer = phrase(query);
                answer = unique(answer);
            }
        }
        answer.sort(); //Testing

        return answer;
    }

    public PostingsList phrase(Query query){
        PostingsList answer;
        ArrayList<Query.QueryTerm> terms = query.queryterm;
        int termSize = terms.size();
        answer = index.getPostings(terms.get(0).term);
        for(int t =1; t<termSize; t++){
            String term = terms.get(t).term;
            PostingsList nextDocs = index.getPostings(term);
            answer = positionalIntersectNew(answer, nextDocs, 1);
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
                            //Next burgers
                            k++;
                        } else {
                            //Next and
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

    public PostingsList positionalIntersectNew(PostingsList p1, PostingsList p2, int posDiff) {
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
                            boolean check = false;
                            if(answer.size()!=0) {
                                for (int m = 0; m < answer.size(); m++) {
                                    PostingsEntry ans = answer.get(m);
                                        if (ans.docID==e2.docID) {
                                            ans.offsets.add(comp2);
                                            check=true;
                                        }
                                    }
                                    if(check==false){
                                        PostingsEntry e = new PostingsEntry();
                                        LinkedList<Integer> tl = new LinkedList<>();
                                        e.token = e2.token;
                                        e.docID = e2.docID;
                                        tl.add(comp2);
                                        e.offsets = tl;
                                        answer.add(e);
                                    }
                            }else{
                                PostingsEntry e = new PostingsEntry();
                                LinkedList<Integer> tl = new LinkedList<>();
                                e.token = e2.token;
                                e.docID = e2.docID;
                                tl.add(comp2);
                                e.offsets = tl;
                                answer.add(e);
                            }
                            k++;
                            l++;
                        } else if (pl2.get(l) - pl1.get(k) > posDiff) {
                            //Next burgers
                            k++;
                        } else {
                            //Next and
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