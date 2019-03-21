/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/**
 *   Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer {

    /** The index to be built up by this Indexer. */
    Index index;

    /** K-gram index to be built up by this Indexer */
    KGramIndex kgIndex;

    /** The next docID to be generated. */
    private int lastDocID = 0;

    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file;

    RandomAccessFile docIDFile;

    {
        try {
            docIDFile = new RandomAccessFile("./index/docIDFile", "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    /* ----------------------------------------------- */


    /** Constructor */
    public Indexer( Index index, String patterns_file ) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.patterns_file = patterns_file;
    }


    /** Generates a new document identifier as an integer. */
    private int generateDocID() {
        return lastDocID++;
    }

    /** own */
    public static ArrayList<Integer> scores = new ArrayList();

    public static float getScore(int index){
        return scores.get(index);
    }

    /**
     *  Tokenizes and indexes the file @code{f}. If <code>f</code> is a directory,
     *  all its files and subdirectories are recursively processed.
     */
    public void processFiles( File f, boolean is_indexing ) {
        // do not try to index fs that cannot be read
        if (is_indexing) {
            if ( f.canRead() ) {
                if ( f.isDirectory() ) {
                    String[] fs = f.list();
                    // an IO error could occur
                    if ( fs != null ) {
                        for ( int i=0; i<fs.length; i++ ) {
                            processFiles( new File( f, fs[i] ), is_indexing );
                        }
                    }
                } else {
                    // First register the document and get a docID
                    int docID = generateDocID();

                    //Debugging
                    /*try {
                        docIDFile.writeChars(f.getPath().toString() + "---" + Integer.toString(docID) +"\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/

                    if ( docID%1000 == 0 ) System.err.println( "Indexed " + docID + " files" );
                    try {
                        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
                        Tokenizer tok = new Tokenizer( reader, true, false, true, patterns_file );
                        int offset = 0;
                        String oldToken = "";
                        HashMap<String, LinkedList<Integer>> offsHash = new HashMap<>();
                        while ( tok.hasMoreTokens() ) {
                            String token = tok.nextToken();
                            if(offsHash.containsKey(token)){
                                LinkedList offsList = offsHash.get(token);
                                offsList.add(offset++);
                            }else{
                                LinkedList<Integer> offsets = new LinkedList<>();
                                offsets.add(offset++);
                                offsHash.put(token, offsets);

                            }
                        }
                        for (Map.Entry<String, LinkedList<Integer>> entry : offsHash.entrySet()) {
                            insertIntoIndex(docID, entry.getKey(), entry.getValue());
                        }


                        index.docNames.put( docID, f.getPath() );
                        index.docLengths.put( docID, offset );
                        //Own line below
                        scores.add(docID, 0);
                        reader.close();
                    } catch ( IOException e ) {
                        System.err.println( "Warning: IOException during indexing." );
                    }
                }
            }
        }
    }


    /* ----------------------------------------------- */


    /**
     *  Indexes one token.
     */
    public void insertIntoIndex( int docID, String token, LinkedList<Integer> offsets ) {
        index.insert( token, docID, offsets );
        if (kgIndex != null)
            kgIndex.insert(token);
    }
}

