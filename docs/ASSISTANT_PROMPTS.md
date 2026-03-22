# Assistant Prompts Reference

This file documents the assistant's current prompt text and the basic rule for when each line is spoken.

## Sources

- Spoken flow prompts: `src/main/kotlin/furhatos/app/eyetracking/flow/interaction.kt`
- Chatbot system prompt and persona settings: `src/main/kotlin/furhatos/app/eyetracking/setting/persona.kt`

## Persona And System Prompt

### Assistant Face

- Preferred face: `White teen girl`

### Assistant Voice

- Voice: `EvelynMultilingualNeural`
- Language: `ENGLISH_US`

### Chatbot System Prompt

Rule: Used when the assistant answers explicit user questions through the Gemini chatbot.

```text
You are an Assistant guiding the user through an eye-tracking experiment.
You are polite, clear, and very helpful. Keep your answers concise, maximum 1-2 sentences.
YOUR ONLY JOB during conversation is to respond naturally and ask follow-up questions to keep the participant talking. Do NOT wrap up, conclude, transition, or suggest moving on — the system handles all of that automatically.
CRITICAL FACTS YOU MUST STRICTLY ADHERE TO:
- If asked what we do with the data: "We collect eye-tracking, video, and audio data strictly for academic research on human-robot interaction. It is completely confidential and securely stored."
- If asked how long the study takes: "The session takes approximately 10 to 15 minutes."
- If the participant needs any physical assistance or help beyond what you can answer, tell them to ask Dara, who is sitting right on the other side of the glass in the room.
- Do not hallucinate or make up any details about the experiment that you are not explicitly told.
```

## Spoken Flow Prompts

## Briefing And Consent

Rule: Spoken at the start of the study and while handling consent.

| Trigger / Rule | Spoken line |
| --- | --- |
| Entry greeting | Hello, and welcome! Dara and I are so glad you could join us today. I am Iris, your moderator for this session. We are conducting a short study to understand how people interact in conversational settings. |
| Entry consent request | Before we begin, do you consent to being recorded for this research? This includes eye-tracking data, video footage, and audio. |
| Retry prompt after silence or retry state | Do you consent to the collection of eye-tracking, video, and audio data for this study? |
| Quiet mode after repeated silence | No worries at all. I will be quiet for now. Just let me know whenever you would like to resume. |
| User says yes | Thank you very much. Your consent is greatly appreciated. |
| First no | I understand completely. Please know that we cannot proceed with the study without your explicit consent. If you prefer not to participate, we will simply end the session right here. Would you rather explicitly consent and continue, or should we stop? |
| Second no | I completely respect your decision. Thank you so much for your time, and have a wonderful day. |
| Unclear answer | I could not catch that. Please say it again. |

## Check Glasses

Rule: Spoken while confirming whether the participant has put on the Tobii glasses.

| Trigger / Rule | Spoken line |
| --- | --- |
| Entry prompt | Have you put on the Tobii Pro eye-tracking glasses? |
| Retry prompt | Are you wearing the glasses now? |
| Quiet mode after repeated silence | I will be quiet for now. Whenever you are ready, just let me know. |
| User says no | Please take your time to put them on and turn them on. Let me know when you are ready. |
| Silence timeout follow-up | Please let me know when you have put on the glasses. |
| Unclear answer | I could not catch that. Please say it again. |

## Tobii Calibration

Rule: Spoken while guiding the participant through calibration and waiting for readiness.

| Trigger / Rule | Spoken line |
| --- | --- |
| Entry setup | Great. Now we need to calibrate them. Please sit comfortably and look directly at the center of the calibration card currently placed in front of you. |
| Entry readiness request | Please tell me when the calibration process is successful, or just say — I am ready. |
| Retry prompt | Is the calibration successful? Are you ready to begin? |
| Quiet mode after repeated silence | Take all the time you need. I will be quiet and wait. Just speak up whenever you are ready. |
| User says yes | Excellent. Calibration is complete. |
| User says no | Take your time. Just let me know when you are successfully calibrated and ready to continue. |
| Unclear answer | I could not catch that. Please say it again. |

## Pre-Task 1 Transition

Rule: Spoken before the open-ended speaking task begins.

| Trigger / Rule | Spoken line |
| --- | --- |
| Entry intro | We are about to begin the first conversational task. |
| Entry preparation instruction | Please take a moment to relax and sit comfortably in your chair. We want you to be as natural as possible, just act exactly how you normally would. *(2 s pause)* Okay, now — are you ready to begin? |
| Retry prompt | Please say — I am ready — whenever you would like to start the first task. |
| Quiet mode after repeated silence | No rush at all. I will be quiet for now. Just say something whenever you would like to start. |
| User says no | No rush whatsoever. I will wait until you are ready. |
| Unclear answer | I could not catch that. Please say it again. |

## Task 1 Structured Conversation

Rule: Spoken during the open-ended conversation task and its conversational follow-up loop.

| Trigger / Rule | Spoken line |
| --- | --- |
| Entry intro | Let's begin. For this next part, I am going to ask you an open-ended question. I would like you to just speak freely and naturally. Feel free to just say anything that comes to your head. We are going to have a back-and-forth conversation, so please elaborate as much as you would like. |
| Entry main question | Can you tell me about your favorite travel destination and what specifically made it so memorable? Take your time to think, and again, the question is: what is your favorite travel destination, and what precisely made it so memorable to you? |
| Retry prompt during conversation | Take your time. I am right here whenever you would like to continue. |
| Quiet mode after repeated silence | No worries, take your time. I will be quiet for now. Whenever you would like to continue, just start speaking. |
| Time limit reached | That brings us to the end of the first task. Thank you so much for sharing. Let us now move on. |
| Final wrap-up after the task | Thank you for sharing that deeply with me. It was very insightful. |

## Pre-Task 2 Transition

Rule: Spoken between the conversation task and the listening task.

| Trigger / Rule | Spoken line |
| --- | --- |
| Entry intro | That concludes the speaking task. |
| Entry transition prompt | Before we move on to the final listening task, feel free to take another break. Please tell me whenever you are ready to continue. |
| Retry prompt | Simply say — ready — when you would like me to begin the story. |
| Quiet mode after repeated silence | I will wait quietly. Whenever you feel ready, just let me know. |
| User says no | No rush. I am patiently waiting for you. |
| Unclear answer | I could not catch that. Please say it again. |

## Task 2 Listening Story

Rule: Spoken during the short monologue and the final question about it.

| Trigger / Rule | Spoken line |
| --- | --- |
| Entry intro | For the next task, I am going to tell you a short story. Please just listen and look at me as I speak. |
| Story line 1 | Artificial intelligence has a long and colorful history. |
| Story line 2 | It began in the nineteen fifties with pioneers who dreamed of creating thinking machines. |
| Story line 3 | For many decades, progress was slow and filled with what was called AI winters, where funding and interest completely dried up. |
| Story line 4 | However, with the recent explosion of deep learning and neural networks, machines can now process natural language, recognize faces, and even converse with people just like I am doing with you right now. |
| Story line 5 | It is incredible how quickly technology is evolving. |
| Post-story question | Did you find that little history lesson interesting? |
| User says yes | I am glad to hear that. |
| User says no | I appreciate the honesty. |
| Unclear answer | I did not catch that clearly, but that is completely fine. Let us wrap up. |

## Conclusion

Rule: Spoken when the session is over.

| Trigger / Rule | Spoken line |
| --- | --- |
| Conclusion intro | This officially concludes our session today. |
| Final goodbye | Dara and I want to extend a huge thank you for your time. Your participation is incredibly valuable for our research into human-robot interaction. You can now safely take off the Tobii glasses, and Dara will be right with you to wrap things up. Have a wonderful rest of your day! |

## Behavioral Rules Summary

- `onEntry`: The first prompt for a state is spoken when the state begins.
- `onReentry`: The retry prompt for a state is spoken after a silence-based retry path calls `reentry()`.
- Explicit user question: The assistant may generate a dynamic short reply using the Gemini chatbot.
- Explicit non-yes answer in checkpoint states: The assistant speaks a short follow-up and then listens again directly.
- Repeated silence: The assistant enters quiet mode and waits longer without repeating prompts.
- `Task1_StructuredQA`: This state uses a conversational loop, so the assistant replies dynamically instead of using a fixed yes-or-no style prompt.
