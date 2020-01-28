package net.xenotoad.nintrack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

/**
 * Created by misson20000 on 3/12/17.
 */
public class MagicNumberIdentifier implements FileIdentifier {
    private final long magicOffset;
    private final byte[] magic;
    private final FileType fileType;

    public MagicNumberIdentifier(long magicOffset, byte[] magic, FileType fileType) {
        this.magicOffset = magicOffset;
        this.magic = magic;
        this.fileType = fileType;
    }

    public MagicNumberIdentifier(byte[] magic, FileType fileType) {
        this.magicOffset = 0;
        this.magic = magic;
        this.fileType = fileType;
    }

    @Override
    public FileType identify(ByteRegion region) throws IOException {
        if(region.getLength() < magicOffset + magic.length) {
            return null;
        }

        SeekableByteChannel channel = region.read();
        ByteBuffer magicBuffer = ByteBuffer.allocateDirect(magic.length);
        channel.position(magicOffset);
        channel.read(magicBuffer);
        magicBuffer.flip();

        byte[] magicValue = new byte[magic.length];
        magicBuffer.get(magicValue);
        if(Arrays.equals(magicValue, this.magic)) {
            return fileType;
        }
        return null;
    }
}
