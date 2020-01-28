package net.xenotoad.nintrack.format.gamefile.brres;

import javafx.application.Platform;
import javafx.concurrent.Task;
import net.xenotoad.nintrack.MagicNumberMismatchException;
import net.xenotoad.nintrack.TreeNode;
import net.xenotoad.nintrack.UnknownFileType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by misson20000 on 3/12/17.
 */
public class BRRESExtractionTask extends Task<Void> {
    private final TreeNode node;

    public BRRESExtractionTask(TreeNode node) {
        this.node = node;
    }

    @Override
    protected Void call() throws Exception {
        SeekableByteChannel channel = node.getRegion().read();
        ByteBuffer header = ByteBuffer.allocateDirect(0x10);
        channel.read(header);
        header.flip();
        header.order(ByteOrder.BIG_ENDIAN);

        byte[] magic = new byte[6];
        header.get(magic);
        if(!Arrays.equals(magic, BRRESIdentifier.magic)) {
            throw new MagicNumberMismatchException(magic, BRRESIdentifier.magic);
        }

        header.getShort(); // padding
        int fileLength = header.getInt();
        int rootOffset = header.getShort();
        int numSections = header.getShort();

        channel.position(rootOffset);
        BRRESRootSection rootSection = BRRESRootSection.readRootSection(channel);

        List<TreeNode> folders = new LinkedList<>();

        for(BRRESIndexEntry e : rootSection.group.entries) {
            channel.position(e.dataPointer);
            BRRESIndexGroup group = BRRESIndexGroup.readIndexGroup(channel, e.dataPointer);
            TreeNode folder = new TreeNode(e.name, BRRESGroupFileType.instance);
            List<TreeNode> files = new LinkedList<>();
            for(BRRESIndexEntry file : group.entries) {
                files.add(new TreeNode(file.name, UnknownFileType.instance));
            }
            folder.getChildren().setAll(files);
            folders.add(folder);
        }

        Platform.runLater(() -> node.getChildren().setAll(folders));
        return null;
    }
}
