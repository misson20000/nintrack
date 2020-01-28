package net.xenotoad.nintrack;

import javafx.concurrent.Task;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Collections;
import java.util.List;

/**
 * Created by misson20000 on 2/24/17.
 */
public interface FileType {
    String getName();
    default Task<Void> extract(TreeNode node) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                throw new NotExtractableException();
            }
        };
    }
    boolean canBeExtracted();
    String getCheckboxName();
    boolean openByDefault();
    default ImageView getIcon() {
        return new ImageView(new Rectangle(12, 12, Color.CORNFLOWERBLUE).snapshot(null, null));
    }

    default List<MenuItem> getContextMenuItems() {
        return Collections.emptyList();
    }
}
