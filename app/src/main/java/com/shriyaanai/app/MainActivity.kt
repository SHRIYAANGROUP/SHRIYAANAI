package com.shriyaanai.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var chatBox: TextView
    private lateinit var input: EditText
    private lateinit var sendButton: Button

    private val apiUrl =
        "https://shriyaan-ai-api.tt6914794.workers.dev/chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 15, 30)
        window.navigationBarColor = Color.rgb(15, 15, 30)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 16)
            setBackgroundColor(Color.rgb(15, 15, 30))
        }

        val title = TextView(this).apply {
            text = "✨ Shriyaan AI"
            textSize = 27f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 16, 8, 16)
        }

        val subtitle = TextView(this).apply {
            text = "Your personal AI assistant"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 16)
        }

        val scrollView = ScrollView(this)

        chatBox = TextView(this).apply {
            text = "Shriyaan AI में आपका स्वागत है! 👋\n\nअपना सवाल नीचे लिखें।"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
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

        root.addView(title)
        root.addView(subtitle)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(
            input,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
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
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()

        if (message.isEmpty()) {
            Toast.makeText(
                this,
                "पहले अपना सवाल लिखें",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        input.text.clear()
        chatBox.append("\n\n🧑 आप: $message\n\n🤖 Shriyaan AI: सोच रहा हूँ...")
        sendButton.isEnabled = false

        thread {
            var reply = ""

            try {
                val connection =
                    URL(apiUrl).openConnection() as HttpURLConnection

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

                connection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val status = connection.responseCode

                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream.bufferedReader().use {
                    it.readText()
                }

                val json = JSONObject(response)

                reply = if (status in 200..299) {
                    json.optString(
                        "reply",
                        "AI से जवाब नहीं मिला।"
                    )
                } else {
                    json.optString(
                        "error",
                        "सर्वर में समस्या है। दोबारा कोशिश करें।"
                    )
                }

                connection.disconnect()

            } catch (e: Exception) {
                reply = "कनेक्शन में समस्या है। इंटरनेट और API जाँचें।"
            }

            runOnUiThread {
                chatBox.text = chatBox.text.toString()
                    .replace("🤖 Shriyaan AI: सोच रहा हूँ...", "")
                    .plus("\n\n🤖 Shriyaan AI: $reply")

                sendButton.isEnabled = true
            }
        }
    }
}
