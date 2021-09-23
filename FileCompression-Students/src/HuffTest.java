import static org.junit.Assert.*;
import java.io.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

public class HuffTest implements IHuffConstants {
    //String root = "/autograder/submission/";
      String root = "";

    @Test
    public void makeHuffTreeTest() throws IOException {
        Huff h = new Huff();
        InputStream ins = new ByteArrayInputStream("abc".getBytes("UTF-8"));
        h.makeHuffTree(ins);
        h.makeTable();
        assertNotNull(h.getCode('a'));

    }
    @Test
    public void writeTest() throws IOException {
        try {
            Huff h = new Huff();
            h.write(root + "testfile", root + "testOut", false);
            BitInputStream stream = new BitInputStream(root + "testOut");
            int magic = stream.read(BITS_PER_INT);
            assertEquals(magic, MAGIC_NUMBER);
            stream.close();

            Huff hu = new Huff();
            hu.write(root + "testfile", root + "testOut1", true);
            BitInputStream stream1 = new BitInputStream(root + "testOut1");
            int magic1 = stream1.read(BITS_PER_INT);
            assertEquals(magic1, MAGIC_NUMBER);
            stream1.close();

            Huff huf = new Huff();
            huf.write(root + "testfileSmall", root + "testOutSmall", true);

            Huff huff = new Huff();
            huff.write(root + "testfileSmall", root + "testOutSmall1", false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void uncompressTest() throws IOException {
        Huff h = new Huff();

        h.write(root + "testfile", root + "testOut3", true);
        h.uncompress(root + "testOut3", root + "uncompressed");



        Huff hu = new Huff();
        hu.write(root + "testfileSmall", root + "testOut4", true);
        hu.uncompress(root + "testOut4", root + "uncompressed1");
    }


}
