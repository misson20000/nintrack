package net.xenotoad.nintrack;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.NumberExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Created by misson20000 on 2/25/17.
 */
public class TreeNode {
    private final ByteRegion region;
    private final SimpleStringProperty name;
    private final SimpleObjectProperty<FileType> fileType;
    private final BooleanProperty shouldExtract = new SimpleBooleanProperty(true);
    private final ListProperty<TreeNode> children = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final SimpleObjectProperty<TreeNode> parent = new SimpleObjectProperty<>();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final SimpleStringProperty status = new SimpleStringProperty("");
    private boolean hasBeenExtracted = false;

    public TreeNode(String name, FileType fileType) {
        this(null, name, fileType);
    }

    public TreeNode(ByteRegion region, String name) {
        this(region, name, null);
    }

    public TreeNode(ByteRegion region, String name, FileType fileType) {
        this.region = region;
        this.name = new SimpleStringProperty(name);
        this.fileType = new SimpleObjectProperty<>(fileType);
    }

    public StringProperty getName() {
        return name;
    }
    public SimpleObjectProperty<FileType> getFileType() {
        return fileType;
    }
    public BooleanProperty getShouldExtract() {
        return shouldExtract;
    }
    public DoubleProperty getExtractionProgress() { return progress; }

    public BooleanExpression isDirectory() {
        return Bindings.size(children).greaterThan(0);
    }
    public ByteRegion getRegion() {
        return region;
    }

    public ObservableList<TreeNode> getChildren() {
        return children;
    }

    public StringProperty getStatus() {
        return status;
    }

    public ObjectProperty<TreeNode> getParent() {
        return parent;
    }

    public StringBinding getPath() {
        return Bindings.when(parent.isNull())
                .then(name)
                .otherwise(
                        Bindings.concat(
                                Util.mapProperty(parent, (node) -> node == null
                                        ? new ReadOnlyStringWrapper("")
                                        : node.getPath()),
                                new ReadOnlyStringWrapper("/"),
                                name));
    }

    public boolean hasBeenExtracted() {
        return hasBeenExtracted;
    }

    public void setHasBeenExtracted(boolean hasBeenExtracted) {
        this.hasBeenExtracted = hasBeenExtracted;
    }

    public NumberExpression getRecursiveExtractionProgress() {
        return Bindings.divide(
                getExtractionProgress().add(
                        Util.unboxNumber(Util.collectList(
                                Util.mapList(children, TreeNode::getRecursiveExtractionProgress),
                                Util.numberSumCollector()))
                ),
                children.sizeProperty().add(1)
        );
    }
}
