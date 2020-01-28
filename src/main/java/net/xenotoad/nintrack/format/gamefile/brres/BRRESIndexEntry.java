package net.xenotoad.nintrack.format.gamefile.brres;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

/**
 * Created by misson20000 on 3/12/17.
 */
public class BRRESIndexEntry {
    private final int entryId;
    private final int leftIndex;
    private final int rightIndex;
    public final String name;
    public final int dataPointer;

    public BRRESIndexEntry(int entryId, int leftIndex, int rightIndex, String name, int dataPointer) {
        this.entryId = entryId;
        this.leftIndex = leftIndex;
        this.rightIndex = rightIndex;
        this.name = name;
        this.dataPointer = dataPointer;
    }

    public static BRRESIndexEntry readIndexEntry(SeekableByteChannel channel, long basePosition) throws IOException {
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(0x10);
        channel.read(readBuffer);
        readBuffer.flip();
        readBuffer.order(ByteOrder.BIG_ENDIAN);

        int entryId = readBuffer.getShort();
        readBuffer.getShort(); // unknown
        int leftIndex = readBuffer.getShort();
        int rightIndex = readBuffer.getShort();
        int namePointer = readBuffer.getInt();
        int dataPointer = readBuffer.getInt() + (int) basePosition;

        String name = "";
        if(namePointer > 0) {
            long retPos = channel.position();
            channel.position(basePosition + namePointer);
            readBuffer.clear();
            channel.read(readBuffer);
            readBuffer.flip();
            StringBuilder nameBuilder = new StringBuilder();
            byte ch = readBuffer.get();
            while (ch != 0) {
                nameBuilder.append((char) ch);
                if (!readBuffer.hasRemaining()) {
                    readBuffer.compact();
                    channel.read(readBuffer);
                    readBuffer.flip();
                }
                ch = readBuffer.get();
            }

            name = nameBuilder.toString();
            channel.position(retPos);
        }

        return new BRRESIndexEntry(entryId, leftIndex, rightIndex, name, dataPointer);
    }

    public void debugDump() {
        System.out.println("    entryId: " + entryId);
        System.out.println("    leftIndex: " + leftIndex);
        System.out.println("    rightIndex: " + rightIndex);
        System.out.println("    name: " + name);
        System.out.println("    dataPointer: 0x" + Integer.toHexString(dataPointer));
    }
}
