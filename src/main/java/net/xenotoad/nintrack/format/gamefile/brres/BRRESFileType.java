package net.xenotoad.nintrack.format.gamefile.brres;

import javafx.concurrent.Task;
import net.xenotoad.nintrack.FileType;
import net.xenotoad.nintrack.TreeNode;

/**
 * Created by misson20000 on 3/12/17.
 */
public class BRRESFileType implements FileType {
    private BRRESFileType() {

    }

    public static final BRRESFileType instance = new BRRESFileType();

    @Override
    public String getName() {
        return "BRRES File";
    }

    @Override
    public boolean canBeExtracted() {
        return true;
    }

    @Override
    public String getCheckboxName() {
        return "formatBrres";
    }

    @Override
    public boolean openByDefault() {
        return false;
    }

    @Override
    public Task<Void> extract(TreeNode node) {
        return new BRRESExtractionTask(node);
    }
}
