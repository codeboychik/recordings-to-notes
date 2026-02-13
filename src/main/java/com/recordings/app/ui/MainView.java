package com.recordings.app.ui;

import com.recordings.app.model.SummaryResult;
import com.recordings.app.service.OpenAiSummarizationService;
import com.recordings.app.service.OpenAiTranscriptionService;
import com.recordings.app.service.RecordingPipeline;
import com.recordings.app.service.VideoAudioExtractor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MainView extends JFrame {

    private final JLabel selectedFileLabel = new JLabel("No MP4 selected");
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel titleLabel = new JLabel("Summary");
    private final JTextArea summaryArea = new JTextArea();
    private final DefaultListModel<String> actionPointsModel = new DefaultListModel<>();
    private final JList<String> actionPointsList = new JList<>(actionPointsModel);
    private final JProgressBar progressBar = new JProgressBar();

    private File selectedFile;

    public MainView() {
        super("Recordings to Notes");
        setupFrame();
        buildUi();
    }

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1280, 820);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.32);
        splitPane.setDividerLocation(400);
        splitPane.setBorder(null);

        root.add(splitPane, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                UIManager.getBorder("Component.border"),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel appTitle = new JLabel("Recordings to Notes");
        appTitle.setFont(appTitle.getFont().deriveFont(28f));

        JLabel subtitle = new JLabel("MP4 → transcript → concise summary + action points");

        JButton chooseButton = new JButton("Choose MP4");
        chooseButton.addActionListener(e -> chooseFile());

        JButton processButton = new JButton("Process Recording");
        processButton.addActionListener(e -> processRecording());

        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        panel.add(appTitle);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(16));
        panel.add(chooseButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(processButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(progressBar);
        panel.add(Box.createVerticalStrut(16));
        panel.add(selectedFileLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(statusLabel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                UIManager.getBorder("Component.border"),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JPanel top = new JPanel(new BorderLayout());
        titleLabel.setFont(titleLabel.getFont().deriveFont(22f));
        top.add(titleLabel, BorderLayout.WEST);

        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setEditable(false);

        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(BorderFactory.createTitledBorder("Summary"));

        JScrollPane actionsScroll = new JScrollPane(actionPointsList);
        actionsScroll.setBorder(BorderFactory.createTitledBorder("Action Points"));
        actionsScroll.setPreferredSize(new Dimension(200, 220));

        panel.add(top, BorderLayout.NORTH);
        panel.add(summaryScroll, BorderLayout.CENTER);
        panel.add(actionsScroll, BorderLayout.SOUTH);

        return panel;
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose MP4 recording");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".mp4")) {
                JOptionPane.showMessageDialog(this, "Please select an MP4 file.", "Invalid file", JOptionPane.WARNING_MESSAGE);
                selectedFile = null;
                selectedFileLabel.setText("No MP4 selected");
                return;
            }
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

        progressBar.setVisible(true);
        statusLabel.setText("Processing...");

        SwingWorker<SummaryResult, Void> worker = new SwingWorker<>() {
            @Override
            protected SummaryResult doInBackground() throws Exception {
                RecordingPipeline pipeline = new RecordingPipeline(
                        new VideoAudioExtractor(),
                        new OpenAiTranscriptionService(apiKey),
                        new OpenAiSummarizationService(apiKey)
                );
                return pipeline.process(Path.of(selectedFile.getAbsolutePath()));
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    SummaryResult result = get();
                    updateResult(result);
                    statusLabel.setText("Done");
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void updateResult(SummaryResult result) {
        titleLabel.setText(result.title());
        summaryArea.setText(result.summary());
        actionPointsModel.clear();
        List<String> actionPoints = result.actionPoints();
        for (String point : actionPoints) {
            actionPointsModel.addElement(point);
        }
    }
}
