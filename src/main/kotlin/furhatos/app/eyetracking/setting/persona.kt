package furhatos.app.eyetracking.setting

import furhatos.app.eyetracking.chatbot.GeminiAIChatbot
import furhatos.flow.kotlin.FlowControlRunner
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.voice.ElevenlabsVoice
import furhatos.flow.kotlin.voice.Voice
import furhatos.util.Gender
import furhatos.util.Language

class Persona(
    val name: String,
    val desc: String,
    val face: List<String>,
    val mask: String = "adult",
    val voice: Voice,
    val systemPrompt: String = ""
) {
    val chatbot = GeminiAIChatbot(
        if (systemPrompt.isNotEmpty()) systemPrompt
        else "You are ${name}, the ${desc}. You are a helpful and polite virtual assistant guiding a user through an eye-tracking experiment. Keep your responses short, maximum two sentences."
    )
}

fun FlowControlRunner.activate(persona: Persona) {
    furhat.mask = persona.mask
    val maskDelay = if (persona.mask == "child") 900L else 300L
    delay(maskDelay)

    furhat.voice = persona.voice
    
    val preferredMaskFaces = furhat.faces[persona.mask] ?: emptyList()
    val selectedFace = persona.face.firstOrNull { it in preferredMaskFaces } ?: preferredMaskFaces.firstOrNull()

    if (selectedFace != null) {
        furhat.character = selectedFace
    }
}

val hostPersona = Persona(
    name = "Assistant",
    desc = "experiment host",
    face = listOf("Assistant", "Host", "White teen girl", "White teen boy"),
    mask = "adult",
    voice = ElevenlabsVoice("Hope-Assistant", Gender.FEMALE, Language.MULTILINGUAL),
    systemPrompt = "You are an Assistant guiding the user through an eye-tracking experiment. You are polite, clear, and very helpful. Keep your answers concise, maximum 1-2 sentences."
)
