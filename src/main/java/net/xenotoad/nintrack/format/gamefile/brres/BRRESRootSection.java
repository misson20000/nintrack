package net.xenotoad.nintrack.format.gamefile.brres;

import net.xenotoad.nintrack.MagicNumberMismatchException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

/**
 * Created by misson20000 on 3/12/17.
 */
public class BRRESRootSection {
    public static final byte[] magic = new byte[] { 'r', 'o', 'o', 't' };

    public final BRRESIndexGroup group;

    public BRRESRootSection(BRRESIndexGroup group) {
        this.group = group;
    }

    public static BRRESRootSection readRootSection(SeekableByteChannel channel) throws IOException, MagicNumberMismatchException {
        ByteBuffer headerBuffer = ByteBuffer.allocateDirect(8);
        channel.read(headerBuffer);
        headerBuffer.flip();
        headerBuffer.order(ByteOrder.BIG_ENDIAN);

        byte[] magicValue = new byte[4];
        headerBuffer.get(magicValue);
        if(!Arrays.equals(magicValue, magic)) {
            throw new MagicNumberMismatchException(magic, magicValue);
        }

        int sectionLength = headerBuffer.getInt();
        return new BRRESRootSection(BRRESIndexGroup.readIndexGroup(channel, channel.position()));
    }
}
