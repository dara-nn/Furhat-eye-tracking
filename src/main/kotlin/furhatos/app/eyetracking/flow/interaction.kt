package furhatos.app.eyetracking.flow

import furhatos.app.eyetracking.chatbot.classifyIntent
import furhatos.app.eyetracking.setting.hostPersona
import furhatos.app.eyetracking.setting.sayWithVoiceFallback
import furhatos.event.Event
import furhatos.flow.kotlin.*
import furhatos.gestures.Gestures
import kotlin.concurrent.thread

data class GeminiChatReply(val reply: String) : Event()
data class ConsentIntentClassified(val label: String) : Event()
data class ConsentQuestionAnswered(val reply: String) : Event()
data class ReadyIntentClassified(val label: String) : Event()
data class ReadyQuestionAnswered(val reply: String) : Event()

private fun isRecognizerNonSpeech(text: String): Boolean {
    val normalized = text.trim().uppercase()
    return normalized.isEmpty() ||
        normalized == "NOMATCH" ||
        normalized == "NO_MATCH" ||
        normalized.startsWith("ERROR")
}

private fun isStartGreeting(text: String): Boolean {
    val normalized = text
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")

    val greetings = listOf(
        "hi",
        "hello",
        "hey",
        "howdy",
        "greetings",
        "good morning",
        "good afternoon",
        "good evening",
        "sup",
        "hiya",
        "hey furhat",
        "hello furhat",
        "hi furhat"
    )

    return greetings.any { greeting -> normalized.contains(greeting) }
}

val Parent: State = state() {}

val Idle: State = state(Parent) {
    onEntry {
        furhat.attendNobody()
        furhat.gesture(Gestures.CloseEyes)
        println(">>> ROBOT_LISTENING: IDLE")
        furhat.listen(timeout = 30000, endSil = 4000, maxSpeech = 10000)
    }

    onReentry {
        println(">>> ROBOT_LISTENING: IDLE")
        furhat.listen(timeout = 30000, endSil = 4000, maxSpeech = 10000)
    }


    onResponse {
        if (isStartGreeting(it.text)) {
            furhat.gesture(Gestures.OpenEyes)
            if (users.count > 0) furhat.attend(users.random)
            goto(BriefingAndConsent)
        } else {
            reentry()
        }
    }

    onNoResponse {
        reentry()
    }
}

// Briefing & Consent
val BriefingAndConsent: State = state(Parent) {
    var consentRetryCount = 0
    var silenceCount = 0
    var quietMode = false


    onEntry {
        consentRetryCount = 0
        silenceCount = 0
        quietMode = false
        furhat.attend(users.random)
        furhat.gesture(Gestures.BigSmile)
        sayWithVoiceFallback("Hi! I am Iris — I love to meet new people. I am so glad you are here today!")
        delay(500)
        sayWithVoiceFallback("Before we start, I need to ask for your permission. We would like to record this session — that includes the data from the glasses you are wearing right now, as well as video and audio. This data is only for this research, and I promise all recordings will be permanently deleted once the course is over. Do you give your consent? And please feel free to ask me anything if you have any questions!")
        println(">>> ROBOT_LISTENING: CONSENT")
        furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
    }

    onReentry {
        sayWithVoiceFallback("Do you give your consent to the recording of this session?")
        println(">>> ROBOT_LISTENING: CONSENT_RETRY")
        furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
    }

    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            if (quietMode) {
                furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
            } else {
                silenceCount++
                if (silenceCount >= 3) {
                    quietMode = true
                    sayWithVoiceFallback("No worries at all. I will be quiet for now. Just let me know whenever you would like to resume.")
                    furhat.attendNobody()
                    furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
                } else {
                    delay(1200)
                    sayWithVoiceFallback("Do you give your consent to the recording of this session?")
                    println(">>> ROBOT_LISTENING: CONSENT_RETRY")
                    furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
                }
            }
            return@onResponse
        }

        if (quietMode) {
            quietMode = false
            silenceCount = 0
            furhat.attend(users.random)
        }

        furhat.gesture(Gestures.BrowRaise)
        val userText = it.text
        val flow = this
        thread {
            val label = classifyIntent(
                "Do you give your consent to the recording of this session?",
                userText,
                "- yes\n- no\n- questions"
            )
            flow.send(ConsentIntentClassified(label))
        }
    }

    onEvent<ConsentIntentClassified> {
        if (users.count > 0) furhat.attend(users.random)
        when {
            it.label.contains("yes") -> {
                furhat.gesture(Gestures.Nod)
                sayWithVoiceFallback("Thank you! I really appreciate that.")
                println(">>> RECORDING_START")
                goto(PreConversation)
            }
            it.label.contains("no") -> {
                consentRetryCount++
                if (consentRetryCount == 1) {
                    sayWithVoiceFallback("I understand completely. Please know that we cannot proceed with the study without your explicit consent. If you prefer not to participate, we will simply end the session right here. Would you rather explicitly consent and continue, or should we stop?")
                    println(">>> ROBOT_LISTENING: CONSENT_RETRY")
                    furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
                } else {
                    sayWithVoiceFallback("I completely respect your decision. Thank you so much for your time, and have a wonderful day.")
                    goto(Idle)
                }
            }
            it.label.contains("questions") -> {
                val flow = this
                thread {
                    val reply = hostPersona.chatbot.getResponse()
                    flow.send(ConsentQuestionAnswered(reply))
                }
            }
            else -> {
                sayWithVoiceFallback("I could not catch that. Please say it again.")
                println(">>> ROBOT_LISTENING: CONSENT_RETRY")
                furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
            }
        }
    }

    onEvent<ConsentQuestionAnswered> {
        if (users.count > 0) furhat.attend(users.random)
        sayWithVoiceFallback(it.reply)
        println(">>> ROBOT_LISTENING: CONSENT_RETRY")
        furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
    }

    onNoResponse {
        if (quietMode) {
            furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
        } else {
            silenceCount++
            if (silenceCount >= 3) {
                quietMode = true
                sayWithVoiceFallback("No worries at all. I will be quiet for now. Just let me know whenever you would like to resume.")
                furhat.attendNobody()
                furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
            } else {
                furhat.gesture(Gestures.BrowFrown)
                delay(1500)
                sayWithVoiceFallback("Do you consent to the collection of eye-tracking, video, and audio data for this study?")
                println(">>> ROBOT_LISTENING: CONSENT_RETRY")
                furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
            }
        }
    }
}

// Pre-conversation transition
val PreConversation: State = state(Parent) {
    var silenceCount = 0
    var quietMode = false


    onEntry {
        silenceCount = 0
        quietMode = false
        sayWithVoiceFallback("Alright! So we are just going to have a chat — nothing formal, just you and me talking.")
        delay(1000)
        sayWithVoiceFallback("Ready whenever you are!")
        println(">>> ROBOT_LISTENING: PRE_CONVERSATION")
        furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
    }

    onReentry {
        sayWithVoiceFallback("Just say ready whenever you would like to start.")
        println(">>> ROBOT_LISTENING: PRE_CONVERSATION_RETRY")
        furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
    }

    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            if (quietMode) {
                furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
            } else {
                silenceCount++
                if (silenceCount >= 3) {
                    quietMode = true
                    sayWithVoiceFallback("No rush at all. I will be quiet for now. Just say something whenever you would like to start.")
                    furhat.attendNobody()
                    furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
                } else {
                    delay(500)
                    reentry()
                }
            }
            return@onResponse
        }

        if (quietMode) {
            quietMode = false
            silenceCount = 0
            furhat.attend(users.random)
        }

        furhat.gesture(Gestures.BrowRaise)
        val userText = it.text
        val flow = this
        thread {
            val label = classifyIntent(
                "Are you ready to begin?",
                userText,
                "- yes\n- no\n- questions"
            )
            flow.send(ReadyIntentClassified(label))
        }
    }

    onEvent<ReadyIntentClassified> {
        if (users.count > 0) furhat.attend(users.random)
        when {
            it.label.contains("yes") -> {
                furhat.gesture(Gestures.Nod)
                goto(Conversation)
            }
            it.label.contains("no") -> {
                sayWithVoiceFallback("No rush whatsoever. I will wait until you are ready.")
                println(">>> ROBOT_LISTENING: PRE_CONVERSATION_RETRY")
                furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
            }
            it.label.contains("questions") -> {
                val flow = this
                thread {
                    val reply = hostPersona.chatbot.getResponse()
                    flow.send(ReadyQuestionAnswered(reply))
                }
            }
            else -> {
                sayWithVoiceFallback("I could not catch that. Please say it again.")
                println(">>> ROBOT_LISTENING: PRE_CONVERSATION_RETRY")
                furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
            }
        }
    }

    onEvent<ReadyQuestionAnswered> {
        if (users.count > 0) furhat.attend(users.random)
        sayWithVoiceFallback(it.reply)
        println(">>> ROBOT_LISTENING: PRE_CONVERSATION_RETRY")
        furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
    }

    onNoResponse {
        if (quietMode) {
            furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
        } else {
            silenceCount++
            if (silenceCount >= 3) {
                quietMode = true
                sayWithVoiceFallback("No rush at all. I will be quiet for now. Just say something whenever you would like to start.")
                furhat.attendNobody()
                furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
            } else {
                delay(500)
                reentry()
            }
        }
    }
}

// Open-ended conversation
val Conversation: State = state(Parent) {
    var startTime = 0L
    var silenceCount = 0
    var quietMode = false


    onEntry {
        startTime = System.currentTimeMillis()
        silenceCount = 0
        quietMode = false
        sayWithVoiceFallback("Let me start! I am Iris — I was created just a few days ago, so I am still very new to this world. I guess my job is having conversations and learning from interesting people, which I love.")
        delay(500)
        sayWithVoiceFallback("And my favorite thing so far? Hearing stories about places I have never been and things I have never experienced.")
        delay(500)
        sayWithVoiceFallback("Okay, your turn — what is your name, and what do you do or study?")
        println(">>> ROBOT_LISTENING: CONVERSATION_OPEN")
        furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
    }

    onReentry {
        sayWithVoiceFallback("Take your time. I am right here whenever you would like to continue.")
        println(">>> ROBOT_LISTENING: CONVERSATION_RETRY")
        furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
    }

    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            if (quietMode) {
                furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
            } else {
                val elapsed = System.currentTimeMillis() - startTime
                silenceCount++
                if (elapsed > 420_000L) {
                    sayWithVoiceFallback("I feel like we could talk for hours, but I think that is a good place to pause. Thank you so much for sharing — I really enjoyed hearing all of that.")
                    goto(Conclusion)
                } else if (silenceCount >= 3) {
                    quietMode = true
                    sayWithVoiceFallback("No worries, take your time. I will be quiet for now. Whenever you would like to continue, just start speaking.")
                    furhat.attendNobody()
                    furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
                } else {
                    delay(500)
                    reentry()
                }
            }
            return@onResponse
        }

        if (quietMode) {
            quietMode = false
            silenceCount = 0
            furhat.attend(users.random)
        }

        furhat.gesture(Gestures.Nod)
        silenceCount = 0

        val flow = this
        thread {
            val reply = hostPersona.chatbot.getResponse()
            flow.send(GeminiChatReply(reply))
        }
    }

    onEvent<GeminiChatReply> {
        if (users.count > 0) furhat.attend(users.random)
        val elapsed = System.currentTimeMillis() - startTime
        sayWithVoiceFallback(it.reply)
        if (elapsed > 420_000L) {
            delay(500)
            sayWithVoiceFallback("I feel like we could talk about this all day — but I think that is a good place to wrap up. I really enjoyed hearing about that.")
            goto(Conclusion)
        } else {
            println(">>> ROBOT_LISTENING: CONVERSATION_TURN")
            furhat.listen(timeout = 20000, endSil = 4000, maxSpeech = 60000)
        }
    }

    onNoResponse {
        if (quietMode) {
            furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
        } else {
            val elapsed = System.currentTimeMillis() - startTime
            silenceCount++

            if (elapsed > 420_000L) {
                sayWithVoiceFallback("I feel like we could talk for hours, but I think that is a good place to pause. Thank you so much for sharing — I really enjoyed hearing all of that.")
                goto(Conclusion)
            } else if (silenceCount >= 3) {
                quietMode = true
                sayWithVoiceFallback("No worries, take your time. I will be quiet for now. Whenever you would like to continue, just start speaking.")
                furhat.attendNobody()
                furhat.listen(timeout = 60000, endSil = 4000, maxSpeech = 60000)
            } else {
                furhat.gesture(Gestures.BrowRaise)
                delay(500)
                reentry()
            }
        }
    }
}

// Conclusion
val Conclusion: State = state(Parent) {
    onEntry {
        furhat.gesture(Gestures.Smile)
        sayWithVoiceFallback("It has been so nice talking with you today — I genuinely enjoyed our conversation, thank you for that. When you are ready, just head outside with your glasses on and Dara will take it from there. Have a nice rest of your day — bye bye!")
        delay(2000)
        goto(Idle)
    }
}
