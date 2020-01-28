package net.xenotoad.nintrack.format.rom.wii;

import javafx.concurrent.Task;
import net.xenotoad.nintrack.FileType;
import net.xenotoad.nintrack.TreeNode;

/**
 * Created by misson20000 on 2/24/17.
 */
public class WiiDiscFileType implements FileType {
    public static final WiiDiscFileType instance = new WiiDiscFileType();

    private WiiDiscFileType() {

    }

    public String getName() {
        return "Wii Disc";
    }

    @Override
    public Task<Void> extract(TreeNode node) {
        return new WiiDiscExtractionTask(node);
    }

    @Override
    public boolean canBeExtracted() {
        return true;
    }

    @Override
    public String getCheckboxName() {
        return "formatWii";
    }

    @Override
    public boolean openByDefault() {
        return true;
    }
}
