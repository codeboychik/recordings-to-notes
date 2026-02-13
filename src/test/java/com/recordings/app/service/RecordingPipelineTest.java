package com.recordings.app.service;

import com.recordings.app.model.SummaryResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordingPipelineTest {

    @Test
    void processRunsThroughAllStages() throws Exception {
        VideoAudioExtractor extractor = new VideoAudioExtractor() {
            @Override
            public Path extractAudio(Path mp4File) {
                return Path.of("/tmp/fake.wav");
            }
        };

        TranscriptionService transcriptionService = wav -> "standup discussed release and testing";
        SummarizationService summarizationService = transcript ->
                new SummaryResult("Daily Standup", "Release and testing updates", List.of("QA: finish regression suite"));

        RecordingPipeline pipeline = new RecordingPipeline(extractor, transcriptionService, summarizationService);
        SummaryResult result = pipeline.process(Path.of("meeting.mp4"));

        assertEquals("Daily Standup", result.title());
        assertEquals(1, result.actionPoints().size());
    }
}
