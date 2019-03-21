package ir;

import java.util.Comparator;

public class SortKGramByString implements Comparator<KGramPostingsEntry>{
    public int compare(KGramPostingsEntry e1, KGramPostingsEntry e2){
        String s1=e1.token;
        String s2=e2.token;

        return s1.compareToIgnoreCase(s2);

    }
}
