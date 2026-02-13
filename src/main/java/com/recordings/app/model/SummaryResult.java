package com.recordings.app.model;

import java.util.List;

public record SummaryResult(String title, String summary, List<String> actionPoints) {
}
