package net.xenotoad.nintrack.format.compression.lz77type11;

import de.saxsys.javafx.test.JfxRunner;
import de.saxsys.javafx.test.TestInJfxThread;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;

/**
 * Created by misson20000 on 3/12/17.
 */

@RunWith(JfxRunner.class)
public class Lz77Type11ExtractionTaskTest {
    @Test
    @TestInJfxThread
    public void testLipsum() throws Exception {
        Lz77Type11ExtractionTask task = new Lz77Type11TestingTask("lipsum.txt");
        task.call();
    }

    @Test
    @TestInJfxThread
    public void testRandom() throws Exception {
        Lz77Type11ExtractionTask task = new Lz77Type11TestingTask("random.bin");
        task.call();
    }

    /**
     * Created by misson20000 on 3/12/17.
     */
    public static class Lz77Type11TestingTask extends Lz77Type11ExtractionTask {
        private final String fileName;

        public Lz77Type11TestingTask(String s) {
            super(null);
            fileName = s;
        }

        @Override
        public Void call() throws Exception {
            FileChannel channel = FileChannel.open(Paths.get(Lz77Type11TestingTask.class.getResource("/" + fileName + ".LZ").toURI()));
            ByteBuffer in = ByteBuffer.allocateDirect((int) channel.size());
            channel.read(in);
            in.flip();

            ByteBuffer out = performExtraction(in);
            out.flip();
            FileChannel cmpChannel = FileChannel.open(Paths.get(Lz77Type11TestingTask.class.getResource("/" + fileName).toURI()));
            ByteBuffer cmp = ByteBuffer.allocateDirect((int) cmpChannel.size());
            cmpChannel.read(cmp);
            cmp.flip();

            while(out.hasRemaining() && cmp.hasRemaining()) {
                byte outB = out.get();
                byte cmpB = cmp.get();
                if(outB != cmpB) {
                    throw new IncorrectOutputException("Diverged at 0x" + Integer.toHexString(out.position()) + ", decmp: " + outB + ", orig: " + cmpB);
                }
            }

            if(cmp.hasRemaining()) {
                throw new IncorrectOutputException("Decompressed output truncated to 0x" + Integer.toHexString(out.position()) + ", output should be 0x" + Integer.toHexString(cmp.limit()) + " bytes long");
            }

            if(out.hasRemaining()) {
                throw new IncorrectOutputException("Output is too long! Decompressed 0x" + Integer.toHexString(out.position()) + " bytes, should be 0x" + Integer.toHexString(cmp.limit()));
            }

            System.out.println("Output okay, " + out.position() + "/" + cmp.position() + "/" + cmp.limit());

            return null;
        }

        private class IncorrectOutputException extends Exception {
            public IncorrectOutputException(String s) {
                super(s);
            }
        }
    }
}