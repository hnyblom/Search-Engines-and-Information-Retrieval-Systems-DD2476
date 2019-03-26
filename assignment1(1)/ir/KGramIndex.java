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
    static HashMap<Integer,String> id2term = new HashMap<Integer,String>();

    /** Mapping from term strings to term ids */
    static HashMap<String,Integer> term2id = new HashMap<String,Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    static HashMap<String,List<KGramPostingsEntry>> KGIndex = new HashMap<String,List<KGramPostingsEntry>>();

    static HashMap<Integer, HashMap<String,List<KGramPostingsEntry>>> indexes = new HashMap<>();

    /** The ID of the last processed term */
    static int lastTermID = -1;

    private static int indexed =0;

    /** Number of symbols to form a K-gram */
    static int K = 3;

    static Object indexLock = new Object();

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private static int generateTermID() {
        return ++lastTermID;
    }

    public static int getK() {
        return K;
    }

    private static int getIndexed(){return indexed++;}

    private static void resetTermMaps(){
        HashMap<String,Integer> newTerm2ID = new HashMap<>();
        HashMap<Integer,String> newID2Term = new HashMap<>();
        term2id = newTerm2ID;
        id2term = newID2Term;
        lastTermID = -1;
    }

    /**
     *  Get intersection of two postings lists
     */
    private static List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        //long startTime = System.currentTimeMillis();
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
                //String term1 = getTermByID(p1.get(i).tokenID);
                //String term2 = getTermByID(p2.get(j).tokenID);
                String term1 = p1.get(i).token;
                String term2 = p2.get(j).token;

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
        //long elapsedTime = System.currentTimeMillis() - startTime;
        //System.err.println(String.format( "intersect done in %.1f seconds.", elapsedTime/1000.0 ));
        return result;
    }

    public static List<KGramPostingsEntry> unique(List<KGramPostingsEntry> kgList){
        LinkedList<KGramPostingsEntry> answer = new LinkedList<>();
        Set<String> uniqueDoc = new HashSet<>();

        for (int i=0;i<kgList.size();i++){
            KGramPostingsEntry entry = kgList.get(i);
            String token = entry.token;
            if(uniqueDoc.add(token)){
                answer.add(entry);
            }
        }
        return answer;
    }

    /** Inserts all k-grams from a token into the index. */
    public static LinkedList<String> getKGrams( String token ) {
        LinkedList<String> result = new LinkedList<>();
        int gramSize = getK();
        String kGramStr = "";
        StringBuilder sBuild1 = new StringBuilder(gramSize-1);

        /**First part**/
        sBuild1.append("$");
        //Append first k-gram with one char less, "replaced" by $
        for(int j=0;j<(gramSize-1)&&j<token.length();j++){
            sBuild1.append(token.charAt(j));
        }
        kGramStr = sBuild1.toString();
        result.add(kGramStr);
        //addToIndex(kGramStr,e);

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
            result.add(kGramStr);
            //addToIndex(kGramStr,e);
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
        result.add(kGramStr);
        //addToIndex(kGramStr,e);

        return result;
    }

    public static void addToIndex(String kGramStr, KGramPostingsEntry e){
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
    public static List<KGramPostingsEntry> getPostings(String kgram, int k) {
        List<KGramPostingsEntry> kgList = indexes.get(k).get(kgram);
        return unique(kgList);
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public static String getTermByID(Integer id) {
        return id2term.get(id);
    }

    public static HashMap<String,String> decodeArgs( String[] args ) {
        HashMap<String,String> decodedArgs = new HashMap<>();
        int i=0;
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
        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        File f = new File(args.get("file"));
        String patterns = args.get("patterns_file");
        K =k;

        /**Create normal index**/
        Index index = new HashedIndex();
        Indexer indexer = new Indexer( index, patterns );
        synchronized (indexLock){
            long startTime = System.currentTimeMillis();
            indexer.processFiles(f, true);
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.err.println(String.format( "Indexing done in %.1f seconds.", elapsedTime/1000.0 ));
        }


        String[] kgrams = args.get("kgram").split(" ");
        boolean wildcard = false;
        /**Check wildcard mode**/
        String w = args.getOrDefault("w", "");
        if(w.equals("int")||w.equals("phr")||w.equals("rank")||w.equals("none")){
            wildcard=true;
        }
        if(wildcard){
            wildcardMode(kgrams, f, patterns, w, index);
        }else {
            normalMode(f, patterns, k, kgrams, true);
        }
    }

    public static List<KGramPostingsEntry> normalMode(File f, String patterns, int k, String[] kgrams, Boolean print) throws FileNotFoundException, IOException {
        System.err.println("Normal mode");
        K=k;
        //KGramIndex kgIndex = new KGramIndex(k);
        if(!indexes.containsKey(k)){
            /**Insert into index**/
            long startTime = System.currentTimeMillis();
            insertIntoIndex(f, patterns);
            indexes.put(k, KGIndex);
            KGIndex = new HashMap<>();
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.err.println(String.format("kgram indexing query done in %.1f seconds.", elapsedTime / 1000.0));
        }

        /**Get postings**/
        List<KGramPostingsEntry> postings = null;
        if(kgrams.length==1){
            postings = getPostings(kgrams[0],k);
        }else {
            for (String kgram : kgrams) {
                postings = processKGram(kgram, postings, k);
            }
        }
        /**Print results**/
        if(print){
            printRes(postings);
        }

        return postings;
    }

    public static void printRes(List<KGramPostingsEntry> postings){
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {

            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            /*if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }*/
            /*for (int i = 0; i < resNum; i++) {
                System.err.println(postings.get(i).token);
                //System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }*/
        }
    }

    public static void wildcardMode(String[] kgrams, File f, String patterns, String w, Index index) throws FileNotFoundException, IOException{
        System.err.println("Wildcard mode");
        long startTime = System.currentTimeMillis();
        HashMap<Integer, String> queryOrder = new HashMap<>();
        HashMap<Integer,List<KGramPostingsEntry>> queryOrderWild = new HashMap<>();

        int kgCount = -1;
        for (String kgram : kgrams) {
            kgCount++;
            if (kgram.contains("*")){
                List<KGramPostingsEntry> wildResult = new ArrayList<>();
                List<List<KGramPostingsEntry>> wildResults = new ArrayList<>();
                int wildPosition = kgram.indexOf("*"); //Star in beginning, middle or end?
                String[] input = kgram.split("\\*");
                String[] wildcardParts = new String[input.length - 1];

                if (input[0].equals("")) {
                    wildcardParts[0] = input[1];
                } else {
                    wildcardParts = input;
                }
                //Wildcards can only be divided into one or two parts.
                String wildpart1 = wildcardParts[0];
                String wildpart2 = "";
                if (wildcardParts.length == 2) {
                    wildpart2 = wildcardParts[1];
                }

                /**Get results from normal mode**/
                for (String wildpart : wildcardParts) {
                    resetTermMaps();
                    int k = wildpart.length();
                    String[] partList = {wildpart};
                    wildResult = normalMode(f, patterns, k, partList, false);
                    /**Check the wildcard results**/
                    wildResult = wildCheck(wildResult, wildPosition, wildpart1, wildpart2);
                    //System.err.println("After wildcheck:");
                    //printRes(wildResult);
                    wildResults.add(wildResult);

                }
                /**Intersect wildcard results**/
                if(wildResults.size()==1){
                    wildResult = wildResults.get(0);
                }else if(wildResults.size()>0){
                    wildResult = wildResults.get(0);
                    for(int i=0;i<wildResults.size()-1;i++){
                        wildResult = intersect(wildResult,wildResults.get(i));
                    }
                }
                queryOrderWild.put(kgCount,wildResult);
            }else{
                //Not wildcard
                queryOrder.put(kgCount, kgram);
            }
        }
        /**Wildcard further query**/
        wildcardSearch(w, queryOrder, queryOrderWild, kgCount+1, index);
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println(String.format( "Wildcard query done in %.1f seconds.", elapsedTime/1000.0 ));
    }
    public static void wildcardSearch(String w, HashMap<Integer,String> queryOrder, HashMap<Integer,List<KGramPostingsEntry>> queryOrderWild, int querySize, Index index){
        System.err.println("Wildcard search");
        Searcher searcher = new Searcher(index);
        PostingsList allSearches = new PostingsList();
        String[] nonWildcards = queryOrder.values().toArray(new String[queryOrder.entrySet().size()]);
        /**Build query**/
        String queryString = "";
        ArrayList<String> queries = new ArrayList<>();
        buildQuery(queryString,0, querySize, queryOrder, queryOrderWild, queries);
        switch (w){
            case "int":
                for(String queryResultString : queries){
                    Query query = new Query(queryResultString);
                    PostingsList pList = searcher.search(query,QueryType.INTERSECTION_QUERY,RankingType.TF_IDF);
                    for(int i=0;i<pList.size();i++){
                        allSearches.add(pList.get(i));
                    }
                }
                allSearches = searcher.unique(allSearches);
                if (allSearches == null) {
                    System.err.println("Found 0 results");
                } else {
                    int resNum2 = allSearches.size();
                    System.err.println("Found " + resNum2 + " results");
                    /*if (resNum2 > 10) {
                        System.err.println("The first 10 of them are:");
                        resNum2 = 10;
                    }*/
                    /*for (int i = 0; i < resNum2; i++) {
                        System.err.println(index.docNames.get(allSearches.get(i).docID));
                    }*/
                }
                break;
            case "phr":
                for(String queryResultString : queries){
                    Query query = new Query(queryResultString);
                    PostingsList pList = searcher.search(query,QueryType.PHRASE_QUERY,RankingType.TF_IDF);
                    for(int i=0;i<pList.size();i++){
                        allSearches.add(pList.get(i));
                    }
                }
                allSearches = searcher.unique(allSearches);
                if (allSearches == null) {
                    System.err.println("Found 0 results");
                } else {
                    int resNum2 = allSearches.size();
                    System.err.println("Found " + resNum2 + " results");
                }
                break;
            case "rank":
                for(String queryResultString : queries){
                    Query query = new Query(queryResultString);
                    PostingsList pList = searcher.search(query,QueryType.RANKED_QUERY,RankingType.TF_IDF);
                    for(int i=0;i<pList.size();i++){
                        allSearches.add(pList.get(i));
                    }
                }
                allSearches = searcher.unique(allSearches);
                if (allSearches == null) {
                    System.err.println("Found 0 results");
                } else {
                    int resNum2 = allSearches.size();
                    System.err.println("Found " + resNum2 + " results");
                }
                break;
        }
    }
    static int printOnce = 0;
    public static void buildQuery(String buildingString, int processed, int querySize, HashMap<Integer, String> queryOrder, HashMap<Integer, List<KGramPostingsEntry>> queryOrderWild, ArrayList<String> results){
        double startTime = 0.0;
        if(printOnce==0){
            System.err.println("BuildQuery");
            startTime = System.currentTimeMillis();
        }
        //for(int i=processed;(i<queryOrder.size() || i<queryOrderWild.size());i++){
            if (queryOrderWild.containsKey(processed)) {
                List<KGramPostingsEntry> wilds = queryOrderWild.get(processed);
                if(processed==querySize-1){
                    for(KGramPostingsEntry wild:wilds){
                        String newBuild;
                        if(!buildingString.equals("")){
                            newBuild = buildingString + " " + wild.token;
                        }else{
                            newBuild = buildingString + wild.token;
                        }
                        results.add(newBuild);
                    }
                }else{
                    processed = processed+1;
                    for(KGramPostingsEntry wild:wilds){
                        String newBuild;
                        if(!buildingString.equals("")){
                            newBuild = buildingString + " " + wild.token;
                        }else{
                            newBuild = buildingString + wild.token;
                        }

                        buildQuery(newBuild, processed,querySize,queryOrder,queryOrderWild,results);
                    }
                }

            }else {
                String nonWild = queryOrder.get(processed);
                buildingString = buildingString + " " + nonWild;
                processed = processed+1;
                if(processed==querySize){
                    results.add(buildingString);
                }else{
                    buildQuery(buildingString,processed,querySize,queryOrder,queryOrderWild,results);
                }
            }
            if(printOnce==0){
                double elapsedTime = System.currentTimeMillis() - startTime;
                double div = 100.0;
                System.err.println("Build query done in "+elapsedTime/div+ " seconds." );
                printOnce++;
            }

        }
    //}

    static int docs = 0;
    public static void insertIntoIndex(File f, String patterns) throws IOException {
        //System.err.println("Insert into index");

        if(f.isDirectory()){
            String[] fs = f.list();
            // an IO error could occur
            if ( fs != null ) {
                for ( int i=0; i<fs.length; i++ ) {
                    insertIntoIndex( new File( f, fs[i] ), patterns);
                }
            }
        }else{
            docs++;
            Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
            Tokenizer tok = new Tokenizer( reader, true, false, true, patterns );
            while ( tok.hasMoreTokens() ) {
                String token = tok.nextToken();
                if (token.length() > K) {
                    int termID;
                    LinkedList<String> kGrams = getKGrams(token);
                    if (!term2id.keySet().contains(token)) {
                        //Only process new terms
                        //Fill index (or indexes)
                        termID = generateTermID();
                        lastTermID = termID;
                        term2id.put(token, termID);
                        id2term.put(termID, token);

                        //Create new KGramPostingsEntry from string
                        KGramPostingsEntry e = new KGramPostingsEntry(termID, token);
                        for (String kgrm : kGrams) {
                            addToIndex(kgrm, e);
                        }
                    }
                }
            }
        }
        if ( docs%1000 == 0 ) System.err.println( "Indexed " + docs + " files (kgram)" );
    }

    public static List<KGramPostingsEntry> processKGram(String kgram, List<KGramPostingsEntry> postings, int k){
        System.err.println("Process");

        /*if (kgram.length() != k) {
            System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
            System.exit(1);
        }*/
        List<KGramPostingsEntry> check = getPostings(kgram, k);

        if (postings == null) {
            postings = check;
        } else {
            postings = intersect(postings, check);
        }
        return postings;
    }

    public static LinkedList<KGramPostingsEntry> wildCheck(List<KGramPostingsEntry> check, int position, String kGram1, String kGram2){
        //System.err.println("WildCheck");
        //long startTime = System.currentTimeMillis();
        LinkedList<KGramPostingsEntry> res = new LinkedList<>();
        int kgLen = kGram1.length();
        if(!kGram2.equals("")){
            //Mid
            position = 1;
        }else if(position==kgLen){
            //* at end
            position = 2;
        }
        //If position is already 0 -> * at beginning

            switch (position){
                case 0:
                    //* at start -> last part need to match

                    for(KGramPostingsEntry entry:check) {
                        String token = entry.token;
                        int tokenLen = token.length();
                        if (token.substring(tokenLen - kgLen, tokenLen).equals(kGram1)) {
                            res.add(entry);
                        }
                    }
                    break;
                case 1:
                    //* at mid
                    for(KGramPostingsEntry entry:check) {
                        String token = entry.token;
                        int tokenLen = token.length();
                        String first;
                        String second;
                        int index1 = token.indexOf(kGram1);
                        int index2 = token.indexOf(kGram2);
                        if (index1 != (-1) && index2 != (-1)) {
                            if (index1 < index2) {
                                first = kGram1;
                                second = kGram2;
                            } else {
                                first = kGram2;
                                second = kGram1;
                            }
                            if (token.matches(first + "\\S*" + second)) {
                                res.add(entry);
                            }
                        }
                    }
                    break;
                case 2:
                    //* at end -> first part need to match
                    //Sort the list to be checked
                    Collections.sort(check, new SortKGramByString());
                    Boolean hasMatched = false;
                    for(KGramPostingsEntry entry:check) {
                        String token = entry.token;
                        String sub = token.substring(0, kgLen);
                        if (sub.equals(kGram1)) {
                            hasMatched = true;
                            res.add(entry);
                        }else if(hasMatched){
                            break;
                        }
                    }
                    break;
            }

        //long elapsedTime = System.currentTimeMillis() - startTime;
        //System.err.println(String.format( "WildCheck done in %.1f seconds.", elapsedTime/1000.0 ));
        return res;
    }
}
