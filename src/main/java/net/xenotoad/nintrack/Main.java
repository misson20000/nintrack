package net.xenotoad.nintrack;/**
 * Created by misson20000 on 2/24/17.
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.xenotoad.nintrack.exportWindow.ExportController;

import java.io.IOException;

public class Main extends Application {

    private MainController controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/export.fxml"));
            ExportController exportController = new ExportController();
            loader.setController(exportController);
            BorderPane exportRoot = loader.load();
            Stage exportStage = new Stage();
            Scene exportScene = new Scene(exportRoot);
            exportStage.setScene(exportScene);
            //exportStage.initModality(Modality.WINDOW_MODAL);
            //exportStage.initOwner(primaryStage);
            exportStage.setTitle("Nintrack - Export Operations");
            exportController.initialize(exportStage, exportScene);

            loader = new FXMLLoader(Main.class.getResource("/main.fxml"));
            controller = new MainController();
            loader.setController(controller);
            BorderPane root = loader.load();

            Scene scene = new Scene(root);
            controller.initialize(primaryStage, scene, exportController);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Nintrack");
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        controller.stop();
    }
}
