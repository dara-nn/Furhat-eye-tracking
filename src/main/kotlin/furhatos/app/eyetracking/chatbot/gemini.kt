package furhatos.app.eyetracking.chatbot

import furhatos.flow.kotlin.DialogHistory
import furhatos.flow.kotlin.Furhat
import java.net.HttpURLConnection
import java.net.URL

val geminiServiceKey: String = run {
    System.getenv("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }
        ?: try {
            val props = java.util.Properties()
            java.io.FileInputStream("local.properties").use { props.load(it) }
            props.getProperty("gemini.api.key", "")
        } catch (e: Exception) { "" }
}

class GeminiAIChatbot(val systemPrompt: String) {

    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"
    private val connectTimeoutMs = 5000
    private val readTimeoutMs = 20000

    fun getResponse(): String {
        repeat(2) { attempt ->
            println(">>> GEMINI_CHAT attempt=${attempt + 1}/2")
            try {

                val textParts = mutableListOf<String>()
                textParts.add(escapeJson(systemPrompt))

                Furhat.dialogHistory.all.takeLast(8).forEach {
                    when (it) {
                        is DialogHistory.ResponseItem -> textParts.add(escapeJson(it.response.text))
                        is DialogHistory.UtteranceItem -> textParts.add(escapeJson(it.toText()))
                    }
                }

                val partsArray = textParts.joinToString(",\n") { text -> "{\"text\": \"${text}\"}" }
                val requestBody = """
                {
                  "contents": [ { "parts": [ $partsArray ] } ],
                  "generationConfig": { "temperature": 0.5, "maxOutputTokens": 1024 }
                }
                """.trimIndent()

                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("x-goog-api-key", geminiServiceKey)
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = connectTimeoutMs
                connection.readTimeout = readTimeoutMs
                connection.doOutput = true

                connection.outputStream.bufferedWriter().use { it.write(requestBody) }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val result = parseGeminiResponse(response)
                    logTokenUsage(response)
                    connection.disconnect()
                    return result
                } else {
                    println(">>> GEMINI_CHAT HTTP ${connection.responseCode}")
                    connection.disconnect()
                    return "Sorry, I zoned out for a second — what were you saying?"
                }
            } catch (e: Exception) {
                println(">>> GEMINI_ERROR attempt=${attempt + 1}: ${e.message}")
            }
        }
        println(">>> GEMINI_CHAT failed after 2 attempts")
        return "I'm so sorry, my mind went blank there — could you say that again?"
    }

    private fun logTokenUsage(jsonResponse: String) {
        try {
            fun extractInt(key: String): Int {
                val idx = jsonResponse.indexOf("\"$key\"")
                if (idx == -1) return -1
                val colon = jsonResponse.indexOf(':', idx)
                val start = colon + 1
                val end = jsonResponse.indexOfAny(charArrayOf(',', '}', '\n'), start)
                if (end == -1) return -1
                return jsonResponse.substring(start, end).trim().toIntOrNull() ?: -1
            }
            val prompt = extractInt("promptTokenCount")
            val candidates = extractInt("candidatesTokenCount")
            val thoughts = extractInt("thoughtsTokenCount")
            val total = extractInt("totalTokenCount")
            println(">>> GEMINI_TOKENS: prompt=$prompt  response=$candidates  thoughts=$thoughts  total=$total")
        } catch (e: Exception) {
            println(">>> GEMINI_TOKENS: (could not parse usage)")
        }
    }

    private fun escapeJson(text: String): String = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "")

    private fun parseGeminiResponse(jsonResponse: String): String {
        return try {
            val candidatesIdx = jsonResponse.indexOf("\"candidates\"")
            if (candidatesIdx == -1) return "Sorry, I lost my train of thought for a second — could you say that again?"
            val textIdx = jsonResponse.indexOf("\"text\"", candidatesIdx)
            if (textIdx == -1) return "Sorry, I lost my train of thought for a second — could you say that again?"
            val colonIdx = jsonResponse.indexOf(':', textIdx + 6)
            val openQuoteIdx = jsonResponse.indexOf('"', colonIdx + 1) + 1
            
            val sb = StringBuilder()
            var pos = openQuoteIdx
            while (pos < jsonResponse.length) {
                val c = jsonResponse[pos]
                if (c == '"') break
                if (c == '\\' && pos + 1 < jsonResponse.length) {
                    when (jsonResponse[pos + 1]) {
                        'n', 'r', 't' -> { sb.append(' '); pos += 2; continue }
                        '"', '\\' -> { sb.append(jsonResponse[pos + 1]); pos += 2; continue }
                        else -> { sb.append(jsonResponse[pos + 1]); pos += 2; continue }
                    }
                }
                sb.append(c)
                pos++
            }
            val text = sb.toString().trim()
            if (text.isNotEmpty()) text else "Hmm, I'm not sure what to say to that — could you tell me more?"
        } catch (e: Exception) {
            "Sorry, I got a bit confused there — what were you saying?"
        }
    }
}

private val YES_KEYWORDS = setOf(
    "yes", "yeah", "yep", "yup", "sure", "ok", "okay", "alright",
    "of course", "absolutely", "definitely", "i consent", "i agree",
    "agreed", "ready", "i'm ready", "im ready"
)
private val NO_KEYWORDS = setOf(
    "no", "nope", "nah", "i don't", "i do not", "i refuse",
    "not really", "no thanks", "no thank you"
)

fun classifyIntent(lastPrompt: String, userSpeech: String, labelsBlock: String): String {
    val normalized = userSpeech.trim().lowercase()
    if (YES_KEYWORDS.any { normalized == it || normalized.startsWith("$it ") || normalized.endsWith(" $it") || normalized.contains(" $it ") }) return "yes"
    if (NO_KEYWORDS.any { normalized == it || normalized.startsWith("$it ") || normalized.endsWith(" $it") || normalized.contains(" $it ") }) return "no"

    val prompt = """
You are classifying what a user said to a conversational robot.
The system just asked: "${lastPrompt.replace("\"", "\\\"")}"
The user responded: "${userSpeech.replace("\"", "\\\"")}"

Classify as exactly ONE of:
$labelsBlock
- unclear

Respond with ONLY the label.
""".trimIndent()

    val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"
    val requestBody = """
    {
      "contents": [{"parts": [{"text": "${prompt.replace("\n", "\\n").replace("\"", "\\\"")}"}]}],
      "generationConfig": {"temperature": 0.0, "maxOutputTokens": 650}
    }
    """.trimIndent()

    repeat(2) { attempt ->
        println(">>> GEMINI_CLASSIFY attempt=${attempt + 1}/2")
        try {
            val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("x-goog-api-key", geminiServiceKey)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.outputStream.bufferedWriter().use { it.write(requestBody) }

            if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                val textIdx = response.indexOf("\"text\"")
                if (textIdx == -1) {
                    println(">>> GEMINI_CLASSIFY no text field in response, returning 'unclear'")
                    return "unclear"
                }
                val colonIdx = response.indexOf(':', textIdx)
                val startQuote = response.indexOf('"', colonIdx + 1)
                val endQuote = response.indexOf('"', startQuote + 1)
                return response.substring(startQuote + 1, endQuote).replace("\\n", "").trim().lowercase()
            } else {
                println(">>> GEMINI_CLASSIFY HTTP ${connection.responseCode}, returning 'unclear'")
                connection.disconnect()
                return "unclear"
            }
        } catch (e: Exception) {
            println(">>> GEMINI_CLASSIFY_ERROR attempt=${attempt + 1}: ${e.message}")
        }
    }
    println(">>> GEMINI_CLASSIFY failed after 2 attempts, returning 'unclear'")
    return "unclear"
}
