# Recordings to Notes (Desktop)

Java desktop app that:
- accepts an MP4 recording,
- extracts audio with `ffmpeg`,
- transcribes audio via OpenAI,
- generates a concise meeting summary with explicit action points.

## Tech stack
- Java 21
- JavaFX UI (dark, IDE-inspired style)
- Java backend services
- OpenAI API for transcription + summarization

## Prerequisites
1. Java 21+
2. `ffmpeg` available on your PATH
3. OpenAI API key in env var:
   ```bash
   export OPENAI_API_KEY=your_key_here
   ```

## Run
```bash
mvn javafx:run
```

## How it works
1. Choose an `.mp4` file.
2. Click **Process Recording**.
3. The app extracts mono 16kHz WAV audio.
4. The transcript is summarized into:
   - a title,
   - a concise summary,
   - action points list.

## Notes
- The app calls OpenAI endpoints directly from Java (`HttpClient`).
- For production, consider adding:
  - local model fallback,
  - token streaming,
  - persistent history,
  - speaker diarization,
  - retry/backoff and rate-limit handling.
