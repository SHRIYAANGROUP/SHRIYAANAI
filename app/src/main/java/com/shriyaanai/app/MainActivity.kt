package com.shriyaanai.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var chatBox: TextView
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private lateinit var scrollView: ScrollView
    private lateinit var newChatButton: Button

    private val apiUrl =
        "https://shriyaan-ai-api.tt6914794.workers.dev/chat"

    private val preferences by lazy {
        getSharedPreferences("shriyaan_chat_history", MODE_PRIVATE)
    }

    private data class ChatMessage(
        val role: String,
        val text: String
    )

    private val messages = mutableListOf<ChatMessage>()

    private var isWaiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 15, 30)
        window.navigationBarColor = Color.rgb(15, 15, 30)

        loadHistory()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(Color.rgb(15, 15, 30))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "✨ Shriyaan AI"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 12, 4, 12)
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        newChatButton = Button(this).apply {
            text = "＋ नई चैट"
            textSize = 12f
            setOnClickListener {
                startNewChat()
            }
        }

        header.addView(
            newChatButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = TextView(this).apply {
            text = "Your personal AI assistant"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 12)
        }

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(22, 22, 40))
        }

        chatBox = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(14, 14, 14, 14)
            setTextIsSelectable(true)
        }

        scrollView.addView(chatBox)

        input = EditText(this).apply {
            hint = "यहाँ अपना सवाल लिखें..."
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(14, 10, 14, 10)
            setBackgroundColor(Color.rgb(40, 40, 60))
            minLines = 1
            maxLines = 4
        }

        sendButton = Button(this).apply {
            text = "Send ➤"
            textSize = 16f
            setOnClickListener {
                sendMessage()
            }
        }

        root.addView(header)
        root.addView(subtitle)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                bottomMargin = 10
            }
        )

        root.addView(
            input,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 6
            }
        )

        root.addView(
            sendButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)

        renderChat()
    }

    private fun loadHistory() {
        messages.clear()

        val saved = preferences.getString("messages", "[]") ?: "[]"

        try {
            val array = JSONArray(saved)

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                messages.add(
                    ChatMessage(
                        role = item.optString("role", "user"),
                        text = item.optString("text", "")
                    )
                )
            }
        } catch (e: Exception) {
            messages.clear()
        }
    }

    private fun saveHistory() {
        val array = JSONArray()

        messages.forEach { message ->
            array.put(
                JSONObject()
                    .put("role", message.role)
                    .put("text", message.text)
            )
        }

        preferences.edit()
            .putString("messages", array.toString())
            .apply()
    }

    private fun renderChat() {
        val display = StringBuilder()

        if (messages.isEmpty()) {
            display.append(
                "Shriyaan AI में आपका स्वागत है! 👋\n\n" +
                "अपना सवाल नीचे लिखें।"
            )
        } else {
            messages.forEach { message ->
                when (message.role) {
                    "user" -> {
                        display.append("🧑 आप:\n")
                        display.append(message.text)
                    }

                    "assistant" -> {
                        display.append("🤖 Shriyaan AI:\n")
                        display.append(message.text)
                    }
                }

                display.append("\n\n")
            }
        }

        if (isWaiting) {
            display.append("🤖 Shriyaan AI: सोच रहा हूँ...")
        }

        chatBox.text = display.toString()

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun startNewChat() {
        if (isWaiting) {
            Toast.makeText(
                this,
                "पहले मौजूदा जवाब आने दें।",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (messages.isEmpty()) {
            Toast.makeText(
                this,
                "यह पहले से नई चैट है।",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        messages.clear()
        saveHistory()
        renderChat()

        Toast.makeText(
            this,
            "नई चैट शुरू हो गई।",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()

        if (message.isEmpty()) {
            Toast.makeText(
                this,
                "पहले अपना सवाल लिखें।",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isWaiting) {
            return
        }

        messages.add(ChatMessage("user", message))
        saveHistory()

        input.text.clear()

        isWaiting = true
        sendButton.isEnabled = false
        newChatButton.isEnabled = false

        renderChat()

        thread {
            val reply: String = try {
                val connection =
                    URL(apiUrl).openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 20000
                    connection.readTimeout = 60000
                    connection.doOutput = true

                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                    )

                    val body = JSONObject()
                        .put("message", message)
                        .toString()

                    connection.outputStream.use { output ->
                        output.write(body.toByteArray(Charsets.UTF_8))
                    }

                    val status = connection.responseCode

                    val stream = if (status in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                    val response = stream?.bufferedReader()?.use {
                        it.readText()
                    } ?: ""

                    val json = JSONObject(
                        response.ifBlank { "{}" }
                    )

                    if (status in 200..299) {
                        json.optString(
                            "reply",
                            "AI से जवाब नहीं मिला।"
                        )
                    } else {
                        val details = json.optString("details", "")

                        when {
                            details.contains("location", ignoreCase = true) ->
                                "Google AI की क्षेत्रीय उपलब्धता में समस्या है। कृपया बाद में कोशिश करें।"

                            status == 503 ->
                                "AI सर्वर अभी व्यस्त है। कुछ देर बाद दोबारा कोशिश करें।"

                            status == 429 ->
                                "AI की अनुरोध सीमा पूरी हो गई है। कुछ देर बाद कोशिश करें।"

                            details.isNotBlank() ->
                                "AI सेवा में समस्या है: $details"

                            else ->
                                json.optString(
                                    "error",
                                    "सर्वर में समस्या है। दोबारा कोशिश करें।"
                                )
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                "कनेक्शन में समस्या है। इंटरनेट और API जाँचें।"
            }

            runOnUiThread {
                messages.add(ChatMessage("assistant", reply))

                isWaiting = false
                sendButton.isEnabled = true
                newChatButton.isEnabled = true

                saveHistory()
                renderChat()
            }
        }
    }
}
