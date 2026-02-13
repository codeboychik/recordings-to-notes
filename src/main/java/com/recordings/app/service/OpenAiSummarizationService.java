package com.recordings.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recordings.app.model.SummaryResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class OpenAiSummarizationService implements SummarizationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey;

    public OpenAiSummarizationService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public SummaryResult summarize(String transcript) throws IOException, InterruptedException {
        String prompt = "You are an operations analyst. Return strict JSON with keys: title (string), summary (string), actionPoints (array of strings). "
                + "Keep action points specific and assigned to implied owner roles when possible. Transcript:\n" + transcript;

        String requestJson = """
                {
                  "model":"gpt-4o-mini",
                  "messages":[
                    {"role":"system","content":"You create concise notes with action points."},
                    {"role":"user","content":%s}
                  ],
                  "response_format":{"type":"json_object"},
                  "temperature":0.2
                }
                """.formatted(MAPPER.writeValueAsString(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI summarization failed: " + response.statusCode() + "\n" + response.body());
        }

        JsonNode root = MAPPER.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText();
        JsonNode summaryJson = MAPPER.readTree(content);

        List<String> actionPoints = new ArrayList<>();
        for (JsonNode node : summaryJson.path("actionPoints")) {
            actionPoints.add(node.asText());
        }

        return new SummaryResult(
                summaryJson.path("title").asText("Recording Summary"),
                summaryJson.path("summary").asText(""),
                actionPoints
        );
    }
}
