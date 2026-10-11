package com.shriyaanai.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
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

    // BASE LAYOUT

    private fun createBaseLayout() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 247, 252))
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
                LinearLayout.LayoutParams(
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

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // AI CHAT SCREEN

    private fun showChatScreen() {
        content.removeAllViews()
        addHeading("AI Chat")

        content.addView(
            TextView(this).apply {
                text = "Shriyaan AI से सवाल पूछें।"
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(4, 0, 4, 12)
            }
        )

        val chatArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val historyScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(
                chatArea,
                ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
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
    }// SEND CHAT MESSAGE

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

    // CHAT MESSAGE CARD

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

    // EXTRACT AI RESPONSE

    private fun extractChatAnswer(
        response: JSONObject
    ): String {
        val candidates = response.optJSONArray("candidates")

        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.optJSONObject(0)
            val contentObject =
                candidate?.optJSONObject("content")

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

    // SAVE AND RESTORE CHAT HISTORY

    private fun loadSavedHistory(
        chatArea: LinearLayout
    ) {
        val saved = prefs.getString("history", null)
            ?: return

        try {
            val array = JSONArray(saved)

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i)
                    ?: continue

                val question = item.optString("question", "")
                val answer = item.optString("answer", "")

                chatHistory.add(
                    Pair(question, answer)
                )

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

    // HIDE KEYBOARD

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
    }// =========================
    // AUTO STUDIO SCREEN
    // =========================

    private fun showStudioScreen() {
        createBaseLayout()

        addHeading("🎬 Shriyaan AI Studio")
        addText(
            "राजस्थान के इतिहास और रोचक तथ्यों पर वीडियो स्क्रिप्ट तैयार करें।",
            14f
        )

        val generateButton = makeButton("✨ नई स्क्रिप्ट बनाएँ") {
            generateScript()
        }
        rootLayout.addView(generateButton)

        val statusButton = makeButton("🔄 Studio Status") {
            loadStudioStatus()
        }
        rootLayout.addView(statusButton)

        val queueButton = makeButton("📋 वीडियो Queue देखें") {
            loadQueue()
        }
        rootLayout.addView(queueButton)

        val chatButton = makeButton("💬 AI Chat पर जाएँ") {
            showChatScreen()
        }
        rootLayout.addView(chatButton)

        addHeading("📚 तैयार स्क्रिप्ट / Queue")

        val queueScroll = ScrollView(this)
        queueScroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        val queueContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 16)
        }

        queueScroll.addView(queueContainer)
        rootLayout.addView(queueScroll)

        loadQueueInto(queueContainer)
    }

    private fun addText(text: String, size: Float = 16f) {
        val label = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.DKGRAY)
            setPadding(8, 8, 8, 8)
        }
        rootLayout.addView(label)
    }

    // =========================
    // GENERATE SCRIPT
    // =========================

    private fun generateScript() {
        AlertDialog.Builder(this)
            .setTitle("नई वीडियो स्क्रिप्ट")
            .setMessage(
                "क्या आप राजस्थान के इतिहास और रोचक तथ्यों पर नई स्क्रिप्ट बनाना चाहते हैं?"
            )
            .setNegativeButton("रद्द करें", null)
            .setPositiveButton("बनाएँ") { _, _ ->
                runGenerateRequest()
            }
            .show()
    }

    private fun runGenerateRequest() {
        showToast("स्क्रिप्ट तैयार की जा रही है...")

        thread {
            try {
                val body = JSONObject().apply {
                    put("topic", "राजस्थान का इतिहास और रोचक तथ्य")
                    put("language", "Hindi")
                    put("platform", "YouTube Shorts")
                    put("durationSeconds", 45)
                }

                val result = request(
                    "/studio/generate",
                    "POST",
                    body
                )

                runOnUiThread {
                    showResponseDialog(
                        "स्क्रिप्ट जनरेशन का परिणाम",
                        result.toString(2)
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast("स्क्रिप्ट नहीं बनी: ${e.message}")
                }
            }
        }
    }

    // =========================
    // STUDIO STATUS
    // =========================

    private fun loadStudioStatus() {
        showToast("Studio Status लोड हो रहा है...")

        thread {
            try {
                val result = request("/studio/status", "GET")

                runOnUiThread {
                    showResponseDialog(
                        "Studio Status",
                        result.toString(2)
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast("Status नहीं मिला: ${e.message}")
                }
            }
        }
    }

    // =========================
    // QUEUE
    // =========================

    private fun loadQueue() {
        showToast("Queue लोड हो रही है...")

        thread {
            try {
                val result = request("/studio/queue", "GET")

                runOnUiThread {
                    showResponseDialog(
                        "Video Queue",
                        result.toString(2)
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast("Queue नहीं खुली: ${e.message}")
                }
            }
        }
    }

    private fun loadQueueInto(container: LinearLayout) {
        container.removeAllViews()

        val loading = TextView(this).apply {
            text = "Queue लोड हो रही है..."
            textSize = 15f
            setPadding(8, 12, 8, 12)
        }
        container.addView(loading)

        thread {
            try {
                val result = request("/studio/queue", "GET")
                val items = result.optJSONArray("items") ?: JSONArray()

                runOnUiThread {
                    container.removeAllViews()

                    if (items.length() == 0) {
                        val empty = TextView(this).apply {
                            text = "अभी Queue खाली है। नई स्क्रिप्ट बनाने के लिए ऊपर बटन दबाएँ।"
                            textSize = 15f
                            setPadding(8, 12, 8, 12)
                        }
                        container.addView(empty)
                        return@runOnUiThread
                    }

                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        addQueueCard(container, item)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    container.removeAllViews()

                    val error = TextView(this).apply {
                        text = "Queue लोड नहीं हुई: ${e.message}"
                        textSize = 14f
                        setPadding(8, 12, 8, 12)
                    }
                    container.addView(error)
                }
            }
        }
    }

    private fun addQueueCard(
        container: LinearLayout,
        item: JSONObject
    ) {
        val title = item.optString(
            "title",
            item.optString("topic", "बिना शीर्षक की स्क्रिप्ट")
        )

        val id = item.optString(
            "id",
            item.optString("scriptId", "")
        )

        val status = item.optString("status", "अज्ञात")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(Color.rgb(245, 245, 245))
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }
        card.addView(titleView)

        val statusView = TextView(this).apply {
            text = "स्थिति: $status"
            textSize = 14f
            setPadding(0, 6, 0, 6)
        }
        card.addView(statusView)

        val openButton = makeButton("स्क्रिप्ट विवरण देखें") {
            showScriptDetails(item)
        }
        card.addView(openButton)

        val actionsButton = makeButton("Review / Approve / Revise") {
            if (id.isBlank()) {
                showToast("इस स्क्रिप्ट की ID उपलब्ध नहीं है।")
            } else {
                showScriptActions(item)
            }
        }
        card.addView(actionsButton)

        val divider = View(this).apply {
            setBackgroundColor(Color.LTGRAY)
        }
        val dividerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        )
        dividerParams.setMargins(0, 12, 0, 12)
        card.addView(divider, dividerParams)

        container.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun statusHindi(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "लंबित"
            "approved" -> "स्वीकृत"
            "rejected" -> "अस्वीकृत"
            "reviewed" -> "जाँची गई"
            "revision_requested" -> "सुधार माँगा गया"
            "processing" -> "तैयार हो रही है"
            "completed" -> "पूरी हुई"
            else -> status
        }
    }

    // =========================
    // SCRIPT DETAILS AND ACTIONS
    // =========================

    private fun showScriptDetails(item: JSONObject) {
        val details = buildString {
            appendLine("शीर्षक:")
            appendLine(
                item.optString("title", item.optString("topic", "उपलब्ध नहीं"))
            )
            appendLine()

            appendLine("स्थिति:")
            appendLine(statusHindi(item.optString("status", "अज्ञात")))
            appendLine()

            appendLine("स्क्रिप्ट:")
            appendLine(
                item.optString(
                    "script",
                    item.optString(
                        "content",
                        item.optString("text", "स्क्रिप्ट का टेक्स्ट उपलब्ध नहीं है।")
                    )
                )
            )
        }

        showResponseDialog("स्क्रिप्ट विवरण", details)
    }

    private fun showScriptActions(item: JSONObject) {
        val choices = arrayOf(
            "स्क्रिप्ट Review करें",
            "Approve करें",
            "सुधार के लिए भेजें",
            "Reject करें"
        )

        AlertDialog.Builder(this)
            .setTitle("स्क्रिप्ट के विकल्प")
            .setItems(choices) { _, which ->
                when (which) {
                    0 -> reviewScript(item)
                    1 -> reviewScriptWithAction(item, "approve", "")
                    2 -> showRevisionDialog(item)
                    3 -> confirmReject(item)
                }
            }
            .setNegativeButton("बंद करें", null)
            .show()
    }

    private fun reviewScript(item: JSONObject) {
        reviewScriptWithAction(item, "review", "")
    }

    private fun showRevisionDialog(item: JSONObject) {
        val input = EditText(this).apply {
            hint = "बताएँ कि स्क्रिप्ट में क्या सुधार चाहिए"
            minLines = 3
            setPadding(24, 16, 24, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("स्क्रिप्ट में सुधार")
            .setMessage("उदाहरण: शुरुआत आकर्षक बनाएँ, तथ्य जाँचें या भाषा सरल करें।")
            .setView(input)
            .setNegativeButton("रद्द करें", null)
            .setPositiveButton("सुधार भेजें") { _, _ ->
                val instructions = input.text.toString().trim()

                if (instructions.isBlank()) {
                    showToast("पहले सुधार का निर्देश लिखें।")
                } else {
                    reviewScriptWithAction(
                        item,
                        "revise",
                        instructions
                    )
                }
            }
            .show()
    }

    private fun confirmReject(item: JSONObject) {
        AlertDialog.Builder(this)
            .setTitle("स्क्रिप्ट Reject करें?")
            .setMessage("क्या आप वाकई इस स्क्रिप्ट को अस्वीकार करना चाहते हैं?")
            .setNegativeButton("नहीं", null)
            .setPositiveButton("हाँ, Reject करें") { _, _ ->
                reviewScriptWithAction(item, "reject", "")
            }
            .show()
    }

    private fun reviewScriptWithAction(
        item: JSONObject,
        action: String,
        instructions: String
    ) {
        val id = item.optString(
            "id",
            item.optString("scriptId", "")
        )

        if (id.isBlank()) {
            showToast("स्क्रिप्ट ID उपलब्ध नहीं है।")
            return
        }

        showToast("अनुरोध भेजा जा रहा है...")

        thread {
            try {
                val body = JSONObject().apply {
                    put("id", id)
                    put("action", action)
                    put("instructions", instructions)
                    put("feedback", instructions)
                }

                val result = request(
                    "/studio/review",
                    "POST",
                    body
                )

                runOnUiThread {
                    showResponseDialog(
                        "Studio का जवाब",
                        result.toString(2)
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast("अनुरोध पूरा नहीं हुआ: ${e.message}")
                }
            }
        }
    }// =========================
    // RESPONSE DIALOG
    // =========================

    private fun showResponseDialog(
        title: String,
        message: String
    ) {
        val textView = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(24, 20, 24, 20)
        }

        val scrollView = ScrollView(this)
        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("ठीक है", null)
            .show()
    }

    // =========================
    // HTTP REQUEST HELPER
    // =========================

    private fun request(
        path: String,
        method: String,
        body: JSONObject? = null
    ): JSONObject {
        val baseUrl =
            "https://shriyaan-ai-api.tt6914794.workers.dev"

        val connection = (URL(baseUrl + path).openConnection()
                as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20000
            readTimeout = 60000
            setRequestProperty("Accept", "application/json")

            if (body != null) {
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )
            }
        }

        try {
            if (body != null) {
                OutputStreamWriter(
                    connection.outputStream,
                    Charsets.UTF_8
                ).use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode

            val responseStream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val responseText = responseStream?.use { stream ->
                BufferedReader(
                    InputStreamReader(stream, Charsets.UTF_8)
                ).use { reader ->
                    reader.readText()
                }
            } ?: ""

            if (responseCode !in 200..299) {
                throw Exception(
                    "Server error $responseCode: $responseText"
                )
            }

            return try {
                JSONObject(responseText)
            } catch (_: Exception) {
                JSONObject().put("response", responseText)
            }
        } finally {
            connection.disconnect()
        }
    }

    // =========================
    // ERROR MESSAGE
    // =========================

    private fun errorMessage(error: Throwable): String {
        return error.message
            ?: error.localizedMessage
            ?: "अज्ञात त्रुटि हुई।"
    }

    // =========================
    // END OF MAIN ACTIVITY
    // =========================
}
