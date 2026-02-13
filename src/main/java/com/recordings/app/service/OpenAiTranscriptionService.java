package com.recordings.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class OpenAiTranscriptionService implements TranscriptionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey;

    public OpenAiTranscriptionService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String transcribe(Path wavFile) throws IOException, InterruptedException {
        String boundary = "----JavaBoundary" + UUID.randomUUID();
        byte[] body = buildMultipartBody(boundary, wavFile);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/audio/transcriptions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI transcription failed: " + response.statusCode() + "\n" + response.body());
        }
        JsonNode jsonNode = MAPPER.readTree(response.body());
        return jsonNode.path("text").asText();
    }

    private byte[] buildMultipartBody(String boundary, Path wavFile) throws IOException {
        String filename = wavFile.getFileName().toString();
        byte[] fileBytes = Files.readAllBytes(wavFile);
        String part1 = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"model\"\r\n\r\n"
                + "gpt-4o-mini-transcribe\r\n";
        String part2 = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"response_format\"\r\n\r\n"
                + "json\r\n";
        String part3Headers = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n";
        String end = "\r\n--" + boundary + "--\r\n";

        byte[] p1 = part1.getBytes();
        byte[] p2 = part2.getBytes();
        byte[] p3 = part3Headers.getBytes();
        byte[] p4 = end.getBytes();

        byte[] result = new byte[p1.length + p2.length + p3.length + fileBytes.length + p4.length];
        int position = 0;
        System.arraycopy(p1, 0, result, position, p1.length);
        position += p1.length;
        System.arraycopy(p2, 0, result, position, p2.length);
        position += p2.length;
        System.arraycopy(p3, 0, result, position, p3.length);
        position += p3.length;
        System.arraycopy(fileBytes, 0, result, position, fileBytes.length);
        position += fileBytes.length;
        System.arraycopy(p4, 0, result, position, p4.length);

        return result;
    }
}
