# Speech Design Decisions

## ASR — Listening Parameters

All `furhat.listen()` calls use these values:

| Parameter | Value | Reason |
|---|---|---|
| `endSil` | 5000ms | 5 seconds of silence before cutting off — allows natural pauses in storytelling |
| `timeout` | 20000ms (normal) / 60000ms (quietMode) | 20s standard; 60s when user has gone silent multiple times |
| `maxSpeech` | 60000ms | Up to 60s per utterance; 10s only in Idle (not a real conversation) |

### endSil history
- Originally: 2000ms (Idle) and 3000ms (all other states)
- Increased to 5000ms everywhere — users were being cut off mid-sentence during natural pauses

## TTS — Voice & Fallback

### Primary voice
ElevenLabs (configured in `persona.kt` via Furhat SDK voice settings)

### Fallback voice
Azure `EvelynMultilingualNeural` (Female, English US) — activates automatically if ElevenLabs throws an exception. Once activated, stays for the rest of the session (`fallbackToAzureEnabled` flag prevents repeated switching).

### Double failure
If Azure also fails, the error is logged and swallowed — conversation continues silently rather than crashing.
Log: `[VOICE] Azure fallback also failed: <message>`

### Rule
Always use `sayWithVoiceFallback()` from `persona.kt` — never call `furhat.say()` directly in the flow. This ensures the ElevenLabs → Azure fallback chain always applies.
