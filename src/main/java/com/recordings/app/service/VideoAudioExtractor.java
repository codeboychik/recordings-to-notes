package com.recordings.app.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VideoAudioExtractor {

    public Path extractAudio(Path mp4File) throws IOException, InterruptedException {
        Path outputWav = Files.createTempFile("recording-audio-", ".wav");
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(mp4File.toAbsolutePath().toString());
        command.add("-vn");
        command.add("-ac");
        command.add("1");
        command.add("-ar");
        command.add("16000");
        command.add(outputWav.toAbsolutePath().toString());

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String logs;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            logs = reader.lines().reduce("", (a, b) -> a + b + System.lineSeparator());
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("ffmpeg failed. Ensure ffmpeg is installed and on PATH.\n" + logs);
        }
        return outputWav;
    }
}
