package net.xenotoad.nintrack.format.rom.wii;

import javafx.application.Platform;
import javafx.concurrent.Task;
import net.xenotoad.nintrack.ExtractionException;
import net.xenotoad.nintrack.TreeNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by misson20000 on 2/25/17.
 */
public class WiiDiscExtractionTask extends Task<Void> {
    private final TreeNode node;

    public WiiDiscExtractionTask(TreeNode node) {
        this.node = node;
    }

    public Void call() throws IOException, ExtractionException {
        final ByteBuffer partitionTableBuffer = ByteBuffer.allocate(0x20);
        final ByteBuffer partitionInfoBuffer = ByteBuffer.allocate(1024);
        final ByteBuffer partitionHeaderBuffer = ByteBuffer.allocate(0x2C0);

        partitionTableBuffer.order(ByteOrder.BIG_ENDIAN);
        partitionInfoBuffer.order(ByteOrder.BIG_ENDIAN);
        partitionHeaderBuffer.order(ByteOrder.BIG_ENDIAN);

        SeekableByteChannel channel = node.getRegion().read();
        channel.position(0x40000);
        partitionTableBuffer.clear();
        partitionTableBuffer.limit(0x20);
        channel.read(partitionTableBuffer);
        partitionTableBuffer.flip();

        WiiPartitionByteRegion[][] tables = new WiiPartitionByteRegion[4][];
        for(int tableId = 0; tableId < 4; tableId++) {
            updateProgress(tableId, 4);

            int numPartitions = partitionTableBuffer.getInt();
            tables[tableId] = new WiiPartitionByteRegion[numPartitions];

            channel.position(((long) partitionTableBuffer.getInt()) << 2);
            partitionInfoBuffer.clear();
            partitionInfoBuffer.limit(numPartitions * 8);
            channel.read(partitionInfoBuffer);
            partitionInfoBuffer.flip();

            for(int partId = 0; partId < numPartitions; partId++) {
                updateProgress(tableId + (partId/(double) numPartitions), 4);

                long offset = ((long) partitionInfoBuffer.getInt()) << 2;
                int type = partitionInfoBuffer.getInt();

                channel.position(offset);
                partitionHeaderBuffer.clear();
                partitionHeaderBuffer.limit(0x2C0);
                channel.read(partitionHeaderBuffer);
                partitionHeaderBuffer.flip();

                partitionHeaderBuffer.position(0x2A4);
                int tmdSize = partitionHeaderBuffer.getInt();
                long tmdOffset = ((long) partitionHeaderBuffer.getInt()) << 2;
                int certChainSize = partitionHeaderBuffer.getInt();
                long certChainOffset = ((long) partitionHeaderBuffer.getInt()) << 2;
                long h3TableOffset = ((long) partitionHeaderBuffer.getInt()) << 2;
                long dataOffset = ((long) partitionHeaderBuffer.getInt()) << 2;
                long dataSize = ((long) partitionHeaderBuffer.getInt()) << 2;
                partitionHeaderBuffer.position(0);
                partitionHeaderBuffer.limit(0x2A4);
                WiiTicket ticket = WiiTicket.read(partitionHeaderBuffer);

                System.out.println("Wii Partition (" + tableId + ", " + partId + "):");
                System.out.println("  dataSize: " + dataSize);
                System.out.println("  dataSize % 0x7C00: " + dataSize % 0x7C00);
                System.out.println("  dataSize % 0x8000: " + dataSize % 0x8000);

                // we ignore the TMD
                tables[tableId][partId] = new WiiPartitionByteRegion(node.getRegion().subdivide(offset + dataOffset, dataSize), ticket, type);
            }
        }

        updateProgress(1, 1);
        Platform.runLater(() -> {
            List<TreeNode> partitions = new LinkedList<>();
            for (int tableId = 0; tableId < tables.length; tableId++) {
                for (int partId = 0; partId < tables[tableId].length; partId++) {
                    partitions.add(new TreeNode(tables[tableId][partId], "Partition " + tableId + ", " + partId, tables[tableId][partId].getType()));
                }
            }
            node.getChildren().setAll(partitions);
        });

        return null;
    }
}
