package com.shriyaanai.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.WindowManager
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

    private lateinit var rootLayout: LinearLayout
    private lateinit var chatArea: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var chatInput: EditText
    private lateinit var studioQueue: LinearLayout

    private val baseUrl =
        "https://shriyaan-ai-api.tt6914794.workers.dev"

    private val prefs by lazy {
        getSharedPreferences("shriyaan_preferences", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        showHomeScreen()
    }

    // =========================================
    // BASE LAYOUT
    // =========================================

    private fun createBaseLayout() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.WHITE)
        }

        setContentView(rootLayout)
    }

    private fun addHeading(text: String) {
        val heading = TextView(this).apply {
            this.text = text
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(35, 55, 95))
            setPadding(4, 12, 4, 12)
        }

        rootLayout.addView(heading)
    }

    private fun addText(text: String, size: Float = 16f) {
        val label = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.DKGRAY)
            setPadding(6, 8, 6, 8)
        }

        rootLayout.addView(label)
    }

    private fun makeButton(
        title: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 15f
            setOnClickListener { action() }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // =========================================
    // HOME SCREEN
    // =========================================

    private fun showHomeScreen() {
        createBaseLayout()

        addHeading("✦ Shriyaan AI")
        addText("आपका AI Chat और Auto Studio", 17f)

        rootLayout.addView(
            makeButton("💬 AI Chat खोलें") {
                showChatScreen()
            }
        )

        rootLayout.addView(
            makeButton("🎬 Auto Studio खोलें") {
                showStudioScreen()
            }
        )

        rootLayout.addView(
            makeButton("🌐 Server Status जाँचें") {
                checkServerStatus()
            }
        )

        rootLayout.addView(
            makeButton("ℹ️ ऐप की जानकारी") {
                showAbout()
            }
        )

        addText("Shriyaan AI — Chat, Script और Studio", 13f)
        addText(
            "AI जवाब और Studio सुविधाएँ सर्वर की उपलब्धता पर निर्भर हैं।",
            12f
        )
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("Shriyaan AI")
            .setMessage(
                "Shriyaan AI में AI Chat और Studio सुविधाएँ उपलब्ध हैं।\n\n" +
                    "स्क्रिप्ट बनाने और समीक्षा करने के लिए इंटरनेट तथा " +
                    "सही ढंग से काम करने वाला backend आवश्यक है।\n\n" +
                    "वीडियो रेंडरिंग और YouTube पर अपलोड तभी काम करेंगे " +
                    "जब सर्वर पर वे सुविधाएँ भी लागू हों।"
            )
            .setPositiveButton("ठीक है", null)
            .show()
    }

    // =========================================
    // SERVER STATUS
    // =========================================

    private fun checkServerStatus() {
        showToast("Server जाँचा जा रहा है...")

        thread {
            try {
                val result = request("/")

                runOnUiThread {
                    showResponseDialog(
                        "Server Status",
                        result.toString(2)
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast(
                        "Server कनेक्शन विफल: ${errorMessage(e)}"
                    )
                }
            }
        }
    }

    // =========================================
    // CHAT SCREEN
    // =========================================

    private fun showChatScreen() {
        createBaseLayout()

        addHeading("💬 Shriyaan AI Chat")

        rootLayout.addView(
            makeButton("← Home") {
                showHomeScreen()
            }
        )

        chatScroll = ScrollView(this)

        chatArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 12)
        }

        chatScroll.addView(chatArea)

        rootLayout.addView(
            chatScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        chatInput = EditText(this).apply {
            hint = "अपना सवाल लिखें..."
            textSize = 16f
            minLines = 1
            maxLines = 4
            setPadding(12, 8, 12, 8)
        }

        rootLayout.addView(
            chatInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val sendButton = Button(this).apply {
            text = "Send ➤"
            isAllCaps = false
        }

        val clearButton = Button(this).apply {
            text = "Clear"
            isAllCaps = false
        }

        buttonRow.addView(
            sendButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        buttonRow.addView(
            clearButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        rootLayout.addView(buttonRow)

        sendButton.setOnClickListener {
            val message = chatInput.text.toString().trim()

            if (message.isEmpty()) {
                showToast("पहले अपना सवाल लिखें।")
            } else {
                sendChatMessage(message, sendButton)
            }
        }

        clearButton.setOnClickListener {
            chatArea.removeAllViews()
            saveChatHistory("")
            showToast("Chat साफ़ कर दी गई।")
        }

        loadChatHistory()
    }

    // =========================================
    // CHAT MESSAGE
    // =========================================

    private fun sendChatMessage(
        message: String,
        sendButton: Button
    ) {
        addChatBubble("आप", message, true)
        saveChatHistoryFromView()

        chatInput.setText("")
        hideKeyboard()

        sendButton.isEnabled = false
        sendButton.text = "रुकें..."

        thread {
            try {
                val body = JSONObject().apply {
                    put("message", message)
                }

                val result = request(
                    "/chat",
                    "POST",
                    body
                )

                val answer = extractChatAnswer(result)

                runOnUiThread {
                    addChatBubble("Shriyaan AI", answer, false)
                    saveChatHistoryFromView()

                    sendButton.isEnabled = true
                    sendButton.text = "Send ➤"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    addChatBubble(
                        "त्रुटि",
                        errorMessage(e),
                        false
                    )

                    saveChatHistoryFromView()

                    sendButton.isEnabled = true
                    sendButton.text = "Send ➤"
                }
            }
        }
    }

    private fun extractChatAnswer(result: JSONObject): String {
        val candidates = arrayOf(
            "reply",
            "response",
            "answer",
            "message",
            "text",
            "output"
        )

        for (key in candidates) {
            val value = result.optString(key, "")
            if (value.isNotBlank() && value != "null") {
                return value
            }
        }

        val nested = result.optJSONObject("data")

        if (nested != null) {
            for (key in candidates) {
                val value = nested.optString(key, "")
                if (value.isNotBlank() && value != "null") {
                    return value
                }
            }
        }

        if (result.optBoolean("success", false)) {
            return "अनुरोध सफल हुआ, लेकिन सर्वर ने जवाब का टेक्स्ट नहीं भेजा।"
        }

        return result.optString(
            "error",
            "AI से कोई टेक्स्ट जवाब नहीं मिला।"
        )
    }

    private fun addChatBubble(
        speaker: String,
        message: String,
        isUser: Boolean
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)

            setBackgroundColor(
                if (isUser) {
                    Color.rgb(230, 240, 255)
                } else {
                    Color.rgb(245, 245, 245)
                }
            )
        }

        val name = TextView(this).apply {
            text = speaker
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(35, 55, 95))
        }

        val content = TextView(this).apply {
            text = message
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 6, 0, 0)
        }

        card.addView(name)
        card.addView(content)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 6, 0, 6)

        chatArea.addView(card, params)

        chatScroll.post {
            chatScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    // =========================================
    // CHAT HISTORY
    // =========================================

    private fun saveChatHistory(text: String) {
        prefs.edit()
            .putString("chat_history", text)
            .apply()
    }

    private fun saveChatHistoryFromView() {
        val history = StringBuilder()

        for (i in 0 until chatArea.childCount) {
            val card = chatArea.getChildAt(i) as? LinearLayout
                ?: continue

            val name = card.getChildAt(0) as? TextView
            val content = card.getChildAt(1) as? TextView

            if (name != null && content != null) {
                history.append(name.text)
                    .append(": ")
                    .append(content.text)
                    .append("\n\n")
            }
        }

        saveChatHistory(history.toString())
    }

    private fun loadChatHistory() {
        val history = prefs.getString("chat_history", "") ?: ""

        if (history.isBlank()) {
            addChatBubble(
                "Shriyaan AI",
                "नमस्ते! मैं आपकी क्या सहायता कर सकता हूँ?",
                false
            )
            return
        }

        val blocks = history.split("\n\n")

        for (block in blocks) {
            val index = block.indexOf(": ")

            if (index > 0) {
                val speaker = block.substring(0, index)
                val message = block.substring(index + 2)

                addChatBubble(
                    speaker,
                    message,
                    speaker == "आप"
                )
            }
        }
    }

    private fun hideKeyboard() {
        try {
            val manager =
                getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager

            manager.hideSoftInputFromWindow(
                chatInput.windowToken,
                0
            )
        } catch (_: Exception) {
        }
    }

    // =========================================
    // AUTO STUDIO SCREEN
    // =========================================

    private fun showStudioScreen() {
        createBaseLayout()

        addHeading("🎬 Shriyaan AI Studio")

        addText(
            "राजस्थान के इतिहास और रोचक तथ्यों पर वीडियो स्क्रिप्ट बनाएँ।",
            14f
        )

        rootLayout.addView(
            makeButton("← Home") {
                showHomeScreen()
            }
        )

        rootLayout.addView(
            makeButton("✨ नई स्क्रिप्ट बनाएँ") {
                generateScript()
            }
        )

        rootLayout.addView(
            makeButton("🔄 Studio Status") {
                loadStudioStatus()
            }
        )

        rootLayout.addView(
            makeButton("📋 Queue Refresh करें") {
                loadQueue()
            }
        )

        addHeading("📚 Video Queue")

        val queueScroll = ScrollView(this)

        studioQueue = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 12)
        }

        queueScroll.addView(studioQueue)

        rootLayout.addView(
            queueScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        loadQueue()
    }

    // =========================================
    // GENERATE SCRIPT
    // =========================================

    private fun generateScript() {
        AlertDialog.Builder(this)
            .setTitle("नई वीडियो स्क्रिप्ट")
            .setMessage(
                "राजस्थान के इतिहास और रोचक तथ्यों पर " +
                    "नई YouTube Shorts स्क्रिप्ट तैयार करें?"
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
                    put(
                        "topic",
                        "राजस्थान का इतिहास और रोचक तथ्य"
                    )
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
                    val item = result.optJSONObject("item")

                    if (result.optBoolean("success", false) &&
                        item != null
                    ) {
                        showScriptDetails(item)
                    } else {
                        showResponseDialog(
                            "स्क्रिप्ट प्रतिक्रिया",
                            result.optString(
                                "message",
                                result.toString(2)
                            )
                        )
                    }

                    loadQueue()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast(
                        "स्क्रिप्ट नहीं बनी: ${errorMessage(e)}"
                    )
                }
            }
        }
    }

    // =========================================
    // STUDIO STATUS
    // =========================================

    private fun loadStudioStatus() {
        showToast("Studio Status लोड हो रहा है...")

        thread {
            try {
                val result = request("/studio/status")

                runOnUiThread {
                    showResponseDialog(
                        "Studio Status",
                        result.toString(2)
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast(
                        "Status नहीं मिला: ${errorMessage(e)}"
                    )
                }
            }
        }
    }

    // =========================================
    // LOAD QUEUE
    // =========================================

    private fun loadQueue() {
        thread {
            try {
                val result = request("/studio/queue")
                val items = result.optJSONArray("items")
                    ?: JSONArray()

                runOnUiThread {
                    displayQueue(items)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (::studioQueue.isInitialized) {
                        studioQueue.removeAllViews()

                        val errorText = TextView(this).apply {
                            text = "Queue लोड नहीं हुई:\n${errorMessage(e)}"
                            textSize = 14f
                            setTextColor(Color.RED)
                            setPadding(8, 12, 8, 12)
                        }

                        studioQueue.addView(errorText)
                    }
                }
            }
        }
    }

    private fun displayQueue(items: JSONArray) {
        studioQueue.removeAllViews()

        if (items.length() == 0) {
            val emptyText = TextView(this).apply {
                text =
                    "अभी Queue खाली है।\n" +
                    "नई स्क्रिप्ट बनाने के लिए ऊपर बटन दबाएँ।"
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(8, 16, 8, 16)
            }

            studioQueue.addView(emptyText)
            return
        }

        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            addQueueCard(item)
        }
    }

    private fun addQueueCard(item: JSONObject) {
        val id = item.optString(
            "id",
            item.optString("scriptId", "")
        )

        val title = item.optString(
            "title",
            item.optString("topic", "बिना शीर्षक की स्क्रिप्ट")
        )

        val status = item.optString("status", "अज्ञात")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(245, 245, 245))
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(35, 55, 95))
        }

        card.addView(titleView)

        val statusView = TextView(this).apply {
            text = "स्थिति: ${statusHindi(status)}"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 6, 0, 6)
        }

        card.addView(statusView)

        card.addView(
            makeButton("स्क्रिप्ट देखें") {
                showScriptDetails(item)
            }
        )

        card.addView(
            makeButton("Review / Approve / Revise") {
                if (id.isBlank()) {
                    showToast("इस स्क्रिप्ट की ID उपलब्ध नहीं है।")
                } else {
                    showScriptActions(item)
                }
            }
        )

        val divider = View(this).apply {
            setBackgroundColor(Color.LTGRAY)
        }

        val dividerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        )
        dividerParams.setMargins(0, 12, 0, 12)
        card.addView(divider, dividerParams)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, 6, 0, 6)

        studioQueue.addView(card, cardParams)
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

    // =========================================
    // SCRIPT DETAILS
    // =========================================

    private fun showScriptDetails(item: JSONObject) {
        val title = item.optString(
            "title",
            item.optString("topic", "स्क्रिप्ट विवरण")
        )

        val script = item.optString(
            "script",
            item.optString(
                "content",
                item.optString(
                    "text",
                    "इस item में script text उपलब्ध नहीं है।"
                )
            )
        )

        val status = item.optString("status", "अज्ञात")

        val details = """
            शीर्षक:
            $title

            स्थिति:
            ${statusHindi(status)}

            स्क्रिप्ट:
            $script
        """.trimIndent()

        showResponseDialog("स्क्रिप्ट विवरण", details)
    }

    // =========================================
    // SCRIPT ACTIONS
    // =========================================

    private fun showScriptActions(item: JSONObject) {
        val actions = arrayOf(
            "स्क्रिप्ट विवरण देखें",
            "Approve करें",
            "सुधार के लिए भेजें",
            "Reject करें"
        )

        AlertDialog.Builder(this)
            .setTitle("स्क्रिप्ट के विकल्प")
            .setItems(actions) { _, index ->
                when (index) {
                    0 -> showScriptDetails(item)

                    1 -> confirmReviewAction(
                        item,
                        "approved",
                        "स्क्रिप्ट Approve करें?",
                        "क्या आप इस स्क्रिप्ट को स्वीकृत करना चाहते हैं?"
                    )

                    2 -> showRevisionDialog(item)

                    3 -> confirmReject(item)
                }
            }
            .setNegativeButton("बंद करें", null)
            .show()
    }

    private fun confirmReviewAction(
        item: JSONObject,
        status: String,
        title: String,
        message: String
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("रद्द करें", null)
            .setPositiveButton("हाँ, आगे बढ़ें") { _, _ ->
                reviewScriptWithAction(item, status, "")
            }
            .show()
    }

    private fun showRevisionDialog(item: JSONObject) {
        val input = EditText(this).apply {
            hint = "सुधार का निर्देश लिखें"
            minLines = 3
            setPadding(20, 16, 20, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("स्क्रिप्ट में सुधार")
            .setMessage(
                "उदाहरण: भाषा सरल करें, शुरुआत आकर्षक बनाएँ या तथ्य जाँचें।"
            )
            .setView(input)
            .setNegativeButton("रद्द करें", null)
            .setPositiveButton("सुधार भेजें") { _, _ ->
                val instruction = input.text.toString().trim()

                if (instruction.isBlank()) {
                    showToast("पहले सुधार का निर्देश लिखें।")
                } else {
                    reviewScriptWithAction(
                        item,
                        "revision_requested",
                        instruction
                    )
                }
            }
            .show()
    }

    private fun confirmReject(item: JSONObject) {
        AlertDialog.Builder(this)
            .setTitle("स्क्रिप्ट Reject करें?")
            .setMessage(
                "क्या आप इस स्क्रिप्ट को अस्वीकार करना चाहते हैं?"
            )
            .setNegativeButton("नहीं", null)
            .setPositiveButton("हाँ, Reject करें") { _, _ ->
                reviewScriptWithAction(item, "rejected", "")
            }
            .show()
    }

    private fun reviewScriptWithAction(
        item: JSONObject,
        status: String,
        note: String
    ) {
        val id = item.optString(
            "id",
            item.optString("scriptId", "")
        )

        if (id.isBlank()) {
            showToast("स्क्रिप्ट ID उपलब्ध नहीं है।")
            return
        }

        showToast("Studio को अनुरोध भेजा जा रहा है...")

        thread {
            try {
                val body = JSONObject().apply {
                    put("id", id)
                    put("status", status)

                    if (note.isNotBlank()) {
                        put("note", note.take(2000))
                    }
                }

                val result = request(
                    "/studio/review",
                    "POST",
                    body
                )

                runOnUiThread {
                    showResponseDialog(
                        "Studio Response",
                        result.toString(2)
                    )
                    loadQueue()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast(
                        "अनुरोध पूरा नहीं हुआ: ${errorMessage(e)}"
                    )
                }
            }
        }
    }

    // =========================================
    // RESPONSE DIALOG
    // =========================================

    private fun showResponseDialog(
        title: String,
        message: String
    ) {
        val textView = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(24, 20, 24, 20)
            setTextIsSelectable(true)
        }

        val scrollView = ScrollView(this)
        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("ठीक है", null)
            .show()
    }

    // =========================================
    // HTTP REQUEST HELPER
    // =========================================

    private fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null
    ): JSONObject {
        val connection = (
            URL(baseUrl + path).openConnection()
                as HttpURLConnection
        ).apply {
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

            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.use {
                BufferedReader(
                    InputStreamReader(it, Charsets.UTF_8)
                ).use { reader ->
                    reader.readText()
                }
            } ?: ""

            if (responseCode !in 200..299) {
                throw Exception("HTTP $responseCode: $responseText")
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

    // =========================================
    // ERROR MESSAGE
    // =========================================

    private fun errorMessage(error: Throwable): String {
        return error.message
            ?: error.localizedMessage
            ?: "अज्ञात त्रुटि हुई।"
    }
}
