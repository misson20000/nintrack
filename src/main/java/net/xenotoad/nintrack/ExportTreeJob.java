package net.xenotoad.nintrack;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Pair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by misson20000 on 2/26/17.
 */
public class ExportTreeJob extends ExportJob {
    private final Consumer<? super ExportJob> jobSubmitter;
    private volatile int pending; // this is how you know you're doing this right
    private volatile double progress;

    public ExportTreeJob(TreeNode src, Path dst, Consumer<? super ExportJob> jobSubmitter) {
        super(src, dst);
        this.jobSubmitter = jobSubmitter;
    }

    @Override
    protected Void call() throws Exception {
        this.updateMessage("Discovering directories...");
        List<Path> directories = new LinkedList<>();
        src.getChildren().forEach((node) -> discoverDirectories(node, dst, directories));
        this.updateMessage("Creating directories...");
        for(Path p : directories) {
            Files.createDirectory(p);
        }
        this.updateMessage("Discovering files...");
        List<Pair<TreeNode, Path>> files = new LinkedList<>();
        src.getChildren().forEach((node) -> discoverFiles(node, dst, files));
        this.updateMessage("Creating export jobs...");
        List<ExportFileJob> jobs = files.stream().map((pair) -> new ExportFileJob(pair.getKey(), pair.getValue())).collect(Collectors.toList());
        pending = jobs.size();
        jobs.forEach((job) -> job.setOnSucceeded((e) -> pending--));
        this.updateMessage("Submitting export jobs...");
        jobs.forEach(jobSubmitter);
        this.updateMessage("Waiting on export jobs...");
        while(pending > 0) { // so progress will update "properly"
            try {
                Thread.sleep(20);
            } catch(InterruptedException ignored) {
            }
            Platform.runLater(() -> { // uh oh, can't call Task.getProgress outside of the FX application thread
                progress = jobs.stream().mapToDouble(Task::getWorkDone).sum() / jobs.stream().mapToDouble(Task::getTotalWork).sum();
            });
            updateProgress(progress, 1.0);
        }
        return null;
    }

    private void discoverFiles(TreeNode node, Path parent, List<Pair<TreeNode, Path>> files) {
        Path path = parent.resolve(node.getName().getValue());
        if(node.isDirectory().getValue()) {
            src.getChildren().forEach((child) -> discoverFiles(child, path, files));
        } else {
            files.add(new Pair<>(node, path));
        }
    }

    private void discoverDirectories(TreeNode node, Path parent, List<Path> directories) {
        if(node.isDirectory().getValue()) {
            Path dir = parent.resolve(node.getName().getValue());
            directories.add(dir);
            src.getChildren().forEach((child) -> discoverDirectories(child, dir, directories));
        }
    }
}
