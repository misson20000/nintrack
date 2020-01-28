package net.xenotoad.nintrack.format.archive.u8;

import net.xenotoad.nintrack.ByteRegion;
import net.xenotoad.nintrack.FileIdentifier;
import net.xenotoad.nintrack.FileType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

/**
 * Created by misson20000 on 3/3/17.
 */
public class U8ArchiveIdentifier implements FileIdentifier {
    public static final byte[] regularMagic = new byte[] {0x55, (byte) 0xAA, 0x38, 0x2D};

    @Override
    public FileType identify(ByteRegion region) throws IOException {
        if(region.getLength() < 16) {
            return null;
        }

        SeekableByteChannel channel = region.read();
        ByteBuffer header = ByteBuffer.allocateDirect(16);
        channel.read(header);
        header.flip();

        header.order(ByteOrder.BIG_ENDIAN);

        byte[] magic = new byte[4];
        header.get(magic);
        if(!Arrays.equals(magic, regularMagic)) {
            return null;
        }
        return U8ArchiveFileType.instance;
    }
}
