package furhatos.app.eyetracking.flow

import furhatos.app.eyetracking.setting.activate
import furhatos.app.eyetracking.setting.hostPersona
import furhatos.app.eyetracking.chatbot.geminiServiceKey
import furhatos.event.requests.RequestConfigElevenlabs
import furhatos.event.responses.ResponseConfigElevenlabs
import furhatos.flow.kotlin.*

val elevenLabsApiKey: String = "sk_9b839c21f8506ffcd4cdb38c2724a36ca5fc70dd127a8ef0"

val Init: State = state() {
    onEvent<RequestConfigElevenlabs>(instant = true) {
        send(ResponseConfigElevenlabs.Builder().apiKey(elevenLabsApiKey).buildEvent())
    }

    init {
        if (geminiServiceKey.isEmpty()) {
            println("Missing API key for Gemini language model.")
            exit()
        }
        activate(hostPersona)
        goto(BriefingAndConsent)
    }
}
