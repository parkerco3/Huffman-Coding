import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.*;

public class CharCounter implements ICharCounter, IHuffConstants {
    public static final int BITS_PER_WORD = 8;

    /**
     * The size of the alphabet given the number of bits per chunk, this
     * should be 2^BITS_PER_WORD.
     */
    public static final int ALPH_SIZE = (1 << BITS_PER_WORD);

    /**
     * The standard number of bits needed to represent/store
     * an int, this is 32 in Java and nearly all other languages.
     */
    public static final int BITS_PER_INT = 32;

    /**
     * The value of the PSEUDO_EOF character. This is one-more
     * than the maximum value of a legal BITS_PER_WORD-bit character.
     */

    static final int PSEUDO_EOF = ALPH_SIZE;

    /**
     * Isolate the magic number in one place.
     */
    public static final int MAGIC_NUMBER = 1234567873;


    Map<Integer, Integer> counts = new HashMap <>();



    /**
     * Returns the count associated with specified character.
     * @param ch is the chunk/character for which count is requested
     * @return count of specified chunk
     * @throws IOException 
     * @throws the appropriate exception if ch isn't a valid chunk/character
     */
    public int getCount(int ch) {
        int tempValue = 0;
        try {
            if (counts.containsKey(ch)) {    
                tempValue = counts.get(ch);
                return tempValue;
            } else if (ch > ALPH_SIZE) {
                throw new IOException();
            } else {
                return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempValue;
    }

    /**
     * Initialize state by counting bits/chunks in a stream
     * @param stream is source of data
     * @return count of all chunks/read
     * @throws IOException if reading fails
     */
    public int countAll(InputStream stream) throws IOException {
        int counter = 0;
        try {
            BitInputStream b = new BitInputStream(stream);
            while (true) {
                int currentChunk = b.read();
                if (currentChunk == -1) {
                    break;
                }
                if (currentChunk > ALPH_SIZE) {
                    continue;
                }
                counter++;
                if (counts.containsKey(currentChunk)) {
                    this.add(currentChunk);
                } else {
                    this.set(currentChunk, 1);
                }
            }
            b.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return counter;
    }

    /**
     * Update state to record one occurrence of specified chunk/character.
     * @param i is the chunk being recorded
     */
    public void add(int i) {
        counts.put(i, this.getCount(i) + 1);
    }

    /**
     * Set the value/count associated with a specific character/chunk.
     * @param i is the chunk/character whose count is specified
     * @param value is # occurrences of specified chunk
     */
    public void set(int i, int value) {
        counts.put(i, value);
    }


    /**
     * All counts cleared to zero.
     */
    public void clear() {
        counts.clear();
    }

    /**
     * @return a map of all characters and their frequency
     */
    public Map<Integer, Integer> getTable() {
        return counts;
    }

}
