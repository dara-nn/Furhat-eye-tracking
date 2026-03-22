package furhatos.app.eyetracking.chatbot

import furhatos.flow.kotlin.DialogHistory
import furhatos.flow.kotlin.Furhat
import java.net.HttpURLConnection
import java.net.URL

val geminiServiceKey: String = "AIzaSyC5RnAkfM38jf0CYiT_h69ag_zwEWN7i7o"

class GeminiAIChatbot(val systemPrompt: String) {

    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent"
    private val connectTimeoutMs = 2000
    private val readTimeoutMs = 7000

    fun getResponse(): String {
        return try {
            val textParts = mutableListOf<String>()
            textParts.add(escapeJson(systemPrompt))
            
            Furhat.dialogHistory.all.takeLast(4).forEach {
                when (it) {
                    is DialogHistory.ResponseItem -> textParts.add(escapeJson(it.response.text))
                    is DialogHistory.UtteranceItem -> textParts.add(escapeJson(it.toText()))
                }
            }
            
            val partsArray = textParts.joinToString(",\n") { text -> "{\"text\": \"${text}\"}" }
            val requestBody = """
            {
              "contents": [ { "parts": [ $partsArray ] } ],
              "generationConfig": { "temperature": 0.5, "maxOutputTokens": 1024, "thinkingConfig": { "thinkingBudget": 600 } }
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
                connection.disconnect()
                "I'm sorry, my language module is currently unreachable."
            }
        } catch (e: Exception) {
            "I encountered an error processing your request."
        }
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
            if (candidatesIdx == -1) return "I apologize."
            val textIdx = jsonResponse.indexOf("\"text\"", candidatesIdx)
            if (textIdx == -1) return "I apologize."
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
            if (text.isNotEmpty()) text else "I couldn't come up with anything to say."
        } catch (e: Exception) {
            "I apologize, but I couldn't generate a response."
        }
    }
}

fun classifyIntent(lastPrompt: String, userSpeech: String, labelsBlock: String): String {
    val prompt = """
You are classifying what a user said to a conversational robot.
The system just asked: "${lastPrompt.replace("\"", "\\\"")}"
The user responded: "${userSpeech.replace("\"", "\\\"")}"

Classify as exactly ONE of:
$labelsBlock
- unclear

Respond with ONLY the label.
""".trimIndent()

    val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent"
    return try {
        val requestBody = """
        {
          "contents": [{"parts": [{"text": "${prompt.replace("\n", "\\n").replace("\"", "\\\"")}"}]}],
          "generationConfig": {"temperature": 0.0, "maxOutputTokens": 650, "thinkingConfig": {"thinkingBudget": 450}}
        }
        """.trimIndent()
        val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("x-goog-api-key", geminiServiceKey)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 2000
        connection.readTimeout = 5000
        connection.doOutput = true
        connection.outputStream.bufferedWriter().use { it.write(requestBody) }
        
        if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            val textIdx = response.indexOf("\"text\"")
            if (textIdx == -1) return "unclear"
            val colonIdx = response.indexOf(':', textIdx)
            val startQuote = response.indexOf('"', colonIdx + 1)
            val endQuote = response.indexOf('"', startQuote + 1)
            response.substring(startQuote + 1, endQuote).replace("\\n", "").trim().lowercase()
        } else {
            connection.disconnect()
            "unclear"
        }
    } catch (e: Exception) {
        "unclear"
    }
}
