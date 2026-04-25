# API Design Decisions

## Model
- **Model:** `gemini-3-flash-preview` for both chat responses and intent classification
- **Why Flash:** Fast enough for real-time conversation; full reasoning models add too much latency

## Thinking Budget
- **Removed** (`thinkingConfig` deleted from both `getResponse()` and `classifyIntent()`)
- **Why:** Added significant latency with no meaningful quality gain for casual conversation

## Timeouts

### Chat (`getResponse`)
| Setting | Value | Reason |
|---|---|---|
| connectTimeout | 5000ms | Allows for slow network handshake |
| readTimeout | 20000ms | Enough headroom for response generation |

### Intent classification (`classifyIntent`)
| Setting | Value | Reason |
|---|---|---|
| connectTimeout | 5000ms | Same as chat |
| readTimeout | 10000ms | Classification responses are shorter |

## Retry Logic

### Chat (`getResponse`)
- Retries **once** on exception (timeout, network failure)
- Non-200 HTTP responses are **not retried** — they indicate a real API rejection (bad key, quota)
- After 2 failures: returns fallback error message (see `errors.md`)

### Intent classification (`classifyIntent`)
- Retries **once** on exception
- On any failure: silently returns `"unclear"` so the state machine handles it gracefully
- No spoken filler on retry (classification is invisible to the user)

## Logging
All API activity is logged to stdout with a `>>>` prefix:

```
>>> GEMINI_CHAT attempt=1/2
>>> GEMINI_CHAT HTTP 429
>>> GEMINI_CHAT failed after 2 attempts
>>> GEMINI_CLASSIFY attempt=1/2
>>> GEMINI_CLASSIFY_ERROR attempt=1: <message>
>>> GEMINI_CLASSIFY failed after 2 attempts, returning 'unclear'
>>> GEMINI_TOKENS: prompt=X  response=X  thoughts=X  total=X
```
