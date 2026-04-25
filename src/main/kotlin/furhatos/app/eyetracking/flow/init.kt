package furhatos.app.eyetracking.flow

import furhatos.app.eyetracking.setting.activate
import furhatos.app.eyetracking.setting.hostPersona
import furhatos.app.eyetracking.chatbot.geminiServiceKey
import furhatos.asr.ASRProvider
import furhatos.flow.kotlin.*

val Init: State = state() {
    init {
        if (geminiServiceKey.isEmpty()) {
            println("Missing API key for Gemini language model.")
            exit()
        }
        furhat.setRecognizer(ASRProvider.GOOGLE)
        println(">>> ASR: Switched to Google Speech Recognition")
        activate(hostPersona)
        goto(Idle)
    }
}
