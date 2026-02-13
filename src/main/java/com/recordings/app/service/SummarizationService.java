package com.recordings.app.service;

import com.recordings.app.model.SummaryResult;

public interface SummarizationService {
    SummaryResult summarize(String transcript) throws Exception;
}
