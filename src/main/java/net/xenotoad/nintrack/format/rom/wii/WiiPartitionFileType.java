package net.xenotoad.nintrack.format.rom.wii;

import javafx.concurrent.Task;
import net.xenotoad.nintrack.FileType;
import net.xenotoad.nintrack.TreeNode;

/**
 * Created by misson20000 on 2/28/17.
 */
public enum WiiPartitionFileType implements FileType {
    DATA("Wii Data Partition") {
        @Override public boolean canBeExtracted() {
            return true;
        }
    }, UPDATE("Wii Update Partition"), CHANNEL_INSTALLER("Wii Channel Installer Partition"), UNKNOWN("Wii Partition (unknown type; missing metadata)") {
        @Override public boolean canBeExtracted() {
            return true;
        }
    };

    private final String name;

    WiiPartitionFileType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canBeExtracted() {
        return false;
    }

    @Override
    public String getCheckboxName() {
        return "formatWiiPartition";
    }

    @Override
    public boolean openByDefault() {
        return true;
    }

    @Override
    public Task<Void> extract(TreeNode node) {
        return new WiiDataPartitionExtractionTask(node);
    }
}
