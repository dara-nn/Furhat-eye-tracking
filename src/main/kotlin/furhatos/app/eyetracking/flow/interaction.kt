package furhatos.app.eyetracking.flow

import furhatos.app.eyetracking.chatbot.classifyIntent
import furhatos.app.eyetracking.setting.hostPersona
import furhatos.app.eyetracking.setting.sayWithVoiceFallback
import furhatos.flow.kotlin.*
import furhatos.gestures.Gestures

private fun isRecognizerNonSpeech(text: String): Boolean {
    val normalized = text.trim().uppercase()
    return normalized.isEmpty() ||
        normalized == "NOMATCH" ||
        normalized == "NO_MATCH" ||
        normalized.startsWith("ERROR")
}

val Idle: State = state() {
    onEntry {
        furhat.attendNobody()
    }
}

// Phase 1: Briefing & Consent
val BriefingAndConsent: State = state() {
    var consentRetryCount = 0
    var silenceCount = 0
    var quietMode = false

    onEntry {
        consentRetryCount = 0
        silenceCount = 0
        quietMode = false
        furhat.attend(users.random)
        furhat.gesture(Gestures.BigSmile)
        sayWithVoiceFallback("Hello, and welcome! Dara and I are so glad you could join us today. I am Iris, your moderator for this session. We are conducting a short study to understand how people interact in conversational settings.")
        delay(500)
        sayWithVoiceFallback("Before we begin, do you consent to being recorded for this research? This includes eye-tracking data, video footage, and audio.")
        println(">>> ROBOT_LISTENING: CONSENT")
        furhat.listen(timeout = 10000, endSil = 5000)
    }

    onReentry {
        sayWithVoiceFallback("Do you consent to the collection of eye-tracking, video, and audio data for this study?")
        println(">>> ROBOT_LISTENING: CONSENT_RETRY")
        furhat.listen(timeout = 10000, endSil = 5000)
    }

    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            if (quietMode) {
                goto(Idle)
                return@onResponse
            }
            silenceCount++
            if (silenceCount >= 3) {
                quietMode = true
                sayWithVoiceFallback("No worries at all. I will be quiet for now. Just let me know whenever you would like to resume.")
                furhat.attendNobody()
                furhat.listen(timeout = 60000, endSil = 5000)
            } else {
                delay(1200)
                sayWithVoiceFallback("Do you consent to the collection of eye-tracking, video, and audio data for this study?")
                println(">>> ROBOT_LISTENING: CONSENT_RETRY")
                furhat.listen(timeout = 10000, endSil = 5000)
            }
            return@onResponse
        }

        if (quietMode) {
            quietMode = false
            silenceCount = 0
            furhat.attend(users.random)
        }

        val label = classifyIntent(
            "Before we begin, do you consent to being recorded for this research? This includes eye-tracking data, video footage, and audio.",
            it.text,
            "- yes\n- no\n- questions"
        )

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Nod)
                sayWithVoiceFallback("Thank you very much. Your consent is greatly appreciated.")
                goto(CheckGlasses)
            }
            label.contains("no") -> {
                consentRetryCount++
                if (consentRetryCount == 1) {
                    sayWithVoiceFallback("I understand completely. Please know that we cannot proceed with the study without your explicit consent. If you prefer not to participate, we will simply end the session right here. Would you rather explicitly consent and continue, or should we stop?")
                    println(">>> ROBOT_LISTENING: CONSENT_RETRY")
                    furhat.listen(timeout = 10000, endSil = 5000)
                } else {
                    sayWithVoiceFallback("I completely respect your decision. Thank you so much for your time, and have a wonderful day.")
                    goto(Idle)
                }
            }
            label.contains("questions") -> {
                val reply = hostPersona.chatbot.getResponse()
                sayWithVoiceFallback(reply)
                println(">>> ROBOT_LISTENING: CONSENT_RETRY")
                furhat.listen(timeout = 10000, endSil = 5000)
            }
            else -> {
                sayWithVoiceFallback("I could not catch that. Please say it again.")
                println(">>> ROBOT_LISTENING: CONSENT_RETRY")
                furhat.listen(timeout = 10000, endSil = 5000)
            }
        }
    }

    onNoResponse {
        if (quietMode) {
            goto(Idle)
            return@onNoResponse
        }

        silenceCount++
        if (silenceCount >= 3) {
            quietMode = true
            sayWithVoiceFallback("No worries at all. I will be quiet for now. Just let me know whenever you would like to resume.")
            furhat.attendNobody()
            furhat.listen(timeout = 60000, endSil = 5000)
        } else {
            furhat.gesture(Gestures.BrowFrown)
            delay(1500)
            sayWithVoiceFallback("Do you consent to the collection of eye-tracking, video, and audio data for this study?")
            println(">>> ROBOT_LISTENING: CONSENT_RETRY")
            furhat.listen(timeout = 10000, endSil = 5000)
        }
    }
}

// Phase 2: Check Glasses & Calibrate
val CheckGlasses: State = state() {
    var silenceCount = 0
    var quietMode = false

    onEntry {
        silenceCount = 0
        quietMode = false
        sayWithVoiceFallback("Have you put on the Tobii Pro eye-tracking glasses?")
        println(">>> ROBOT_LISTENING: GLASSES")
        furhat.listen(timeout = 20000, endSil = 5000)
    }
    
    onReentry {
        sayWithVoiceFallback("Are you wearing the glasses now?")
        println(">>> ROBOT_LISTENING: GLASSES_RETRY")
        furhat.listen(timeout = 20000, endSil = 5000)
    }
    
    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            if (quietMode) {
                goto(Idle)
                return@onResponse
            }
            silenceCount++
            if (silenceCount >= 3) {
                quietMode = true
                sayWithVoiceFallback("I will be quiet for now. Whenever you are ready, just let me know.")
                furhat.attendNobody()
                furhat.listen(timeout = 60000, endSil = 5000)
            } else {
                delay(3000)
                reentry()
            }
            return@onResponse
        }

        if (quietMode) {
            quietMode = false
            silenceCount = 0
            furhat.attend(users.random)
        }

        val label = classifyIntent(
            "Have you put on the Tobii Pro eye-tracking glasses?",
            it.text,
            "- yes\n- no\n- questions"
        )

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Nod)
                goto(TobiiCalibration)
            }
            label.contains("no") -> {
                sayWithVoiceFallback("Please take your time to put them on and turn them on. Let me know when you are ready.")
                println(">>> ROBOT_LISTENING: GLASSES_RETRY")
                furhat.listen(timeout = 20000, endSil = 5000)
            }
            label.contains("questions") -> {
                val reply = hostPersona.chatbot.getResponse()
                sayWithVoiceFallback(reply)
                println(">>> ROBOT_LISTENING: GLASSES_RETRY")
                furhat.listen(timeout = 20000, endSil = 5000)
            }
            else -> {
                sayWithVoiceFallback("I could not catch that. Please say it again.")
                println(">>> ROBOT_LISTENING: GLASSES_RETRY")
                furhat.listen(timeout = 20000, endSil = 5000)
            }
        }
    }
    
    onNoResponse {
        if (quietMode) {
            goto(Idle)
            return@onNoResponse
        }

        silenceCount++
        if (silenceCount >= 3) {
            quietMode = true
            sayWithVoiceFallback("I will be quiet for now. Whenever you are ready, just let me know.")
            furhat.attendNobody()
            furhat.listen(timeout = 60000, endSil = 5000)
        } else {
            delay(8000)
            sayWithVoiceFallback("Please let me know when you have put on the glasses.")
            reentry()
        }
    }
}

val TobiiCalibration: State = state() {
    var silenceCount = 0
    var quietMode = false

    onEntry {
        silenceCount = 0
        quietMode = false
        sayWithVoiceFallback("Great. Now we need to calibrate them. Please sit comfortably and look directly at the center of the calibration card currently placed in front of you.")
        delay(4000)
        sayWithVoiceFallback("Please tell me when the calibration process is successful, or just say — I am ready.")
        println(">>> ROBOT_LISTENING: CALIBRATION")
        furhat.listen(timeout = 20000, endSil = 5000)
    }

    onReentry {
        sayWithVoiceFallback("Is the calibration successful? Are you ready to begin?")
        println(">>> ROBOT_LISTENING: CALIBRATION_RETRY")
        furhat.listen(timeout = 20000, endSil = 5000)
    }

    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            if (quietMode) {
                goto(Idle)
                return@onResponse
            }
            silenceCount++
            if (silenceCount >= 3) {
                quietMode = true
                sayWithVoiceFallback("Take all the time you need. I will be quiet and wait. Just speak up whenever you are ready.")
                furhat.attendNobody()
                furhat.listen(timeout = 60000, endSil = 5000)
            } else {
                delay(3000)
                reentry()
            }
            return@onResponse
        }

        if (quietMode) {
            quietMode = false
            silenceCount = 0
            furhat.attend(users.random)
        }

        val label = classifyIntent(
            "Is the calibration successful? Are you ready to begin?",
            it.text,
            "- yes\n- no\n- questions"
        )

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Smile)
                sayWithVoiceFallback("Excellent. Calibration is complete.")
                goto(PreTask1Transition)
            }
            label.contains("no") -> {
                sayWithVoiceFallback("Take your time. Just let me know when you are successfully calibrated and ready to continue.")
                println(">>> ROBOT_LISTENING: CALIBRATION_RETRY")
                furhat.listen(timeout = 20000, endSil = 5000)
            }
            label.contains("questions") -> {
                val reply = hostPersona.chatbot.getResponse()
                sayWithVoiceFallback(reply)
                println(">>> ROBOT_LISTENING: CALIBRATION_RETRY")
                furhat.listen(timeout = 20000, endSil = 5000)
            }
            else -> {
                sayWithVoiceFallback("I could not catch that. Please say it again.")
                println(">>> ROBOT_LISTENING: CALIBRATION_RETRY")
                furhat.listen(timeout = 20000, endSil = 5000)
            }
        }
    }

    onNoResponse {
        if (quietMode) {
            goto(Idle)
            return@onNoResponse
        }

        silenceCount++
        if (silenceCount >= 3) {
            quietMode = true
            sayWithVoiceFallback("Take all the time you need. I will be quiet and wait. Just speak up whenever you are ready.")
            furhat.attendNobody()
            furhat.listen(timeout = 60000, endSil = 5000)
        } else {
            delay(8000)
            reentry()
        }
    }
}

// Inter-Phase Transition 1
val PreTask1Transition: State = state() {
    var silenceCount = 0
    var quietMode = false

    onEntry {
        silenceCount = 0
        quietMode = false
        sayWithVoiceFallback("We are about to begin the first conversational task.")
        delay(1000)
        sayWithVoiceFallback("Please take a moment to relax and sit comfortably in your chair. We want you to be as natural as possible, just act exactly how you normally would.")
        delay(2000)
        sayWithVoiceFallback("Okay, now — are you ready to begin?")
        println(">>> ROBOT_LISTENING: PRE_TASK1")
        furhat.listen(timeout = 40000, endSil = 5000)
    }

    onReentry {
        sayWithVoiceFallback("Please say — I am ready — whenever you would like to start the first task.")
        println(">>> ROBOT_LISTENING: PRE_TASK1_RETRY")
        furhat.listen(timeout = 40000, endSil = 5000)
    }

    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            if (quietMode) {
                goto(Idle)
                return@onResponse
            }
            silenceCount++
            if (silenceCount >= 3) {
                quietMode = true
                sayWithVoiceFallback("No rush at all. I will be quiet for now. Just say something whenever you would like to start.")
                furhat.attendNobody()
                furhat.listen(timeout = 60000, endSil = 5000)
            } else {
                delay(3000)
                reentry()
            }
            return@onResponse
        }

        if (quietMode) {
            quietMode = false
            silenceCount = 0
            furhat.attend(users.random)
        }

        val label = classifyIntent(
            "Are you ready to begin the first task?",
            it.text,
            "- yes\n- no\n- questions"
        )

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Nod)
                goto(Task1_StructuredQA)
            }
            label.contains("no") -> {
                sayWithVoiceFallback("No rush whatsoever. I will wait until you are ready.")
                println(">>> ROBOT_LISTENING: PRE_TASK1_RETRY")
                furhat.listen(timeout = 40000, endSil = 5000)
            }
            label.contains("questions") -> {
                val reply = hostPersona.chatbot.getResponse()
                sayWithVoiceFallback(reply)
                println(">>> ROBOT_LISTENING: PRE_TASK1_RETRY")
                furhat.listen(timeout = 40000, endSil = 5000)
            }
            else -> {
                sayWithVoiceFallback("I could not catch that. Please say it again.")
                println(">>> ROBOT_LISTENING: PRE_TASK1_RETRY")
                furhat.listen(timeout = 40000, endSil = 5000)
            }
        }
    }

    onNoResponse {
        if (quietMode) {
            goto(Idle)
            return@onNoResponse
        }

        silenceCount++
        if (silenceCount >= 3) {
            quietMode = true
            sayWithVoiceFallback("No rush at all. I will be quiet for now. Just say something whenever you would like to start.")
            furhat.attendNobody()
            furhat.listen(timeout = 60000, endSil = 5000)
        } else {
            delay(8000)
            reentry()
        }
    }
}

// Phase 3: Task 1 (Conversation)
val Task1_StructuredQA: State = state() {
    var startTime = 0L
    var silenceCount = 0
    var quietMode = false

    onEntry {
        startTime = System.currentTimeMillis()
        silenceCount = 0
        quietMode = false
        sayWithVoiceFallback("Let's begin. For this next part, I am going to ask you an open-ended question. I would like you to just speak freely and naturally. Feel free to just say anything that comes to your head. We are going to have a back-and-forth conversation, so please elaborate as much as you would like.")
        delay(500)
        sayWithVoiceFallback("Can you tell me about your favorite travel destination and what specifically made it so memorable? Take your time to think, and again, the question is: what is your favorite travel destination, and what precisely made it so memorable to you?")
        println(">>> ROBOT_LISTENING: TASK1")
        furhat.listen(timeout = 30000, endSil = 10000)
    }

    onReentry {
        sayWithVoiceFallback("Take your time. I am right here whenever you would like to continue.")
        println(">>> ROBOT_LISTENING: TASK1_CONVERSATION_RETRY")
        furhat.listen(timeout = 30000, endSil = 10000)
    }

    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            if (quietMode) {
                goto(Idle)
                return@onResponse
            }
            val elapsed = System.currentTimeMillis() - startTime
            silenceCount++
            if (elapsed > 150_000L) {
                sayWithVoiceFallback("That brings us to the end of the first task. Thank you so much for sharing. Let us now move on.")
                goto(PreTask2Transition)
            } else if (silenceCount >= 3) {
                quietMode = true
                sayWithVoiceFallback("No worries, take your time. I will be quiet for now. Whenever you would like to continue, just start speaking.")
                furhat.attendNobody()
                furhat.listen(timeout = 60000, endSil = 5000)
            } else {
                delay(2000)
                reentry()
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
        
        val elapsed = System.currentTimeMillis() - startTime
        val reply = hostPersona.chatbot.getResponse()
        sayWithVoiceFallback(reply)
        
        if (elapsed > 150_000L) { // Limit to ~2.5 minutes
            delay(500)
            sayWithVoiceFallback("Thank you for sharing that deeply with me. It was very insightful.")
            goto(PreTask2Transition)
        } else {
            println(">>> ROBOT_LISTENING: TASK1_CONVERSATION")
            furhat.listen(timeout = 30000, endSil = 10000)
        }
    }

    onNoResponse {
        if (quietMode) {
            goto(Idle)
            return@onNoResponse
        }

        val elapsed = System.currentTimeMillis() - startTime
        silenceCount++
        
        if (elapsed > 150_000L) {
            sayWithVoiceFallback("That brings us to the end of the first task. Thank you so much for sharing. Let us now move on.")
            goto(PreTask2Transition)
        } else if (silenceCount >= 3) {
            quietMode = true
            sayWithVoiceFallback("No worries, take your time. I will be quiet for now. Whenever you would like to continue, just start speaking.")
            furhat.attendNobody()
            furhat.listen(timeout = 60000, endSil = 5000)
        } else {
            furhat.gesture(Gestures.BrowRaise)
            delay(5000)
            reentry()
        }
    }
}

// Inter-Phase Transition 2
val PreTask2Transition: State = state() {
    var silenceCount = 0
    var quietMode = false

    onEntry {
        silenceCount = 0
        quietMode = false
        sayWithVoiceFallback("That concludes the speaking task.")
        delay(1000)
        sayWithVoiceFallback("Before we move on to the final listening task, feel free to take another break. Please tell me whenever you are ready to continue.")
        println(">>> ROBOT_LISTENING: PRE_TASK2")
        furhat.listen(timeout = 40000, endSil = 5000)
    }

    onReentry {
        sayWithVoiceFallback("Simply say — ready — when you would like me to begin the story.")
        println(">>> ROBOT_LISTENING: PRE_TASK2_RETRY")
        furhat.listen(timeout = 40000, endSil = 5000)
    }

    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            if (quietMode) {
                goto(Idle)
                return@onResponse
            }
            silenceCount++
            if (silenceCount >= 3) {
                quietMode = true
                sayWithVoiceFallback("I will wait quietly. Whenever you feel ready, just let me know.")
                furhat.attendNobody()
                furhat.listen(timeout = 60000, endSil = 5000)
            } else {
                delay(3000)
                reentry()
            }
            return@onResponse
        }

        if (quietMode) {
            quietMode = false
            silenceCount = 0
            furhat.attend(users.random)
        }

        val label = classifyIntent(
            "Are you ready for the listening task?",
            it.text,
            "- yes\n- no\n- questions"
        )

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Smile)
                goto(Task2_Monologue)
            }
            label.contains("no") -> {
                sayWithVoiceFallback("No rush. I am patiently waiting for you.")
                println(">>> ROBOT_LISTENING: PRE_TASK2_RETRY")
                furhat.listen(timeout = 40000, endSil = 5000)
            }
            label.contains("questions") -> {
                val reply = hostPersona.chatbot.getResponse()
                sayWithVoiceFallback(reply)
                println(">>> ROBOT_LISTENING: PRE_TASK2_RETRY")
                furhat.listen(timeout = 40000, endSil = 5000)
            }
            else -> {
                sayWithVoiceFallback("I could not catch that. Please say it again.")
                println(">>> ROBOT_LISTENING: PRE_TASK2_RETRY")
                furhat.listen(timeout = 40000, endSil = 5000)
            }
        }
    }

    onNoResponse {
        if (quietMode) {
            goto(Idle)
            return@onNoResponse
        }

        silenceCount++
        if (silenceCount >= 3) {
            quietMode = true
            sayWithVoiceFallback("I will wait quietly. Whenever you feel ready, just let me know.")
            furhat.attendNobody()
            furhat.listen(timeout = 60000, endSil = 5000)
        } else {
            delay(8000)
            reentry()
        }
    }
}


// Phase 4: Task 2 (Listening)
val Task2_Monologue: State = state() {
    onEntry {
        sayWithVoiceFallback("For the next task, I am going to tell you a short story. Please just listen and look at me as I speak.")
        delay(1000)
        
        furhat.gesture(Gestures.Smile)
        sayWithVoiceFallback("Artificial intelligence has a long and colorful history.")
        delay(1500)
        sayWithVoiceFallback("It began in the nineteen fifties with pioneers who dreamed of creating thinking machines.")
        delay(1500)
        furhat.gesture(Gestures.Nod)
        sayWithVoiceFallback("For many decades, progress was slow and filled with what was called AI winters, where funding and interest completely dried up.")
        delay(1500)
        sayWithVoiceFallback("However, with the recent explosion of deep learning and neural networks, machines can now process natural language, recognize faces, and even converse with people just like I am doing with you right now.")
        delay(1500)
        furhat.gesture(Gestures.BigSmile)
        sayWithVoiceFallback("It is incredible how quickly technology is evolving.")
        delay(1000)
        
        sayWithVoiceFallback("Did you find that little history lesson interesting?")
        println(">>> ROBOT_LISTENING: TASK2")
        furhat.listen(timeout = 20000, endSil = 5000)
    }

    onResponse {
        if (isRecognizerNonSpeech(it.text)) {
            goto(Conclusion)
        }

        val label = classifyIntent(
            "Did you find that little history lesson interesting?",
            it.text,
            "- yes\n- no\n- questions"
        )

        when {
            label.contains("yes") -> {
                sayWithVoiceFallback("I am glad to hear that.")
                goto(Conclusion)
            }
            label.contains("no") -> {
                sayWithVoiceFallback("I appreciate the honesty.")
                goto(Conclusion)
            }
            label.contains("questions") -> {
                val reply = hostPersona.chatbot.getResponse()
                sayWithVoiceFallback(reply)
                goto(Conclusion)
            }
            else -> {
                sayWithVoiceFallback("I did not catch that clearly, but that is completely fine. Let us wrap up.")
                goto(Conclusion)
            }
        }
    }

    onNoResponse {
        goto(Conclusion)
    }
}

// Phase 5: Conclusion
val Conclusion: State = state() {
    onEntry {
        sayWithVoiceFallback("This officially concludes our session today.")
        delay(500)
        furhat.gesture(Gestures.Smile)
        sayWithVoiceFallback("Dara and I want to extend a huge thank you for your time. Your participation is incredibly valuable for our research into human-robot interaction. You can now safely take off the Tobii glasses, and Dara will be right with you to wrap things up. Have a wonderful rest of your day!")
        delay(2000)
        goto(Idle)
    }
}
