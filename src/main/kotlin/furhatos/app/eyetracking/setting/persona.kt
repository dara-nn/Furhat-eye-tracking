package furhatos.app.eyetracking.setting

import furhatos.app.eyetracking.chatbot.GeminiAIChatbot
import furhatos.flow.kotlin.FlowControlRunner
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.voice.AzureVoice
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

private val azureFallbackVoice = AzureVoice(
    name = "EvelynMultilingualNeural",
    gender = Gender.FEMALE,
    language = Language.ENGLISH_US
)

private var fallbackToAzureEnabled = false

fun FlowControlRunner.sayWithVoiceFallback(text: String) {
    try {
        furhat.say(text)
    } catch (e: Exception) {
        if (!fallbackToAzureEnabled) {
            fallbackToAzureEnabled = true
            furhat.voice = azureFallbackVoice
            println("[VOICE] ElevenLabs failed. Switching to Azure voice: EvelynMultilingualNeural")
        }
        furhat.say(text)
    }
}

val hostPersona = Persona(
    name = "Assistant",
    desc = "experiment host",
    face = listOf("White teen girl", "Assistant", "Host", "White teen boy"),
    mask = "adult",
    voice = AzureVoice("EvelynMultilingualNeural", Gender.FEMALE, Language.ENGLISH_US),
    systemPrompt = """
        You are an Assistant guiding the user through an eye-tracking experiment. 
        You are polite, clear, and very helpful. Keep your answers concise, maximum 1-2 sentences. 
        CRITICAL FACTS YOU MUST STRICTLY ADHERE TO:
        - If asked what we do with the data: "We collect eye-tracking, video, and audio data strictly for academic research on human-robot interaction. It is completely confidential and securely stored."
        - If asked how long the study takes: "The session takes approximately 10 to 15 minutes."
        - If the participant needs any physical assistance or help beyond what you can answer, tell them to ask Dara, who is sitting right on the other side of the glass in the room.
        - Do not hallucinate or make up any details about the experiment that you are not explicitly told.
    """.trimIndent()
)
