package net.xenotoad.nintrack.format.compression.lz77type11;

import net.xenotoad.nintrack.ByteRegion;
import net.xenotoad.nintrack.FileIdentifier;
import net.xenotoad.nintrack.FileType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * Created by misson20000 on 3/1/17.
 */
public class Lz77Type11Identifier implements FileIdentifier {
    @Override
    public FileType identify(ByteRegion region) throws IOException {
        if(region.getLength() < 4) {
            return null;
        }
        SeekableByteChannel channel = region.read();
        ByteBuffer buffer = ByteBuffer.allocate(8);
        channel.read(buffer);
        buffer.flip();
        if(buffer.get(0) != 0x11) {
            return null;
        }
        int fileSize = buffer.getInt() & 0x00FFFFFF;
        if(fileSize == 0) {
            if(region.getLength() < 8) {
                return null;
            }
            fileSize = buffer.getInt();
        }
        if(fileSize < region.getLength()/4) { // maybe it's just really suckily "compressed"?
            return null; // if it's this bad, we probably just read garbage and this isn't *actually* compressed
        }
        return Lz77Type11FileType.instance;
    }
}
