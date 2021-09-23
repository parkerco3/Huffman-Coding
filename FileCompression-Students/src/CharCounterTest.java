import static org.junit.Assert.*;
import java.io.*;

import org.junit.Test;

public class CharCounterTest {

    @Test
    public void countAlltest() throws IOException {
        ICharCounter cc = new CharCounter();
        InputStream ins = new ByteArrayInputStream("teststring".getBytes("UTF-8"));
        assertEquals(cc.countAll(ins), 10);
    }
    
    @Test
    public void getCounttest() throws IOException {
        ICharCounter cc = new CharCounter();
        InputStream ins = new ByteArrayInputStream("teststring".getBytes("UTF-8"));
        cc.countAll(ins);
        int count = cc.getCount('t');
        assertEquals(count,3);
        assertNotNull(cc.getTable());
        cc.clear();
        int count1 = cc.getCount('t');
        assertEquals(count1,0);
    }

}
