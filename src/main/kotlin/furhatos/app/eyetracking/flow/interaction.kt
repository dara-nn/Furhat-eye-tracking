package furhatos.app.eyetracking.flow

import furhatos.app.eyetracking.chatbot.classifyIntent
import furhatos.app.eyetracking.setting.hostPersona
import furhatos.flow.kotlin.*
import furhatos.gestures.Gestures

val Idle: State = state() {
    onEntry {
        furhat.attendNobody()
    }
}

// Phase 1: Briefing & Consent
val BriefingAndConsent: State = state() {
    onEntry {
        furhat.attend(users.random)
        furhat.gesture(Gestures.BigSmile)
        furhat.say("Hello! Welcome to the study. I am your moderator today.")
        delay(500)
        furhat.say("Before we begin, do you consent to being recorded for this research? This includes eye-tracking data, video footage, and audio.")
        println(">>> ROBOT_LISTENING: CONSENT")
        furhat.listen(timeout = 20000)
    }

    onReentry {
        furhat.say("Do you consent to the collection of eye-tracking, video, and audio data for this study?")
        println(">>> ROBOT_LISTENING: CONSENT_RETRY")
        furhat.listen(timeout = 20000)
    }

    onResponse {
        val label = call {
            classifyIntent(
                "Before we begin, do you consent to being recorded for this research? This includes eye-tracking data, video footage, and audio.",
                it.text,
                "- yes\n- no\n- questions"
            )
        } as String

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Nod)
                furhat.say("Thank you.")
                goto(CheckGlasses)
            }
            label.contains("no") -> {
                furhat.say("I understand. We cannot proceed without your consent. Thank you for your time, and have a great day.")
                goto(Idle)
            }
            label.contains("questions") || label.contains("unclear") -> {
                val reply = call { hostPersona.chatbot.getResponse() } as String
                furhat.say(reply)
                println(">>> ROBOT_LISTENING: CONSENT_RETRY")
                furhat.listen(timeout = 20000)
            }
            else -> {
                furhat.say("I am sorry, I did not quite catch that. Could you please say yes or no?")
                reentry()
            }
        }
    }

    onNoResponse {
        furhat.gesture(Gestures.BrowFrown)
        delay(3000)
        reentry()
    }
}

// Phase 2: Check Glasses & Calibrate
val CheckGlasses: State = state() {
    onEntry {
        furhat.say("Have you put on the Tobii Pro eye-tracking glasses?")
        println(">>> ROBOT_LISTENING: GLASSES")
        furhat.listen(timeout = 20000)
    }
    
    onReentry {
        furhat.say("Are you wearing the glasses now?")
        println(">>> ROBOT_LISTENING: GLASSES_RETRY")
        furhat.listen(timeout = 20000)
    }
    
    onResponse {
        val label = call {
            classifyIntent(
                "Have you put on the Tobii Pro eye-tracking glasses?",
                it.text,
                "- yes\n- no\n- questions"
            )
        } as String

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Nod)
                goto(TobiiCalibration)
            }
            label.contains("no") -> {
                furhat.say("Please take your time to put them on and turn them on. Let me know when you are ready.")
                reentry()
            }
            label.contains("questions") -> {
                val reply = call { hostPersona.chatbot.getResponse() } as String
                furhat.say(reply)
                reentry()
            }
            else -> {
                furhat.say("I did not quite understand. Have you put on the glasses? Please say yes or no.")
                reentry()
            }
        }
    }
    
    onNoResponse {
        furhat.say("Please let me know when you have put on the glasses.")
        reentry()
    }
}

val TobiiCalibration: State = state() {
    onEntry {
        furhat.say("Great. Now we need to calibrate them. Please sit comfortably and look directly at the center of the calibration card currently placed in front of you.")
        delay(4000)
        furhat.say("Please tell me when the calibration process is successful, or just say I am ready.")
        println(">>> ROBOT_LISTENING: CALIBRATION")
        furhat.listen(timeout = 20000)
    }

    onReentry {
        furhat.say("Is the calibration successful? Are you ready to begin?")
        println(">>> ROBOT_LISTENING: CALIBRATION_RETRY")
        furhat.listen(timeout = 20000)
    }

    onResponse {
        val label = call {
            classifyIntent(
                "Is the calibration successful? Are you ready to begin?",
                it.text,
                "- yes\n- no\n- questions"
            )
        } as String

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Smile)
                furhat.say("Excellent. Calibration is complete.")
                goto(PreTask1Transition)
            }
            label.contains("no") -> {
                furhat.say("Take your time. Just let me know when you are successfully calibrated and ready to continue.")
                reentry()
            }
            label.contains("questions") -> {
                val reply = call { hostPersona.chatbot.getResponse() } as String
                furhat.say(reply)
                reentry()
            }
            else -> {
                furhat.say("I did not quite catch that. Please let me know when you are done calibrating.")
                reentry()
            }
        }
    }

    onNoResponse {
        delay(4000)
        reentry()
    }
}

// Inter-Phase Transition 1
val PreTask1Transition: State = state() {
    onEntry {
        furhat.say("We are about to begin the first conversational task.")
        delay(1000)
        furhat.say("Please take a moment to relax. Stay completely silent for as long as you need. Just tell me when you are ready to begin.")
        println(">>> ROBOT_LISTENING: PRE_TASK1")
        furhat.listen(timeout = 40000)
    }

    onReentry {
        furhat.say("Take your time. Say ready when you would like to start the first task.")
        println(">>> ROBOT_LISTENING: PRE_TASK1_RETRY")
        furhat.listen(timeout = 40000)
    }

    onResponse {
        val label = call {
            classifyIntent(
                "Are you ready to begin the first task?",
                it.text,
                "- yes\n- no\n- questions"
            )
        } as String

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Nod)
                goto(Task1_StructuredQA)
            }
            label.contains("no") -> {
                furhat.say("No rush whatsoever. I will wait until you are ready.")
                reentry()
            }
            label.contains("questions") -> {
                val reply = call { hostPersona.chatbot.getResponse() } as String
                furhat.say(reply)
                reentry()
            }
            else -> {
                furhat.say("Just let me know when you are ready to start.")
                reentry()
            }
        }
    }

    onNoResponse {
        // Infinite patience buffer loop
        reentry()
    }
}

// Phase 3: Task 1 (Conversation)
val Task1_StructuredQA: State = state() {
    var turnCount = 0

    onEntry {
        furhat.say("Let's begin. I would like to hear your thoughts on a topic.")
        delay(500)
        furhat.say("Can you tell me about your favorite travel destination and what specifically made it so memorable?")
        println(">>> ROBOT_LISTENING: TASK1")
        furhat.listen(timeout = 40000, endSil = 2500)
    }

    onReentry {
        if (turnCount == 0) {
            furhat.say("I am listening. Please tell me about your favorite travel destination.")
            println(">>> ROBOT_LISTENING: TASK1_RETRY")
            furhat.listen(timeout = 40000, endSil = 2500)
        } else {
            furhat.say("I am listening. You can continue speaking.")
            println(">>> ROBOT_LISTENING: TASK1_FOLLOWUP_RETRY")
            furhat.listen(timeout = 40000, endSil = 2500)
        }
    }

    onResponse {
        furhat.gesture(Gestures.Nod)
        
        turnCount++
        if (turnCount < 2) {
            // Placeholder Gemini Fluid Integration
            furhat.say("That sounds absolutely fascinating.")
            delay(500)
            furhat.say("What was the most challenging or unexpected part of that trip?")
            println(">>> ROBOT_LISTENING: TASK1_FOLLOWUP")
            furhat.listen(timeout = 40000, endSil = 2500)
        } else {
            // Completes the 2-minute time loop
            furhat.say("Thank you for sharing that deeply with me. It was very insightful.")
            goto(PreTask2Transition)
        }
    }

    onNoResponse {
        furhat.gesture(Gestures.BrowRaise)
        delay(3000)
        reentry()
    }
}

// Inter-Phase Transition 2
val PreTask2Transition: State = state() {
    onEntry {
        furhat.say("That concludes the speaking task.")
        delay(1000)
        furhat.say("Before we move on to the final listening task, feel free to take another break. Please tell me whenever you are ready to continue.")
        println(">>> ROBOT_LISTENING: PRE_TASK2")
        furhat.listen(timeout = 40000)
    }

    onReentry {
        furhat.say("Simply say ready when you would like me to begin the story.")
        println(">>> ROBOT_LISTENING: PRE_TASK2_RETRY")
        furhat.listen(timeout = 40000)
    }

    onResponse {
        val label = call {
            classifyIntent(
                "Are you ready for the listening task?",
                it.text,
                "- yes\n- no\n- questions"
            )
        } as String

        when {
            label.contains("yes") -> {
                furhat.gesture(Gestures.Smile)
                goto(Task2_Monologue)
            }
            label.contains("no") -> {
                furhat.say("No rush. I am patiently waiting for you.")
                reentry()
            }
            label.contains("questions") -> {
                val reply = call { hostPersona.chatbot.getResponse() } as String
                furhat.say(reply)
                reentry()
            }
            else -> {
                furhat.say("Just let me know whenever you are ready.")
                reentry()
            }
        }
    }

    onNoResponse {
        // Infinite patience buffer loop
        reentry()
    }
}


// Phase 4: Task 2 (Listening)
val Task2_Monologue: State = state() {
    onEntry {
        furhat.say("For the next task, I am going to tell you a short story. Please just listen and look at me as I speak.")
        delay(1000)
        
        furhat.gesture(Gestures.Smile)
        furhat.say("Artificial intelligence has a long and colorful history.")
        delay(1500)
        furhat.say("It began in the nineteen fifties with pioneers who dreamed of creating thinking machines.")
        delay(1500)
        furhat.gesture(Gestures.Nod)
        furhat.say("For many decades, progress was slow and filled with what was called AI winters, where funding and interest completely dried up.")
        delay(1500)
        furhat.say("However, with the recent explosion of deep learning and neural networks, machines can now process natural language, recognize faces, and even converse with people just like I am doing with you right now.")
        delay(1500)
        furhat.gesture(Gestures.BigSmile)
        furhat.say("It is incredible how quickly technology is evolving.")
        delay(1000)
        
        furhat.say("Did you find that little history lesson interesting?")
        println(">>> ROBOT_LISTENING: TASK2")
        furhat.listen(timeout = 20000)
    }

    onResponse {
        furhat.say("I am glad to hear that.")
        goto(Conclusion)
    }

    onNoResponse {
        goto(Conclusion)
    }
}

// Phase 5: Conclusion
val Conclusion: State = state() {
    onEntry {
        furhat.say("This officially concludes our session today.")
        delay(500)
        furhat.gesture(Gestures.Smile)
        furhat.say("Thank you so much for your participation and for helping with the research. You can now take off the Tobii glasses.")
        delay(2000)
        goto(Idle)
    }
}
