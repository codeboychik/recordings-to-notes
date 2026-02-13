package com.recordings.app.service;

import com.recordings.app.model.SummaryResult;

import java.nio.file.Path;

public class RecordingPipeline {

    private final VideoAudioExtractor extractor;
    private final TranscriptionService transcriptionService;
    private final SummarizationService summarizationService;

    public RecordingPipeline(VideoAudioExtractor extractor,
                             TranscriptionService transcriptionService,
                             SummarizationService summarizationService) {
        this.extractor = extractor;
        this.transcriptionService = transcriptionService;
        this.summarizationService = summarizationService;
    }

    public SummaryResult process(Path mp4File) throws Exception {
        Path wav = extractor.extractAudio(mp4File);
        String transcript = transcriptionService.transcribe(wav);
        return summarizationService.summarize(transcript);
    }
}
