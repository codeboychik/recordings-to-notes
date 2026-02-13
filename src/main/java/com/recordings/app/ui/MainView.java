package com.recordings.app.ui;

import com.recordings.app.model.SummaryResult;
import com.recordings.app.service.OpenAiSummarizationService;
import com.recordings.app.service.OpenAiTranscriptionService;
import com.recordings.app.service.RecordingPipeline;
import com.recordings.app.service.VideoAudioExtractor;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

public class MainView {

    private final Stage stage;
    private final Label selectedFileLabel = new Label("No MP4 selected");
    private final Label statusLabel = new Label("Ready");
    private final TextArea summaryArea = new TextArea();
    private final Label titleLabel = new Label("Summary");
    private final ListView<String> actionPointsList = new ListView<>();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    private File selectedFile;

    public MainView(Stage stage) {
        this.stage = stage;
    }

    public Parent build() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        VBox leftPanel = new VBox(14);
        leftPanel.setPadding(new Insets(24));
        leftPanel.getStyleClass().add("panel");

        Label appTitle = new Label("Recordings to Notes");
        appTitle.getStyleClass().add("app-title");

        Label subtitle = new Label("MP4 ➝ transcript ➝ concise summary + action points");
        subtitle.getStyleClass().add("subtitle");

        Button selectButton = new Button("Choose MP4");
        selectButton.getStyleClass().add("primary");
        selectButton.setOnAction(event -> chooseFile());

        Button processButton = new Button("Process Recording");
        processButton.getStyleClass().add("accent");
        processButton.setOnAction(event -> processRecording());

        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(28, 28);

        HBox controls = new HBox(10, selectButton, processButton, progressIndicator);
        controls.setAlignment(Pos.CENTER_LEFT);

        selectedFileLabel.getStyleClass().add("file-label");
        statusLabel.getStyleClass().add("status");

        leftPanel.getChildren().addAll(appTitle, subtitle, controls, selectedFileLabel, statusLabel);
        leftPanel.setPrefWidth(420);

        VBox rightPanel = new VBox(12);
        rightPanel.setPadding(new Insets(24));
        rightPanel.getStyleClass().add("panel");

        titleLabel.getStyleClass().add("section-title");

        summaryArea.setWrapText(true);
        summaryArea.setEditable(false);
        summaryArea.setPromptText("Your summary will appear here...");
        VBox.setVgrow(summaryArea, Priority.ALWAYS);

        Label actionsTitle = new Label("Action Points");
        actionsTitle.getStyleClass().add("section-title");
        actionPointsList.setPrefHeight(200);

        rightPanel.getChildren().addAll(titleLabel, summaryArea, actionsTitle, actionPointsList);

        HBox content = new HBox(18, leftPanel, rightPanel);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        root.setCenter(content);

        return root;
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose MP4 recording");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP4 Files", "*.mp4"));
        File chosen = chooser.showOpenDialog(stage);
        if (chosen != null) {
            selectedFile = chosen;
            selectedFileLabel.setText("Selected: " + selectedFile.getAbsolutePath());
            statusLabel.setText("Ready to process");
        }
    }

    private void processRecording() {
        if (selectedFile == null) {
            statusLabel.setText("Please choose an MP4 file first.");
            return;
        }

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            statusLabel.setText("Missing OPENAI_API_KEY environment variable.");
            return;
        }

        progressIndicator.setVisible(true);
        statusLabel.setText("Processing...");

        Task<SummaryResult> task = new Task<>() {
            @Override
            protected SummaryResult call() throws Exception {
                RecordingPipeline pipeline = new RecordingPipeline(
                        new VideoAudioExtractor(),
                        new OpenAiTranscriptionService(apiKey),
                        new OpenAiSummarizationService(apiKey)
                );
                return pipeline.process(Path.of(selectedFile.getAbsolutePath()));
            }
        };

        task.setOnSucceeded(event -> {
            SummaryResult result = task.getValue();
            updateResult(result);
            progressIndicator.setVisible(false);
            statusLabel.setText("Done");
        });

        task.setOnFailed(event -> {
            progressIndicator.setVisible(false);
            Throwable error = task.getException();
            statusLabel.setText("Error: " + (error == null ? "Unknown" : error.getMessage()));
        });

        Thread worker = new Thread(task, "recording-pipeline");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateResult(SummaryResult result) {
        Platform.runLater(() -> {
            titleLabel.setText(result.title());
            summaryArea.setText(result.summary());
            actionPointsList.getItems().setAll(result.actionPoints());
        });
    }
}
