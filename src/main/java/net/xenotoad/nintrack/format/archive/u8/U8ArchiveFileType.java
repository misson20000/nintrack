package net.xenotoad.nintrack.format.archive.u8;

import javafx.concurrent.Task;
import net.xenotoad.nintrack.FileType;
import net.xenotoad.nintrack.TreeNode;

/**
 * Created by misson20000 on 3/3/17.
 */
public class U8ArchiveFileType implements FileType {
    public static final U8ArchiveFileType instance = new U8ArchiveFileType();

    private U8ArchiveFileType() {

    }

    @Override
    public String getName() {
        return "U8 Archive";
    }

    @Override
    public boolean canBeExtracted() {
        return true;
    }

    @Override
    public String getCheckboxName() {
        return "formatU8";
    }

    @Override
    public boolean openByDefault() {
        return false;
    }

    @Override
    public Task<Void> extract(TreeNode node)  {
        return new U8ArchiveExtractionTask(node);
    }
}
