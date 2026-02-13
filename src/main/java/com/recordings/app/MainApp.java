package com.recordings.app;

import com.recordings.app.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView(stage);
        Scene scene = new Scene(mainView.build(), 1240, 780);
        scene.getStylesheets().add(getClass().getResource("/fleet-like.css").toExternalForm());

        stage.setTitle("Recordings to Notes");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
