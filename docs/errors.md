# Error Messages Reference

All hardcoded fallback strings spoken by the robot when Gemini API calls fail.

**Source:** `src/main/kotlin/furhatos/app/eyetracking/chatbot/gemini.kt`

---

## Chat responses (`getResponse`)

| Situation | Message |
|---|---|
| API returned non-200 (bad key, quota exceeded) | "Sorry, I zoned out for a second — what were you saying?" |
| Both retry attempts timed out / network error | "I'm so sorry, my mind went blank there — could you say that again?" |
| Response JSON missing `candidates` field | "Sorry, I lost my train of thought for a second — could you say that again?" |
| Response JSON missing `text` field | "Sorry, I lost my train of thought for a second — could you say that again?" |
| Parsed text was empty | "Hmm, I'm not sure what to say to that — could you tell me more?" |
| JSON parsing crashed | "Sorry, I got a bit confused there — what were you saying?" |

## Intent classification (`classifyIntent`)

Classification failures are **silent** — no message is spoken. The function returns `"unclear"` and the state machine handles it (typically re-prompts the user).

Failures are logged to stdout:
```
>>> GEMINI_CLASSIFY_ERROR attempt=1: <message>
>>> GEMINI_CLASSIFY failed after 2 attempts, returning 'unclear'
```
