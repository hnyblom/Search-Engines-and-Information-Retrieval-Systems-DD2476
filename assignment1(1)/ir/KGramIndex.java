/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;


public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer,String> id2term = new HashMap<Integer,String>();

    /** Mapping from term strings to term ids */
    HashMap<String,Integer> term2id = new HashMap<String,Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String,List<KGramPostingsEntry>> KGIndex = new HashMap<String,List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }

    /**
     *  Get intersection of two postings lists
     */
    private List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        LinkedList<KGramPostingsEntry> result = new LinkedList<>();
        if (p1 != null && p2 != null) {
            //Only unique values
            p1 = unique(p1);
            p2 = unique(p2);
            //Sort the lists
            Collections.sort(p1, new SortKGramByString());
            Collections.sort(p2, new SortKGramByString());

            //Find intersections
            int i = 0;
            int j = 0;
            while (i < p1.size() && j < p2.size()) {
                String term1 = getTermByID(p1.get(i).tokenID);
                String term2 = getTermByID(p2.get(j).tokenID);

                int res = term1.compareToIgnoreCase(term2);
                if (res == 0) {
                    result.add(p1.get(i));
                    i++;
                    j++;
                } else if (res > 0) {
                    j++;
                } else {
                    i++;
                }
            }
            return result;
        }
        if(p1!=null){
            p1 = unique(p1);
            return p1;
        }if(p2!=null){
            p2 = unique(p2);
            return p2;
        }
        return result;
    }

    public List<KGramPostingsEntry> unique(List<KGramPostingsEntry> kgList){
        LinkedList<KGramPostingsEntry> answer = new LinkedList<>();
        Set<String> uniqueDoc = new HashSet<>();

        for (int i=0;i<kgList.size();i++){
            KGramPostingsEntry entry = kgList.get(i);
            String token = getTermByID(entry.tokenID);
            if(uniqueDoc.add(token)){
                answer.add(entry);
            }
        }
        return answer;
    }

    /** Inserts all k-grams from a token into the index. */
    public void insert( String token ) {
        int gramSize = getK();
        String kGramStr = "";
        int termID = generateTermID();
        lastTermID = termID;
        term2id.put(token, termID);
        id2term.put(termID,token);
        //Create new KGramPostingsEntry from string
        KGramPostingsEntry e = new KGramPostingsEntry(termID, token);
        StringBuilder sBuild1 = new StringBuilder(gramSize-1);

        /**First part**/
        sBuild1.append("$");
        //Append first k-gram with one char less, "replaced" by $
        for(int j=0;j<(gramSize-1)&&j<token.length();j++){
            sBuild1.append(token.charAt(j));
        }
        kGramStr = sBuild1.toString();
        addToIndex(kGramStr,e);

        /**Middle part**/
        //Go through word char by char
        for(int i=0;i<(token.length()-(gramSize-1));i++){
            //New stringBuilder
            StringBuilder sBuild2 = new StringBuilder(token.length()-(gramSize-1));
            //Append K characters
            for(int l=i;l<gramSize+i;l++){
                sBuild2.append(token.charAt(l));
            }
            kGramStr = sBuild2.toString();
            addToIndex(kGramStr,e);
        }

        /**Last part**/
        StringBuilder sBuild3 = new StringBuilder(gramSize-1);
        int start = token.length()-(gramSize-1);
        if(start<0){
            start = 0;
        }
        for(int k=start;k<token.length();k++){
            sBuild3.append(token.charAt(k));
        }
        sBuild3.append("$");
        kGramStr = sBuild3.toString();
        addToIndex(kGramStr,e);

    }

    public void addToIndex(String kGramStr, KGramPostingsEntry e){
        if(!KGIndex.containsKey(kGramStr)){
            LinkedList<KGramPostingsEntry> newList = new LinkedList<>();
            newList.add(e);
            KGIndex.put(kGramStr,newList);
        }else{
            List<KGramPostingsEntry> fetchedList = KGIndex.get(kGramStr);
            fetchedList.add(e);
            KGIndex.put(kGramStr,fetchedList);
        }
    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        return unique(KGIndex.get(kgram));
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    private static HashMap<String,String> decodeArgs( String[] args ) {
        HashMap<String,String> decodedArgs = new HashMap<String,String>();
        int i=0, j=0;
        while ( i < args.length ) {
            if ( "-p".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ( "-f".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ( "-k".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if("-w".equals(args[i])){
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("w", args[i++]);
                }
            } else if ( "-kg".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println( "Unknown option: " + args[i] );
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String,String> args = decodeArgs(arguments);
        Index index = new HashedIndex();
        Indexer indexer = new Indexer(index, args.get("p"));
        Searcher searcher = new Searcher(index);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        boolean wildcard = false;
        /**Check wildcard mode**/
        String w = args.getOrDefault("w", "");
        if(w.equals("int")||w.equals("phr")||w.equals("rank")||w.equals("none")){
            wildcard=true;
        }
        HashMap<Integer, KGramIndex> indexes = new HashMap<>();
        String[] kgrams = args.get("kgram").split(" ");
        //Wildcard mode
        if(wildcard){
            //Get lengths of all sub-parts of wildcard terms
            LinkedList<Integer> ks = new LinkedList<>();
            for(String kgram:kgrams){
                //Only for wildcard terms
                if(kgram.contains("*")) {
                    String[] wildcardParts = kgram.split("\\*");
                    for (String wildpart : wildcardParts) {
                        ks.add(wildpart.length());
                    }
                }
            }
            //Create an index for every relevant k (sizes of the sub-parts of the wildcard term)
            for(int thisk : ks){
                if(!indexes.containsKey(thisk)){
                    KGramIndex kgIndex = new KGramIndex(thisk);
                    indexes.put(thisk,kgIndex);
                }
            }
            //Wildcard mode not enabled.
        }else{
            //Create only one index of the specified(or default) type.
            KGramIndex kgIndex = new KGramIndex(k);
            indexes.put(k,kgIndex);
        }

        /**Insert into index/es**/
        LinkedList<Integer> indexesList = new LinkedList<>(indexes.keySet());
        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
        Tokenizer tok = new Tokenizer( reader, true, false, true, args.get("patterns_file") );
        while ( tok.hasMoreTokens() ) {
            String token = tok.nextToken();
            //Fill index (or indexes)
            for(int indexl : indexesList){
                indexes.get(indexl).insert(token);
            }
        }

        /**Get postings**/
        List<KGramPostingsEntry> postings = null;
        LinkedList<String> nonWildcards = new LinkedList<>();
        for (String kgram : kgrams) {

            //Wildcard term
            if(kgram.contains("*") && kgram.length()>1){
                int wildPosition = kgram.indexOf("*"); //Stra in beginning, middle or end?
                String[] wildcardParts = kgram.split("\\*");

                //Wildcards can only be divided into one or two parts.
                String wildpart1 = wildcardParts[0];
                String wildpart2 = "";
                if(wildcardParts.length==2){
                    wildpart2 = wildcardParts[1];
                }
                for(int i=0;i<wildcardParts.length;i++){
                    String otherPart="";
                    String wildpart = wildcardParts[i];
                    int len = wildpart.length();
                    KGramIndex lenIndex = indexes.get(len);
                    if (wildpart.equals(wildpart1)) {
                        otherPart = wildpart2;
                    }else if(wildpart.equals(wildpart2)){
                        otherPart = wildpart1;
                    }
                    postings = lenIndex.processKGram(wildpart, postings, lenIndex);
                }
                postings = wildCheck(postings, wildPosition, wildpart1, wildpart2);

            }else if(wildcard){
                //In wildcard mode: don't k-gram search for words without *
                nonWildcards.add(kgram);
            }else{
                //Normal mode
                KGramIndex kgIndex = indexes.get(k);
                postings = kgIndex.processKGram(kgram, postings, kgIndex);
            }
        }

        /**Print results**/
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {

            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(postings.get(i).token);
                //System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }

            /**Wildcard further query**/
            if(wildcard){
            //Wildcard mode: do intersection, phrase and ranked multiword query.
                switch (w){
                    case "int":
                        String queryString = "";
                        PostingsList allSearches = new PostingsList();
                        for(KGramPostingsEntry p : postings){
                            queryString = queryString + p.token;
                            for(String s: nonWildcards){
                                queryString = queryString+s;
                            }
                            Query query = new Query(queryString);
                            PostingsList pList = searcher.search(query,QueryType.INTERSECTION_QUERY,RankingType.TF_IDF);
                            for(int i=0;i<pList.size();i++){
                                allSearches.add(pList.get(i));
                            }
                        }
                        //TODO: Print the result
                        break;
                    case "phr":
                        break;
                    case "rank":
                        break;
                }
            }
        }
    }

    public List<KGramPostingsEntry> processKGram(String kgram, List<KGramPostingsEntry> postings, KGramIndex kgIndex){
        /*if (kgram.length() != k) {
            System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
            System.exit(1);
        }*/
        List<KGramPostingsEntry> check = kgIndex.getPostings(kgram);

        if (postings == null) {
            postings = check;
        } else {
            postings = kgIndex.intersect(postings, check);
        }
        return postings;
    }

    public static LinkedList<KGramPostingsEntry> wildCheck(List<KGramPostingsEntry> check, int position, String kGram1, String kGram2){
        LinkedList<KGramPostingsEntry> res = new LinkedList<>();
        int kgLen = kGram1.length();
        if(position==(kgLen-1)){
            //End
            position = 2;
        }else if(position!=0){
            //Mid
            position = 1;
        }

        for(KGramPostingsEntry entry:check){
            String token = entry.token;
            int tokenLen = token.length();
            switch (position){
                case 0:
                    //Start
                    if(token.substring(0,kgLen-1).equals(kGram1)){
                        res.add(entry);
                    }
                    break;
                case 1:
                    //Mid
                    String first;
                    String second;
                    if(token.indexOf(kGram1)<token.indexOf(kGram2)){
                        first = kGram1;
                        second = kGram2;
                    }else{
                        first = kGram2;
                        second = kGram1;
                    }
                    if(token.matches("\\S*"+first+"\\S+"+second+"\\S*")){
                        res.add(entry);
                    }
                    break;
                case 2:
                    //End
                    if(token.substring(tokenLen-(kgLen-1),tokenLen).equals(kGram1)){
                        res.add(entry);
                    }
                    break;
            }
        }
        return res;
    }
}
