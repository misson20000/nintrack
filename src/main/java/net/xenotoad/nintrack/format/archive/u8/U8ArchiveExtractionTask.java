package net.xenotoad.nintrack.format.archive.u8;

import javafx.application.Platform;
import javafx.concurrent.Task;
import net.xenotoad.nintrack.ByteRegion;
import net.xenotoad.nintrack.CorruptedFileException;
import net.xenotoad.nintrack.DirectoryFileType;
import net.xenotoad.nintrack.MagicNumberMismatchException;
import net.xenotoad.nintrack.TreeNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by misson20000 on 3/3/17.
 */
public class U8ArchiveExtractionTask extends Task<Void> {
    private final TreeNode node;

    public U8ArchiveExtractionTask(TreeNode node) {
        this.node = node;
    }

    @Override
    protected Void call() throws Exception {
        SeekableByteChannel channel = node.getRegion().read();
        ByteBuffer header = ByteBuffer.allocateDirect(16);
        channel.read(header);
        header.flip();

        header.order(ByteOrder.BIG_ENDIAN);

        byte[] magic = new byte[4];
        header.get(magic);
        if(!Arrays.equals(magic, U8ArchiveIdentifier.regularMagic)) {
            throw new MagicNumberMismatchException(U8ArchiveIdentifier.regularMagic, magic);
        }

        int rootNodeOffset = header.getInt();
        int headerSize = header.getInt();
        int dataOffset = header.getInt();

        channel.position(rootNodeOffset);
        ByteBuffer index = ByteBuffer.allocateDirect(headerSize);
        channel.read(index);
        index.flip();
        index.order(ByteOrder.BIG_ENDIAN);

        U8Record root = new U8Record(node.getRegion(), dataOffset, index);
        if (!root.isDirectory) {
            throw new CorruptedFileException("U8 root is not directory");
        }
        U8Record[] records = new U8Record[root.size];
        records[0] = root;
        for (int i = 1; i < records.length; i++) {
            records[i] = new U8Record(node.getRegion(), dataOffset, index);
        }

        int stringTableOffset = index.position();

        U8Directory rootDir = new U8Directory((rec) -> {
            index.position(stringTableOffset + rec.fileNameOffset);
            StringBuilder name = new StringBuilder();
            char ch = (char) index.get();
            while (ch != 0) {
                name.append(ch);
                ch = (char) index.get();
            }
            return name.toString();
        }, root, records, 0);

        Collection<TreeNode> children = rootDir.toNodeTree();
        Platform.runLater(() -> node.getChildren().setAll(children));
        return null;
    }

    private class U8Record {
        private final boolean isDirectory;
        private final int fileNameOffset;
        private final long offset;
        private final int size;
        private final ByteRegion region;

        public U8Record(ByteRegion region, int dataOffset, ByteBuffer fst) {
            this.region = region;
            long typeAndFname = fst.getInt() & 0xFFFFFFFFL; // force unsigned behavior
            int type  = (int) (typeAndFname  & 0xFF000000L);
            int fname = (int) (typeAndFname  & 0x00FFFFFFL);
            long offset = (long) fst.getInt();
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

    private class U8File {
        private String name;
        private U8Record record;

        private U8File(NameResolver nr, U8Record myRecord) throws IOException {
            this.name = nr.getName(myRecord);
            this.record = myRecord;
        }

        public TreeNode toNode() {
            return new TreeNode(record.toRegion(), name);
        }
    }

    private class U8Directory {
        private String name;
        private List<U8Directory> subdirectories;
        private List<U8File> files;

        public U8Directory(NameResolver nr, U8Record myRecord, U8Record[] records, int i) throws IOException {
            subdirectories = new LinkedList<>();
            files = new LinkedList<>();
            name = nr.getName(myRecord);

            i++;
            while(i < myRecord.size) {
                U8Record record = records[i];
                if(record.isDirectory) {
                    subdirectories.add(new U8Directory(nr, record, records, i));
                    i = record.size;
                } else {
                    files.add(new U8File(nr, record));
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
                    subdirectories.stream().map(U8Directory::toNode),
                    files.stream().map(U8File::toNode)).collect(Collectors.toList());
        }
    }

    private interface NameResolver {
        String getName(U8Record record) throws IOException;
    }
}
