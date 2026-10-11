package com.shriyaanai.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val baseUrl =
        "https://shriyaan-ai-api.tt6914794.workers.dev"

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout

    private val prefs by lazy {
        getSharedPreferences("shriyaan_chat", Context.MODE_PRIVATE)
    }

    private val chatHistory = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(18, 24, 45)
        window.navigationBarColor = Color.rgb(18, 24, 45)

        createBaseLayout()
        showChatScreen()
    }

    // --------------------------------------------------
    // BASE LAYOUT
    // --------------------------------------------------

    private fun createBaseLayout() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 247, 252))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 12)
            setBackgroundColor(Color.rgb(18, 24, 45))
        }

        val title = TextView(this).apply {
            text = "SHRIYAAN AI"
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        val subtitle = TextView(this).apply {
            text = "Your AI assistant"
            textSize = 13f
            setTextColor(Color.LTGRAY)
        }

        header.addView(title)
        header.addView(subtitle)

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 8, 10, 8)
            setBackgroundColor(Color.rgb(30, 38, 65))
        }

        val chatTab = makeButton("AI Chat") {
            showChatScreen()
        }

        val studioTab = makeButton("Auto Studio") {
            showStudioScreen()
        }

        navigation.addView(
            chatTab,
            LinearLayout.LayoutParams(0, 48, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
        )

        navigation.addView(
            studioTab,
            LinearLayout.LayoutParams(0, 48, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
        )

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        val contentScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(
                content,
                ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        root.addView(header)

        root.addView(navigation)

        root.addView(
            contentScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            setOnClickListener {
                action()
            }
        }
    }

    private fun addHeading(text: String) {
        val heading = TextView(this).apply {
            this.text = text
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(25, 35, 65))
            setPadding(4, 8, 4, 12)
        }

        content.addView(heading)
    }

    private fun addMessage(
        label: String,
        message: String,
        isUser: Boolean
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)

            setBackgroundColor(
                if (isUser) {
                    Color.rgb(224, 235, 255)
                } else {
                    Color.WHITE
                }
            )
        }

        val name = TextView(this).apply {
            text = label
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(50, 65, 105))
        }

        val body = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(Color.rgb(30, 35, 45))
            setPadding(0, 6, 0, 0)
        }

        card.addView(name)
        card.addView(body)

        content.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 5, 0, 9)
            }
        )
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --------------------------------------------------
    // AI CHAT
    // --------------------------------------------------

    private fun showChatScreen() {
        content.removeAllViews()

        addHeading("AI Chat")

        val info = TextView(this).apply {
            text = "Shriyaan AI से सवाल पूछें।"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(4, 0, 4, 12)
        }

        content.addView(info)

        val chatArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val historyScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(chatArea)
        }

        content.addView(
            historyScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val input = EditText(this).apply {
            hint = "अपना सवाल यहाँ लिखें..."
            textSize = 16f
            minLines = 2
            maxLines = 5
            setPadding(12, 10, 12, 10)
            setBackgroundColor(Color.WHITE)
        }

        content.addView(
            input,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 6)
            }
        )

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val newChat = makeButton("New Chat") {
            chatHistory.clear()
            prefs.edit().remove("history").apply()
            chatArea.removeAllViews()
            input.setText("")
            showToast("नई चैट शुरू हो गई।")
        }

        val send = makeButton("Send ➤") {
            // Button click action is set below.
        }

        send.setOnClickListener {
            val message = input.text.toString().trim()

            if (message.isEmpty()) {
                showToast("पहले अपना सवाल लिखें।")
            } else {
                sendChatMessage(
                    message,
                    input,
                    send,
                    chatArea,
                    historyScroll
                )
            }
        }

        actions.addView(
            newChat,
            LinearLayout.LayoutParams(0, 50, 1f)
        )

        actions.addView(
            send,
            LinearLayout.LayoutParams(0, 50, 1f)
        )

        content.addView(actions)

        loadSavedHistory(chatArea)
    }

    private fun sendChatMessage(
        message: String,
        input: EditText,
        sendButton: Button,
        chatArea: LinearLayout,
        historyScroll: ScrollView
    ) {
        input.setText("")
        input.isEnabled = false
        sendButton.isEnabled = false

        hideKeyboard(input)

        val userCard = createChatCard(
            "आप",
            message,
            true
        )

        chatArea.addView(userCard)

        val loading = createChatCard(
            "Shriyaan AI",
            "जवाब तैयार हो रहा है...",
            false
        )

        chatArea.addView(loading)

        historyScroll.post {
            historyScroll.fullScroll(View.FOCUS_DOWN)
        }

        thread {
            try {
                val body = JSONObject().apply {
                    put("message", message)
                }

                val response = request(
                    "/chat",
                    "POST",
                    body
                )

                val answer = extractChatAnswer(response)

                runOnUiThread {
                    chatArea.removeView(loading)

                    chatArea.addView(
                        createChatCard(
                            "Shriyaan AI",
                            answer,
                            false
                        )
                    )

                    chatHistory.add(
                        Pair(message, answer)
                    )

                    saveHistory()

                    input.isEnabled = true
                    sendButton.isEnabled = true

                    historyScroll.post {
                        historyScroll.fullScroll(
                            View.FOCUS_DOWN
                        )
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    chatArea.removeView(loading)

                    chatArea.addView(
                        createChatCard(
                            "Error",
                            errorMessage(e),
                            false
                        )
                    )

                    input.isEnabled = true
                    sendButton.isEnabled = true

                    historyScroll.post {
                        historyScroll.fullScroll(
                            View.FOCUS_DOWN
                        )
                    }
                }
            }
        }
    }

    private fun createChatCard(
        label: String,
        message: String,
        isUser: Boolean
    ): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)

            setBackgroundColor(
                if (isUser) {
                    Color.rgb(224, 235, 255)
                } else {
                    Color.WHITE
                }
            )
        }

        val title = TextView(this).apply {
            text = label
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(45, 60, 105))
        }

        val body = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(Color.rgb(30, 35, 45))
            setPadding(0, 5, 0, 0)
        }

        card.addView(title)
        card.addView(body)

        card.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 4, 0, 8)
        }

        return card
    }

    private fun extractChatAnswer(
        response: JSONObject
    ): String {
        val candidates = response.optJSONArray("candidates")

        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.optJSONObject(0)
            val contentObject = candidate?.optJSONObject("content")
            val parts = contentObject?.optJSONArray("parts")

            if (parts != null) {
                val result = StringBuilder()

                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val text = part?.optString("text", "") ?: ""

                    if (text.isNotBlank()) {
                        if (result.isNotEmpty()) {
                            result.append("\n")
                        }
                        result.append(text)
                    }
                }

                if (result.isNotEmpty()) {
                    return result.toString()
                }
            }
        }

        val possibleKeys = listOf(
            "reply",
            "response",
            "answer",
            "text",
            "message"
        )

        for (key in possibleKeys) {
            val value = response.optString(key, "")
            if (value.isNotBlank() && value != "null") {
                return value
            }
        }

        return response.toString(2)
    }

    private fun loadSavedHistory(
        chatArea: LinearLayout
    ) {
        val saved = prefs.getString("history", null)
            ?: return

        try {
            val array = JSONArray(saved)

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue

                val question = item.optString("question", "")
                val answer = item.optString("answer", "")

                chatArea.addView(
                    createChatCard(
                        "आप",
                        question,
                        true
                    )
                )

                chatArea.addView(
                    createChatCard(
                        "Shriyaan AI",
                        answer,
                        false
                    )
                )
            }
        } catch (_: Exception) {
            prefs.edit().remove("history").apply()
        }
    }

    private fun saveHistory() {
        val array = JSONArray()

        for (item in chatHistory) {
            array.put(
                JSONObject().apply {
                    put("question", item.first)
                    put("answer", item.second)
                }
            )
        }

        prefs.edit()
            .putString("history", array.toString())
            .apply()
    }

    private fun hideKeyboard(view: View) {
        try {
            val manager = getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

            manager.hideSoftInputFromWindow(
                view.windowToken,
                0
            )
        } catch (_: Exception) {
        }
    }

    // --------------------------------------------------
    // AUTO STUDIO
    // --------------------------------------------------

    private fun showStudioScreen() {
        content.removeAllViews()

        addHeading("Auto Studio")

        val description = TextView(this).apply {
            text = "AI वीडियो स्क्रिप्ट तैयार करें और अपनी Queue देखें।"
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }

        content.addView(description)

        val generateButton = makeButton(
            "Generate New Script"
        ) {
            generateScript()
        }

        content.addView(
            generateButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                52
            ).apply {
                setMargins(0, 12, 0, 8)
            }
        )

        val refreshButton = makeButton(
            "Refresh Queue"
        ) {
            loadQueue()
        }

        content.addView(
            refreshButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        )

        val statusText = TextView(this).apply {
            text = "Studio status लोड हो रहा है..."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(4, 4, 4, 10)
        }

        content.addView(statusText)

        val queueHeading = TextView(this).apply {
            text = "Script Queue"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(25, 35, 65))
            setPadding(4, 12, 4, 8)
        }

        content.addView(queueHeading)

        val queueContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(queueContainer)

        loadStudioStatus(statusText)
        loadQueueInto(queueContainer)
    }

    private fun generateScript() {
        AlertDialog.Builder(this)
            .setTitle("Generate Script")
            .setMessage(
                "क्या आप Auto Studio के मौजूदा सेटिंग्स के अनुसार नई स्क्रिप्ट बनाना चाहते हैं?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Generate") { _, _ ->
                runGenerateRequest()
            }
            .show()
    }

    private fun runGenerateRequest() {
        showToast("स्क्रिप्ट बनाई जा रही है...")

        thread {
            try {
                val response = request(
                    "/studio/generate",
                    "POST",
                    JSONObject()
                )

                runOnUiThread {
                    showResponseDialog(
                        "Generate Result",
                        response
                    )

                    showStudioScreen()
                }

            } catch (e: Exception) {
                showToast(errorMessage(e))
            }
        }
    }

    private fun loadStudioStatus(
        statusText: TextView
    ) {
        thread {
            try {
                val response = request(
                    "/studio/status",
                    "GET"
                )

                runOnUiThread {
                    val settings = response.optJSONObject("settings")
                    val topic = settings?.optString(
                        "topic",
                        "Not set"
                    ) ?: "Not set"

                    val language = settings?.optString(
                        "language",
                        "Hindi"
                    ) ?: "Hindi"

                    val platform = settings?.optString(
                        "platform",
                        "YouTube Shorts"
                    ) ?: "YouTube Shorts"

                    val videos = settings?.optInt(
                        "videosPerDay",
                        1
                    ) ?: 1

                    val duration = settings?.optInt(
                        "durationSeconds",
                        45
                    ) ?: 45

                    statusText.text =
                        "Topic: $topic\n" +
                        "Language: $language\n" +
                        "Platform: $platform\n" +
                        "Videos per day: $videos\n" +
                        "Duration: $duration seconds\n" +
                        "Status: ${response.optString("status", "Available")}"
