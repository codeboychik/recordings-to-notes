package com.recordings.app.service;

import java.nio.file.Path;

public interface TranscriptionService {
    String transcribe(Path wavFile) throws Exception;
}
