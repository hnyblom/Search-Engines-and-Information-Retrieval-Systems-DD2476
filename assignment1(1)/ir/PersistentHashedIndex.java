/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, KTH, 2018
 */

package ir;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
//import java.util.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks.
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The dictionary file name */
    public static final String DATA_FNAME = "data";

    public  static  final String COLL_FNAME = "collisions";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    RandomAccessFile collisionFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    long freeColl = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    public static final int WORD_KEY_LEN = 64;
    public static final int POINTERLEN = 64;

    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {
        public int collision;
        public long dataPointer;
        public int datalen;
        //Possibly some more information you deem necessary
        //public String word;
        //public long hashOffset;

    }

    HashMap<Long, LinkedList> collisionMap = new HashMap<Long, LinkedList>();


    // ==================================================================


    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
            collisionFile = new RandomAccessFile(INDEXDIR + "/" + COLL_FNAME, "rw");

        } catch ( IOException e ) {
            e.printStackTrace();
        }
        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr );
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }
    int writeColl( String dataString, long ptr ) {
        try {
            collisionFile.seek( ptr );
            byte[] data = dataString.getBytes();
            int length = data.length;
            collisionFile.write( data );
            return length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }
    void writeDict( int collision, long dataPtr, int dataSize, long ptr ) {
        try {
            dictionaryFile.seek( ptr );
            dictionaryFile.writeInt(collision);
            //dictionaryFile.writeChars("*");
            dictionaryFile.writeLong( dataPtr );
            //dictionaryFile.writeChars("*");
            dictionaryFile.writeInt(dataSize);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }


    /**
     *  Reads data from the data file
     */
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }
    Entry readDict( long ptr) {
        Entry entry = new Entry();
        entry.collision=2; //Default value indicates miss in read
        try {
            dictionaryFile.seek( ptr );
            long dictPointer = dictionaryFile.getFilePointer();
            long dictLength = dictionaryFile.length();
            if(dictPointer+16<dictLength) {
                entry.collision = dictionaryFile.readInt();
                //char c1 = dictionaryFile.readChar();
                entry.dataPointer = dictionaryFile.readLong();
                //char c2 = dictionaryFile.readChar();
                entry.datalen = dictionaryFile.readInt();
            }
            return entry;
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }
    String readColl( long ptr, int size ) {
        try {
            collisionFile.seek( ptr );
            byte[] data = new byte[size];
            collisionFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }
    //djb2 http://www.cse.yorku.ca/~oz/hash.html
    //The magic of number 33 (why it works better than many other constants, prime or not) has never been adequately explained.
    //Why 5381: practically any good multiplier works. Will play a role in generating different hash values for strings of different lengths.
    long hash(String term){
        long hash = 5381;
        for(int i=0; i<term.length();i++){
            hash = ((hash<<5)+hash)+term.charAt(i); /* hash * 33 + c */
        }
        hash %= TABLESIZE;
        if(hash<0){
            hash+=TABLESIZE;
        }
        //hash = Math.round(hash/10)*20;
        hash = hash*16;
        return hash;
    }

    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file.
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry(Entry entry, String word) {
        //long ptr = Math.abs(word.hashCode()%TABLESIZE);
        long ptr = hash(word);
        Entry existingEntry = readEntry(word);
        //Hash-collision
        if((existingEntry.collision == 0)&&(existingEntry.datalen!=0)){ //&& ptr!=0
            HashMap c = collisionMap;
            LinkedList o = collisionMap.get(ptr);
            if(o!=null) {
                Object first = o.getFirst();
                String firstWord = first.toString();

                //Get the previous postingsList for the word hashed to this value
                //PostingsList existingList = getPostList(existingEntry, firstWord); //Uncomment
                //String existingWord = existingList.get(0).token;


                //Prepare string for CollisionFile
                StringBuffer sb = new StringBuffer("");
                sb.append(firstWord);
                sb.append("*");
                sb.append(existingEntry.dataPointer);
                sb.append("*");
                sb.append(existingEntry.datalen);
                sb.append("*");
                sb.append(word);
                sb.append("*");
                sb.append(entry.dataPointer);
                sb.append("*");
                sb.append(entry.datalen);
                int datalen = writeColl(sb.toString(), freeColl);

                //Write the collisionFile-address to the dictionary-file

                if (datalen > 0) {
                    writeDict(1, freeColl, datalen, ptr);
                    freeColl = freeColl + datalen;
                }
            }
            //Already a collision on this line of the dictionary-file
        }else if (existingEntry.collision == 1){
            HashMap c = collisionMap;
            LinkedList o = collisionMap.get(ptr);
            int existingDataLen = existingEntry.datalen;
            if((o!=null)&&(existingDataLen>0)) {

                String s = readColl(existingEntry.dataPointer, existingDataLen);
                StringBuffer doubleColl = new StringBuffer(s);
                doubleColl.append("*");
                doubleColl.append(word);
                doubleColl.append("*");
                doubleColl.append(entry.dataPointer);
                doubleColl.append("*");
                doubleColl.append(entry.datalen);

                int datalen = writeColl(doubleColl.toString(), freeColl);
                //Write the collisionFile-address to the dictionary-file
                if (datalen>0){
                    writeDict(1, freeColl, datalen, ptr);
                    freeColl = freeColl+datalen;
                }
            }else{
                System.err.println("Overwriting, datalen 0");
            }
            //Empty line of the dictionary-file
        }else if(((existingEntry.collision==0)||existingEntry.collision==2)&&(existingEntry.dataPointer==0)&&(existingEntry.datalen==0)){
            long dataPointer = entry.dataPointer;
            int dataLen = entry.datalen;
            if(dataLen>0){
                writeDict(0, dataPointer, dataLen, ptr);
            }
        }else{
            System.err.println("Trying to write over existing values");
            System.err.println("Collision: " + existingEntry.collision);
            System.err.println("DataPointer: "+existingEntry.dataPointer);
            System.err.println("DataLen: "+existingEntry.datalen);
        }

        //Keep track of all word-hashValue tuplets.
        LinkedList collList = collisionMap.get(ptr);
        if(collList!=null){
            collList.add(word);
            //collisionMap.put(ptr, collList);
        }else{
            LinkedList newCollList = new LinkedList();
            newCollList.add(word);
            collisionMap.put(ptr, newCollList);
        }

    }

    Entry readEntry(String token){
        //long ptr = Math.abs(token.hashCode()%TABLESIZE);
        long ptr = hash(token);
        Entry e = readDict(ptr);
        return e;
    }

    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }

    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }

    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            index.forEach((k,v) -> stringPosting(k,v));

        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }


    // ==================================================================

    public void stringPosting(String word, PostingsList pList){
        //Avoid serialization with string-buffer
        StringBuffer sb = new StringBuffer("");
        for (int i =0;i<pList.size();i++){
            PostingsEntry entry = pList.get(i);
            sb.append(entry.token);
            sb.append("*");
            sb.append(entry.docID);
            sb.append("*");
            sb.append(entry.score);
            for(int j=0; j<entry.offsets.size(); j++){
                sb.append("*");
                sb.append(entry.offsets.get(j));
            }
            sb.append("¤");
        }
        //Write string representation to Data-file
        int datalen = writeData(sb.toString(),free);

        Entry e = new Entry();
        e.collision = 2;
        e.dataPointer = free;
        //Increment file position
        //TODO: Detta var felet,incrementade file position för tidigt.
        if (datalen>0) free = free+datalen;
        //e.word = word;
        e.datalen = datalen;
        //Write entry with saved Data-file pointer to Dictionary
        writeEntry(e, word);
    }

    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        PostingsList postList = new PostingsList();
        Entry e = readEntry(token);
        if(e.collision!=2){
            postList = getPostList(e, token);
        }
        return postList;
    }
    public HashMap<String,PostingsEntry> getTokens(int docID){
        HashMap<String,PostingsEntry> result = new HashMap<String,PostingsEntry>();
        for(Map.Entry<String, PostingsList> entry : index.entrySet()){
            PostingsList l = entry.getValue();
            for(int i=0;i<l.size();i++){
                PostingsEntry e = l.get(i);
                if(e.docID==docID){
                    result.put(e.token, e);
                }
            }
        }
        return result;
    }

    public PostingsList getPostList(Entry entry, String token){
        PostingsList postList = new PostingsList();
        int coll = entry.collision;
        long ptr = entry.dataPointer;
        int length = entry.datalen;
        String data = "";
        if(coll==1){
            //Read from collisionFile
            data = readColl(ptr, length);
            String[] collEntries = data.split("\\*");
            for(int j=0; j<collEntries.length; j=j+3){
                if(collEntries[j].equals(token)){
                    long dataPointer = Long.parseLong(collEntries[j+1]);
                    int dataLength = Integer.parseInt(collEntries[j+2]);
                    data = readData(dataPointer, dataLength);
                }
            }
        }else{
            long lengthCheck = 0;
            try {

                lengthCheck = dataFile.length();
                int nothing = 0;
            } catch (IOException e) { e.printStackTrace(); }
            long check2 = ptr+length;
            if(check2<lengthCheck){
                data = readData(ptr,length);
            }else{
                System.err.println("Trying to read past end of data-file");
            }

        }
        //Turn data string into postList
        //data: {ID,offset,score}{}...
        //Split at / -> one PostingsEntry
        //Split at , -> vals of PostingsEntry
        String[] entries = data.split("¤");
        for(int i=0;i<entries.length;i++){
            String[] values = entries[i].split("\\*");
            PostingsEntry newEntry = new PostingsEntry();
            LinkedList<Integer> offsets = new LinkedList<Integer>();
            if(values.length>3){
                newEntry.token = values[0];
                newEntry.docID = Integer.parseInt(values[1]);
                newEntry.score = Double.parseDouble(values[2]);

                for(int j=3; j<values.length; j++){
                    offsets.add(Integer.parseInt(values[j]));
                }
                newEntry.offsets = offsets;
                postList.add(newEntry);
            }
        }
        return postList;
    }


    /**
     *  Inserts this token in the main-memory hashtable.
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
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
