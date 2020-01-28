package net.xenotoad.nintrack.format.gamefile.brres;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

/**
 * Created by misson20000 on 3/12/17.
 */
public class BRRESIndexGroup {
    private final BRRESIndexEntry root;
    public final BRRESIndexEntry[] entries;
    public final long basePosition;

    public BRRESIndexGroup(long basePosition, BRRESIndexEntry root, BRRESIndexEntry[] entries) {
        this.root = root;
        this.entries = entries;
        this.basePosition = basePosition;
    }

    public static BRRESIndexGroup readIndexGroup(SeekableByteChannel channel, long basePosition) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocateDirect(8);
        channel.read(headerBuffer);
        headerBuffer.flip();
        headerBuffer.order(ByteOrder.BIG_ENDIAN);

        int bytesLength = headerBuffer.getInt();
        int numberInGroup = headerBuffer.getInt();

        BRRESIndexEntry root = BRRESIndexEntry.readIndexEntry(channel, basePosition);

        BRRESIndexEntry[] entries = new BRRESIndexEntry[numberInGroup];
        for(int i = 0; i < entries.length; i++) {
            entries[i] = BRRESIndexEntry.readIndexEntry(channel, basePosition);
        }

        return new BRRESIndexGroup(basePosition, root, entries);
    }

    public void debugDump() {
        System.out.println("  Number of Entries (not including root): " + entries.length);
        System.out.println("  Base Position: 0x" + Long.toHexString(basePosition));
        System.out.println("  Root:");
        root.debugDump();
        for(int i = 0; i < entries.length; i++) {
            System.out.println("  Entry " + i + ":");
            entries[i].debugDump();
        }
    }
}
