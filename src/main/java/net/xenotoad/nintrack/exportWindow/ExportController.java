package net.xenotoad.nintrack.exportWindow;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.stage.Stage;
import net.xenotoad.nintrack.ExportJob;
import net.xenotoad.nintrack.Util;

import java.util.Optional;

/**
 * Created by misson20000 on 2/26/17.
 */
public class ExportController {
    private Stage stage;
    private Scene scene;

    @FXML private TableView<ExportJob> table;
    @FXML private TableColumn<ExportJob, String> pathColumn;
    @FXML private TableColumn<ExportJob, Double> progressColumn;
    @FXML private TableColumn<ExportJob, String> statusColumn;
    @FXML private Button closeButton;

    private int running = 0;

    public void initialize(Stage stage, Scene scene) {
        this.stage = stage;
        this.scene = scene;
        progressColumn.setCellFactory(ProgressBarTableCell.forTableColumn());
        progressColumn.setCellValueFactory((p) -> p.getValue().progressProperty().asObject());
        pathColumn.setCellValueFactory((p) -> p.getValue().getNode().getPath());
        statusColumn.setCellValueFactory((p) -> Util.mapStringExpression(p.getValue().stateProperty(), (state) -> {
            switch(state) {
            case CANCELLED:
                return new ReadOnlyStringWrapper("Cancelled");
            case FAILED:
                return new ReadOnlyStringWrapper("Failed: " + p.getValue().getException().getMessage());
            case RUNNING:
                StringExpression message = p.getValue().messageProperty();
                return new ReadOnlyStringWrapper("Running").concat(
                        Bindings.when(message.length().greaterThan(0))
                                .then(new ReadOnlyStringWrapper(": ").concat(message))
                                .otherwise(new ReadOnlyStringWrapper("")).concat(new ReadOnlyStringWrapper("...")));
            case SUCCEEDED:
                return new ReadOnlyStringWrapper("Succeeded");
            case READY:
            case SCHEDULED:
                return new ReadOnlyStringWrapper("Pending...");
            }
            return new ReadOnlyStringWrapper("Unknown");
        }));

        closeButton.setOnAction((e) -> {
            if(running > 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirm Cancel Export Operations");
                alert.setHeaderText("Cancel Running Export Operations?");
                alert.setHeaderText("Closing this window will cancel any running export operations.");

                ButtonType continueBtn = new ButtonType("Continue", ButtonBar.ButtonData.FINISH);
                ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(continueBtn, cancelBtn);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == cancelBtn) {
                    table.getItems().forEach(ExportJob::cancel);
                }
            }
            table.getItems().clear();
            stage.hide();
        });
    }

    public void addJob(ExportJob job) {
        job.runningProperty().addListener((o, oldValue, newValue) -> {
            if(newValue) {
                running++;
            } else {
                running--;
            }
            if(running <= 0) {
                running = 0;
            } else {
                stage.show();
            }
        });
        table.getItems().add(job);
    }
}
