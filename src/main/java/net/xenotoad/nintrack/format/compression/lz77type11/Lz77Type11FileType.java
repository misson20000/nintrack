package net.xenotoad.nintrack.format.compression.lz77type11;

import javafx.concurrent.Task;
import net.xenotoad.nintrack.FileType;
import net.xenotoad.nintrack.TreeNode;

/**
 * Created by misson20000 on 3/1/17.
 */
public class Lz77Type11FileType implements FileType {
    public static final Lz77Type11FileType instance = new Lz77Type11FileType();

    private Lz77Type11FileType() {

    }

    @Override
    public String getName() {
        return "LZ77 Type 11 Compressed File";
    }

    @Override
    public boolean canBeExtracted() {
        return true;
    }

    @Override
    public String getCheckboxName() {
        return "formatLz77Type11";
    }

    @Override
    public boolean openByDefault() {
        return true;
    }

    @Override
    public Task<Void> extract(TreeNode node) {
        return new Lz77Type11ExtractionTask(node);
    }
}
