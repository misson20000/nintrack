package net.xenotoad.nintrack.format.compression.lz77type11;

import javafx.application.Platform;
import javafx.concurrent.Task;
import net.xenotoad.nintrack.ExtractionException;
import net.xenotoad.nintrack.MemoryByteRegion;
import net.xenotoad.nintrack.TreeNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

/**
 * Created by misson20000 on 3/3/17.
 */
public class Lz77Type11ExtractionTask extends Task<Void> {
    private final TreeNode node;

    public Lz77Type11ExtractionTask(TreeNode node) {
        this.node = node;
    }

    protected ByteBuffer performExtraction(ByteBuffer in) throws BacktrackedTooFarException {
        in.order(ByteOrder.LITTLE_ENDIAN);
        int filesizeAndFlags = in.getInt();
        int filesize = (filesizeAndFlags & 0xFFFFFF00) >>> 8;
        if (filesize == 0) {
            filesize = in.getInt();
        }
        int compressionType = filesizeAndFlags & 0x000000FF;

        int mdSize0Count = 0;
        int mdSize1Count = 0;
        int mdSize2Count = 0;

        ByteBuffer out = ByteBuffer.allocateDirect(filesize);
        while(in.hasRemaining() && out.hasRemaining()) {
            int flags = in.get() & 0xFF; // force unsigned

            for(int i = 0; i < 8 && out.hasRemaining() && in.hasRemaining(); i++) {
                if(((flags >>> (7-i)) & 0x01) > 0) {
                    int metadata1 = in.get() & 0xFF; // force unsigned
                    int disp;
                    int len;
                    if(metadata1 >>> 4 == 1) {
                        mdSize1Count++;
                        int metadata2 = in.get() & 0xFF; // force unsigned
                        int metadata3 = in.get() & 0xFF; // force unsigned
                        int metadata4 = in.get() & 0xFF; // force unsigned
                        len = (((metadata1 & 0b1111) << 12) | (metadata2 << 4) | (metadata3 >>> 4))
                                + 0x111;
                        disp = (((metadata3 & 0b1111) << 8) | metadata4) + 0x1;
                    } else if(metadata1 >>> 4 == 0) {
                        mdSize0Count++;
                        int metadata2 = in.get() & 0xFF; // force unsigned
                        int metadata3 = in.get() & 0xFF; // force unsigned
                        len = (((metadata1 & 0b1111) << 4) | (metadata2 >>> 4)) + 0x11;
                        disp = (((metadata2 & 0b1111) << 8) | metadata3) + 0x1;
                    } else {
                        mdSize2Count++;
                        len = (int) (((metadata1 & 0b11110000L) >>> 4) + 0x1);
                        int metadata2 = in.get() & 0xFF; // force unsigned
                        disp = (((metadata1 & 0b1111) << 8) | metadata2) + 0x1;
                    }
                    if(disp > out.position()) {
                        throw new BacktrackedTooFarException(disp, out.position());
                    }
                    for(int j = 0; j < len; j++) {
                        out.put(out.get(out.position() - disp));
                    }
                } else {
                    out.put(in.get());
                }
                updateProgress(out.position(), out.limit());
            }
        }

        updateMessage("Wrote " + ((out.position() * 100.0) / out.limit()) + "%, md: [" + Arrays.toString(new int[] {
                mdSize0Count, mdSize1Count, mdSize2Count
        }));
        return out;
    }

    @Override
    protected Void call() throws Exception {
        ByteBuffer in = ByteBuffer.allocateDirect((int) node.getRegion().getLength());
        try {
            SeekableByteChannel channel = node.getRegion().read();
            channel.read(in);
            channel.close();
        } catch (IOException e) {
            throw new ExtractionException(e);
        }
        in.flip();
        in.order(ByteOrder.BIG_ENDIAN);

        ByteBuffer out = performExtraction(in);

        String name = node.getName().getValue();
        int extensionIndex = name.toLowerCase().lastIndexOf(".lz");
        if(extensionIndex >= 0) {
            name = name.substring(0, extensionIndex);
        }

        TreeNode newNode = new TreeNode(new MemoryByteRegion(out), name);
        Platform.runLater(() -> node.getChildren().add(newNode));
        return null;
    }
}
