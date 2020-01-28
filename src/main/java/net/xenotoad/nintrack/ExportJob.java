package net.xenotoad.nintrack;

import javafx.concurrent.Task;

import java.nio.file.Path;

/**
 * Created by misson20000 on 2/26/17.
 */
public abstract class ExportJob extends Task<Void> {
    protected final TreeNode src;
    protected final Path dst;

    public ExportJob(TreeNode src, Path dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    protected abstract Void call() throws Exception ;

    public Path getPath() {
        return dst;
    }

    public TreeNode getNode() {
        return src;
    }
}
