package net.xenotoad.nintrack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Created by misson20000 on 2/26/17.
 */
public class ExportFileJob extends ExportJob {
    public ExportFileJob(TreeNode src, Path dst) {
        super(src, dst);
    }

    @Override
    public Void call() throws IOException {
        try {
            FileChannel out = FileChannel.open(dst, new StandardOpenOption[] {
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            });
            SeekableByteChannel in = src.getRegion().read();

            long size = src.getRegion().getLength();

            ByteBuffer buffer = ByteBuffer.allocateDirect(65536);
            int forceCounter = 0;
            while (in.read(buffer) != -1 && !isCancelled()) {
                buffer.flip();
                out.write(buffer);
                buffer.compact();
                updateProgress(in.position(), size);
                if(forceCounter++ > 50) {
                    out.force(false);
                    forceCounter = 0;
                }
            }
            updateMessage("Flushing buffers...");
            buffer.flip();
            while (buffer.hasRemaining() && !isCancelled()) {
                out.write(buffer);
                updateProgress(out.position(), size);
            }
            in.close();
            out.close();
            if(isCancelled()) {
                Files.delete(dst);
            }
        } catch (IOException e) {
            throw e;
        }
        return null;
    }
}
