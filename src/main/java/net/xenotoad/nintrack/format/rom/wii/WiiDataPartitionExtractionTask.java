package net.xenotoad.nintrack.format.rom.wii;

import javafx.application.Platform;
import javafx.concurrent.Task;
import net.xenotoad.nintrack.ByteRegion;
import net.xenotoad.nintrack.CorruptedFileException;
import net.xenotoad.nintrack.DirectoryFileType;
import net.xenotoad.nintrack.ExtractionException;
import net.xenotoad.nintrack.TreeNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by misson20000 on 2/25/17.
 */
public class WiiDataPartitionExtractionTask extends Task<Void> {
    private final TreeNode node;

    public WiiDataPartitionExtractionTask(TreeNode node) {
        this.node = node;
    }

    public Void call() throws IOException, ExtractionException {
        try {
            final ByteBuffer headerBuffer = ByteBuffer.allocateDirect(0x430);
            final ByteBuffer fstBuffer = ByteBuffer.allocateDirect(0x0C);
            final ByteBuffer nameBuffer = ByteBuffer.allocateDirect(256);

            SeekableByteChannel channel = node.getRegion().read();
            headerBuffer.clear();
            channel.read(headerBuffer);
            headerBuffer.position(0x424);
            headerBuffer.order(ByteOrder.BIG_ENDIAN);

            long fstOffset = ((long) headerBuffer.getInt()) << 2;
            int fstSize = headerBuffer.getInt();
            int maxFstSize = headerBuffer.getInt();

            fstBuffer.clear();
            channel.position(fstOffset);
            channel.read(fstBuffer);
            fstBuffer.flip();

            FSTRecord root = new FSTRecord(node.getRegion(), fstBuffer);
            if (!root.isDirectory) {
                throw new CorruptedFileException("FST root is not directory");
            }
            FSTRecord[] records = new FSTRecord[root.size];
            records[0] = root;
            for (int i = 1; i < records.length; i++) {
                fstBuffer.clear();
                channel.position(fstOffset + (0x0C * i));
                channel.read(fstBuffer);
                fstBuffer.flip();
                records[i] = new FSTRecord(node.getRegion(), fstBuffer);
            }

            FSTDirectory rootDir = new FSTDirectory((rec) -> {
                channel.position(fstOffset + (root.size * 0x0C) + rec.fileNameOffset);
                nameBuffer.clear();
                channel.read(nameBuffer);
                nameBuffer.flip();
                StringBuilder name = new StringBuilder();
                char ch = (char) nameBuffer.get();
                while (ch != 0) {
                    if (nameBuffer.remaining() <= 0) {
                        nameBuffer.clear();
                        channel.read(nameBuffer);
                        nameBuffer.flip();
                    }
                    name.append(ch);
                    ch = (char) nameBuffer.get();
                }
                return name.toString();
            }, root, records, 0);

            Collection<TreeNode> children = rootDir.toNodeTree();
            Platform.runLater(() -> node.getChildren().setAll(children));
        } catch(Throwable t) {
            t.printStackTrace();
            throw t;
        }
        return null;
    }

    private class FSTRecord {
        private final boolean isDirectory;
        private final int fileNameOffset;
        private final long offset;
        private final int size;
        private final ByteRegion region;

        public FSTRecord(ByteRegion region, ByteBuffer fst) {
            this.region = region;
            long typeAndFname = fst.getInt() & 0xFFFFFFFFL; // force unsigned behavior
            int type  = (int) (typeAndFname  & 0xFF000000L);
            int fname = (int) (typeAndFname  & 0x00FFFFFFL);
            long offset = ((long) fst.getInt()) << 2;
            int size = fst.getInt();

            this.isDirectory = type != 0;
            this.fileNameOffset = fname;
            this.offset = offset;
            this.size = size;
        }

        public ByteRegion toRegion() {
            return region.subdivide(offset, size);
        }
    }

    private class FSTFile {
        private String name;
        private FSTRecord record;

        private FSTFile(NameResolver nr, FSTRecord myRecord) throws IOException {
            this.name = nr.getName(myRecord);
            this.record = myRecord;
        }

        public TreeNode toNode() {
            return new TreeNode(record.toRegion(), name);
        }
    }

    private class FSTDirectory {
        private String name;
        private List<FSTDirectory> subdirectories;
        private List<FSTFile> files;

        public FSTDirectory(NameResolver nr, FSTRecord myRecord, FSTRecord[] records, int i) throws IOException {
            subdirectories = new LinkedList<>();
            files = new LinkedList<>();
            name = nr.getName(myRecord);

            i++;
            while(i < myRecord.size) {
                FSTRecord record = records[i];
                if(record.isDirectory) {
                    subdirectories.add(new FSTDirectory(nr, record, records, i));
                    i = record.size;
                } else {
                    files.add(new FSTFile(nr, record));
                    i++;
                }
            }
        }

        public TreeNode toNode() {
            TreeNode node = new TreeNode(name, DirectoryFileType.instance);
            node.getChildren().setAll(toNodeTree());
            return node;
        }

        public List<TreeNode> toNodeTree() {
            return Stream.concat(
                    subdirectories.stream().map(FSTDirectory::toNode),
                    files.stream().map(FSTFile::toNode)).collect(Collectors.toList());
        }
    }

    private interface NameResolver {
        String getName(FSTRecord record) throws IOException;
    }
}
