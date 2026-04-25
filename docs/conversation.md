# Conversation Flow Design Decisions

## Session structure

States run in this order:
1. **Idle** — robot waits with eyes closed, listening for a greeting
2. **BriefingAndConsent** — intro + recording consent
3. **PreConversation** — "we're going to have a chat, ready when you are"
4. **Conversation** — free conversation, up to 7 minutes
5. **Conclusion** — warm goodbye, guide participant to walk out

## No task labels
The robot never announces "Task", "Phase", etc. The flow feels like a natural conversation, not an experiment.

## 7-minute timer
`Conversation` has a hard 420,000ms (7-minute) limit. When it expires, the robot wraps up naturally (*"I feel like we could talk for hours..."*). Timer is checked on every user response and every no-response.

## Conversation phases (steered by system prompt, not code)
Gemini guides the conversation through phases without announcing them:
1. **Introductions** — name, what they do/study, hobbies
2. **Hometown/country** — where they're from; describe it as if Iris has never heard of it
3. **Field of study** (backup, only if time allows)

## Persona: Iris
- Brand new to the world, curious about humans and places
- Loves hearing stories about things she hasn't experienced
- Warm and conversational — never robotic or formal

## Tone rules (applied throughout all spoken lines)
- No formal or clinical language
- No research framing ("your participation is valuable...")
- No equipment instructions — Dara handles all of that
- Goodbye feels like two people ending a nice chat

## Incomplete utterance handling
If the user's speech seems cut off or mid-sentence, the system prompt tells Gemini to say something like "Please go on" or "I'm listening" rather than respond to the incomplete thought.

## Silence handling
- 1–2 silences: robot re-prompts gently
- 3+ silences: enters `quietMode` — robot goes quiet and waits up to 60 seconds
- `quietMode` exits as soon as the user speaks

## Consent flow
- Single ask with full explanation of what's recorded and why
- First refusal: robot clarifies they can't proceed without consent, offers one more chance
- Second refusal: session ends gracefully
- Questions: Gemini answers using the factual constraints in the system prompt
