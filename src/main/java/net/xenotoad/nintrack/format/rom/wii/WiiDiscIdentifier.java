package net.xenotoad.nintrack.format.rom.wii;

import net.xenotoad.nintrack.ByteRegion;
import net.xenotoad.nintrack.FileIdentifier;
import net.xenotoad.nintrack.FileType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

/**
 * Created by misson20000 on 2/24/17.
 */
public class WiiDiscIdentifier implements FileIdentifier {
    @Override
    public FileType identify(ByteRegion region) throws IOException {
        if(region.getLength() < 1024) {
            return null;
        }
        SeekableByteChannel channel = region.read();
        ByteBuffer header = ByteBuffer.allocate(1024);
        header.clear();
        channel.read(header);
        header.position(0x18);
        byte[] magic = new byte[4];
        header.get(magic);
        byte[] test = new byte[] {0x5D, 0x1C, (byte) 0x9E, (byte) 0xA3 };
        if(!Arrays.equals(magic, test)) {
            return null;
        }
        header.position(0x60);
        if(header.getShort() == 0) { // these two bytes are each set to 1 if this is a wii partition
            return WiiDiscFileType.instance;
        } else {
            return WiiPartitionFileType.UNKNOWN;
        }
    }
}
