package net.xenotoad.nintrack;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.ProgressBarTreeTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.xenotoad.nintrack.exportWindow.ExportController;
import net.xenotoad.nintrack.format.archive.u8.U8ArchiveIdentifier;
import net.xenotoad.nintrack.format.compression.lz77type11.Lz77Type11Identifier;
import net.xenotoad.nintrack.format.gamefile.brres.BRRESIdentifier;
import net.xenotoad.nintrack.format.rom.wii.WiiDiscIdentifier;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by misson20000 on 2/24/17.
 */
public class MainController  {
    @FXML private Button extractButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private TreeTableView<TreeNode> table;
    @FXML private TreeTableColumn<TreeNode, Boolean> tableColumnExtract;
    @FXML private TreeTableColumn<TreeNode, String> tableColumnName;
    @FXML private TreeTableColumn<TreeNode, Double> tableColumnProgress;
    @FXML private TreeTableColumn<TreeNode, String> tableColumnType;
    @FXML private TreeTableColumn<TreeNode, String> tableColumnStatus;
    @FXML private TreeTableColumn<TreeNode, String> tableColumnSize;
    @FXML private ProgressBar overallProgressBar;
    @FXML private CheckBox immediateCheckbox;
    @FXML private VBox formatCheckboxesContainer;

    private FileChooser fileChooser = new FileChooser();
    private DirectoryChooser directoryChooser = new DirectoryChooser();
    private TreeItem<TreeNode> rootNode = new TreeItem<>();

    private ExecutorService executor = Executors.newWorkStealingPool();
    private FileIdentifier[] identifiers = new FileIdentifier[] {
            new WiiDiscIdentifier(),
            new U8ArchiveIdentifier(),
            new BRRESIdentifier(),

            // non-magic number identifiers
            new Lz77Type11Identifier(),

    };
    private FileIdentifier multipleIdentifier = new MultipleFileIdentifier(identifiers);
    private Stage stage;
    private Scene scene;
    private ExportController exportController;

    public void initialize(Stage stage, Scene scene, ExportController exportController) {
        this.stage = stage;
        this.scene = scene;
        this.exportController = exportController;
        importButton.setOnAction((e) -> {
            fileChooser.setTitle("Import File");
            List<File> files = fileChooser.showOpenMultipleDialog(stage);
            if(files != null) {
                files.forEach((f) -> {
                    if(f.exists() && f.isFile() && f.canRead()) {
                        rootNode.getChildren().add(discover(new TreeNode(new FileByteRegion(f), f.getName())));
                    }
                });
            }
        });

        extractButton.disableProperty().bind(immediateCheckbox.selectedProperty());
        extractButton.setOnAction((e) -> {
            List<Task<Void>> submissions = new LinkedList<>();
            rootNode.getChildren().forEach((child) -> submitExtraction(child, submissions));
            submissions.forEach(executor::execute);
        });

        tableColumnName.setCellValueFactory((p) -> p.getValue().getValue().getName());
        tableColumnType.setCellValueFactory((p) -> {
            ObjectProperty<FileType> ft = p.getValue().getValue().getFileType();
            return Util.map(ft, (ftValue) -> ftValue == null ? "Identifying..." : ftValue.getName());
        });
        tableColumnStatus.setCellValueFactory((p) -> p.getValue().getValue().getStatus());
        tableColumnSize.setCellValueFactory((p) -> new ReadOnlyStringWrapper(p.getValue().getValue().getRegion() == null ? "" : Util.humanReadableByteCount(p.getValue().getValue().getRegion().getLength(), false)));
        tableColumnExtract.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(tableColumnExtract));
        tableColumnExtract.setCellValueFactory(param -> param.getValue().getValue().getShouldExtract());
        tableColumnExtract.setEditable(true);
        tableColumnExtract.setVisible(false);
        tableColumnProgress.setCellFactory(ProgressBarTreeTableCell.forTreeTableColumn());
        tableColumnProgress.setCellValueFactory((param) -> Util.asDouble(param.getValue().getValue().getRecursiveExtractionProgress()));
        table.setRowFactory(ttv -> new TreeTableRow<TreeNode>() {
            @Override
            public void updateItem(TreeNode node, boolean empty) {
                super.updateItem(node, empty);
                if(empty) {
                    setContextMenu(null);
                    setGraphic(null);
                } else {
                    ContextMenu menu = new ContextMenu();
                    MenuItem extract = new MenuItem("Extract");
                    extract.disableProperty().bind(Util.mapBoolean(node.getFileType(), (ft) -> ft != null && !ft.canBeExtracted()));
                    extract.setOnAction((e) -> executor.submit(node.getFileType().getValue().extract(node)));

                    MenuItem export = new MenuItem("Export...");
                    export.setDisable(node.getRegion() == null);
                    export.setOnAction((e) -> exportNode(node));

                    MenuItem exportTree = new MenuItem("Export Tree...");
                    exportTree.disableProperty().bind(Bindings.size(node.getChildren()).lessThanOrEqualTo(0));
                    exportTree.setOnAction((e) -> exportNodeTree(node));

                    MenuItem expandAll = new MenuItem("Expand All");
                    expandAll.setOnAction((e) -> expandAll(getTreeItem()));

                    MenuItem collapseChildren = new MenuItem("Collapse Children");
                    collapseChildren.disableProperty().bind(Bindings.size(node.getChildren()).lessThanOrEqualTo(0));
                    collapseChildren.setOnAction((e) -> collapseChildren(getTreeItem()));

                    ChangeListener<FileType> fileTypeChangeListener = (obs, oldV, newV) -> {
                        menu.getItems().clear();
                        menu.getItems().addAll(extract, export, exportTree, expandAll, collapseChildren);
                        if(newV != null) {
                            //setGraphic(newV.getIcon());
                            List<MenuItem> typeSpecificItems = newV.getContextMenuItems();
                            if (typeSpecificItems.size() > 0) {
                                menu.getItems().add(new SeparatorMenuItem());
                                menu.getItems().addAll(typeSpecificItems);
                            }
                        } else {
                            //setGraphic(null);
                        }
                    };
                    node.getFileType().addListener(fileTypeChangeListener);
                    fileTypeChangeListener.changed(node.getFileType(), null, node.getFileType().getValue());

                    setContextMenu(menu);
                }
            }
        });
        table.setShowRoot(false);
        table.setRoot(rootNode);
    }

    private void expandAll(TreeItem<TreeNode> item) {
        item.setExpanded(true);
        item.getChildren().forEach(this::expandAll);
    }

    private void collapseChildren(TreeItem<TreeNode> item) {
        item.getChildren().forEach((child) -> {
            child.setExpanded(false);
            child.getChildren().forEach(this::collapseChildren);
        });
    }

    private void exportNode(TreeNode node) {
        fileChooser.setInitialFileName(node.getName().getValue());
        fileChooser.setTitle("Export file");
        File target = fileChooser.showSaveDialog(stage);

        if(target != null) {
            ExportJob exportJob = new ExportFileJob(node, target.toPath());
            submitExportJob(exportJob);
        }
    }

    private Future<?> submitExportJob(ExportJob exportJob) {
        exportJob.setOnFailed((wse) -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Exception Exporting File");
            alert.setHeaderText("Could not export '" + exportJob.getNode().getName().getValue() + "':");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exportJob.getException().printStackTrace(pw);
            String stackTrace = sw.toString();
            TextArea stackArea = new TextArea(stackTrace);
            stackArea.setEditable(false);
            stackArea.setWrapText(true);
            stackArea.setMaxWidth(Double.MAX_VALUE);
            stackArea.setMaxHeight(Double.MAX_VALUE);
            alert.getDialogPane().setExpandableContent(stackArea);
            alert.showAndWait();
        });
        exportController.addJob(exportJob);
        return executor.submit(exportJob);
    }

    private void exportNodeTree(TreeNode node) {
        directoryChooser.setTitle("Export directory tree");
        File target = directoryChooser.showDialog(stage);
        if(target != null) {
            submitExportJob(new ExportTreeJob(node, target.toPath(), this::submitExportJob));
        }
    }

    private void submitExtraction(TreeItem<TreeNode> item, List<Task<Void>> submissions) {
        TreeNode node = item.getValue();
        if(node.getFileType().getValue().canBeExtracted()
                && !node.hasBeenExtracted()
                && ((CheckBox) scene.lookup("#" + node.getFileType().getValue().getCheckboxName())).isSelected()) {
            Task<Void> extractionTask = node.getFileType().getValue().extract(node);
            extractionTask.setOnSucceeded((wse) -> {
                node.getExtractionProgress().unbind();
                node.getExtractionProgress().setValue(1);
                node.setHasBeenExtracted(true);
            });
            extractionTask.setOnFailed((wse) -> {
                node.getStatus().unbind();
                node.getStatus().setValue(extractionTask.getException().toString());
            });
            node.getStatus().bind(extractionTask.messageProperty());
            node.getExtractionProgress().bind(extractionTask.progressProperty());

            submissions.add(extractionTask);
        }
        for (TreeItem<TreeNode> treeNodeTreeItem : item.getChildren()) {
            submitExtraction(treeNodeTreeItem, submissions);
        }
    }

    public TreeItem<TreeNode> discover(TreeNode node) {
        TreeItem<TreeNode> item = new TreeItem<>(node);
        item.setExpanded(node.getFileType().getValue() != null && node.getFileType().getValue().openByDefault());
        node.getFileType().addListener((obs, oldV, newV) -> item.setExpanded(newV.openByDefault()));
        node.getChildren().forEach((child) -> {
            item.getChildren().add(discover(child));
            child.getParent().setValue(node);
        });
        node.getChildren().addListener((ListChangeListener<TreeNode>) (change) -> {
            while(change.next()) {
                change.getRemoved().forEach((TreeNode removed) -> item.getChildren().removeIf((testingItem) -> testingItem.getValue() == removed));
                change.getAddedSubList().forEach((TreeNode newChild) -> {
                    item.getChildren().add(discover(newChild));
                    newChild.getParent().setValue(node);
                });
            }
        });
        final boolean immediate = immediateCheckbox.isSelected();
        executor.submit(() -> {
            synchronized (node) {
                FileType type = node.getFileType().getValue();
                if(type == null) {
                    if (node.getRegion() != null) {
                        type = multipleIdentifier.identify(node.getRegion());
                    } else {
                        type = DirectoryFileType.instance;
                    }
                }
                final FileType finalType = type;
                Platform.runLater(() -> {
                    node.getFileType().setValue(finalType);
                    if(!finalType.canBeExtracted()) {
                        node.getExtractionProgress().setValue(1);
                    }
                    if(immediate && finalType.canBeExtracted()
                            && ((CheckBox) formatCheckboxesContainer.lookup("#" + finalType.getCheckboxName())).isSelected()) {
                        List<Task<Void>> submissions = new LinkedList<>();
                        submitExtraction(item, submissions);
                        submissions.forEach(executor::execute);
                    }
                });
                return null;
            }
        });
        return item;
    }

    public void stop() {
        try {
            executor.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
    }

}
