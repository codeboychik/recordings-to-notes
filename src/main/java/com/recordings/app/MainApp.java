package com.recordings.app;

import com.formdev.flatlaf.FlatDarkLaf;
import com.recordings.app.ui.MainView;

import javax.swing.SwingUtilities;

public class MainApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            MainView mainView = new MainView();
            mainView.setVisible(true);
        });
    }
}
