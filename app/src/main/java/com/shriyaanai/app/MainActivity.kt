package com.shriyaanai.app

import android.app.Activity
import android.app.AlertDialog
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

    private val baseUrl =
        "https://shriyaan-ai-api.tt6914794.workers.dev"

    private val preferences by lazy {
        getSharedPreferences("shriyaan_chat_history", MODE_PRIVATE)
    }

    private data class ChatMessage(
        val role: String,
        val text: String
    )

    private val messages = mutableListOf<ChatMessage>()

    private lateinit var root: LinearLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var chatButton: Button
    private lateinit var studioButton: Button

    private var isWaiting = false
    private var currentScreen = "chat"

    private var topicInput: EditText? = null
    private var queueContainer: LinearLayout? = null
    private var queueScroll: ScrollView? = null
    private var studioMessage: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 15, 30)
        window.navigationBarColor = Color.rgb(15, 15, 30)

        loadHistory()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 10)
            setBackgroundColor(Color.rgb(15, 15, 30))
        }

        val header = TextView(this).apply {
            text = "✨ Shriyaan AI"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(6, 12, 6, 12)
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        chatButton = Button(this).apply {
            text = "💬 AI Chat"
            setOnClickListener {
                showChatScreen()
            }
        }

        studioButton = Button(this).apply {
            text = "🎬 Auto Studio"
            setOnClickListener {
                showStudioScreen()
            }
        }

        navigation.addView(
            chatButton,
            LinearLayout.LayoutParams(0, 52, 1f).apply {
                marginEnd = 5
            }
        )

        navigation.addView(
            studioButton,
            LinearLayout.LayoutParams(0, 52, 1f).apply {
                marginStart = 5
            }
        )

        root.addView(navigation)

        contentFrame = FrameLayout(this)

        root.addView(
            contentFrame,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = 10
            }
        )

        setContentView(root)

        showChatScreen()
    }

    // --------------------------------------------------
    // COMMON UI HELPERS
    // --------------------------------------------------

    private fun makeText(
        text: String,
        size: Float = 15f,
        color: Int = Color.WHITE
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            setPadding(10, 8, 10, 8)
        }
    }

    private fun makeButton(
        text: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }
    }

    private fun setScreen(view: View, screen: String) {
        currentScreen = screen
        contentFrame.removeAllViews()
        contentFrame.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        chatButton.isEnabled = screen != "chat"
        studioButton.isEnabled = screen != "studio"
    }

    private fun showLoading(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Shriyaan AI")
            .setMessage(message)
            .setPositiveButton("ठीक है", null)
            .show()
    }

    // --------------------------------------------------
    // CHAT HISTORY
    // --------------------------------------------------

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
        } catch (_: Exception) {
            messages.clear()
        }
    }

    private fun saveHistory() {
        val array = JSONArray()

        messages.forEach {
            array.put(
                JSONObject()
                    .put("role", it.role)
                    .put("text", it.text)
            )
        }

        preferences.edit()
            .putString("messages", array.toString())
            .apply()
    }

    // --------------------------------------------------
    // AI CHAT SCREEN
    // --------------------------------------------------

    private fun showChatScreen() {
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(22, 22, 40))
            setPadding(8, 8, 8, 8)
        }

        val chatScroll = ScrollView(this)

        val chatText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            setTextIsSelectable(true)
        }

        chatScroll.addView(chatText)

        val input = EditText(this).apply {
            hint = "यहाँ अपना सवाल लिखें..."
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(40, 40, 60))
            minLines = 1
            maxLines = 4
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val newChat = makeButton("＋ नई चैट") {
            if (isWaiting) {
                showLoading("पहले जवाब आने दें।")
            } else {
                messages.clear()
                saveHistory()
                renderChat(chatText, false)
            }
        }

        val send = makeButton("Send ➤") {
            sendChatMessage(input, send, newChat, chatText)
        }

        buttons.addView(
            newChat,
            LinearLayout.LayoutParams(0, 54, 1f)
        )

        buttons.addView(
            send,
            LinearLayout.LayoutParams(0, 54, 1f)
        )

        screen.addView(
            chatScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        screen.addView(input)

        screen.addView(buttons)

        setScreen(screen, "chat")

        renderChat(chatText, false)

        chatScroll.post {
            chatScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun renderChat(
        chatText: TextView,
        waiting: Boolean
    ) {
        val result = StringBuilder()

        if (messages.isEmpty()) {
            result.append(
                "Shriyaan AI में आपका स्वागत है! 👋\n\n" +
                "अपना सवाल नीचे लिखें।"
            )
        } else {
            messages.forEach {
                if (it.role == "user") {
                    result.append("🧑 आप:\n")
                } else {
                    result.append("🤖 Shriyaan AI:\n")
                }

                result.append(it.text)
                result.append("\n\n")
            }
        }

        if (waiting) {
            result.append("🤖 Shriyaan AI जवाब तैयार कर रहा है...")
        }

        chatText.text = result.toString()
    }

    private fun sendChatMessage(
        input: EditText,
        send: Button,
        newChat: Button,
        chatText: TextView
    ) {
        val message = input.text.toString().trim()

        if (message.isEmpty()) {
            showLoading("पहले अपना सवाल लिखें।")
            return
        }

        if (isWaiting) return

        messages.add(ChatMessage("user", message))
        saveHistory()
        input.text.clear()

        isWaiting = true
        send.isEnabled = false
        newChat.isEnabled = false

        renderChat(chatText, true)

        thread {
            val result = request(
                path = "/chat",
                method = "POST",
                body = JSONObject().put("message", message)
            )

            val reply = if (result.first in 200..299) {
                try {
                    JSONObject(result.second).optString(
                        "reply",
                        "AI से जवाब नहीं मिला।"
                    )
                } catch (_: Exception) {
                    "AI का जवाब पढ़ने में समस्या हुई।"
                }
            } else {
                errorMessage(result.first, result.second)
            }

            runOnUiThread {
                messages.add(ChatMessage("assistant", reply))
                saveHistory()

                isWaiting = false
                send.isEnabled = true
                newChat.isEnabled = true

                renderChat(chatText, false)
            }
        }
    }

    // --------------------------------------------------
    // AUTO STUDIO SCREEN
    // --------------------------------------------------

    private fun showStudioScreen() {
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.rgb(22, 22, 40))
        }

        screen.addView(
            makeText("🎬 Shriyaan AI Auto Studio", 22f)
        )

        screen.addView(
            makeText(
                "हिंदी YouTube Shorts के लिए Scripts तैयार करें।",
                14f,
                Color.LTGRAY
            )
        )

        screen.addView(makeText("वीडियो का विषय:", 15f))

        val topic = EditText(this).apply {
            hint = "राजस्थान का इतिहास और रोचक तथ्य"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(40, 40, 60))
            setText("राजस्थान का इतिहास और रोचक तथ्य")
            minLines = 2
            maxLines = 3
        }

        topicInput = topic
        screen.addView(topic)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val generateButton = makeButton("✨ Generate Script") {
            generateScript()
        }

        val refreshButton = makeButton("↻ Queue Refresh") {
            loadQueue()
        }

        actionRow.addView(
            generateButton,
            LinearLayout.LayoutParams(0, 56, 1f)
        )

        actionRow.addView(
            refreshButton,
            LinearLayout.LayoutParams(0, 56, 1f)
        )

        screen.addView(actionRow)

        studioMessage = makeText(
            "Queue लोड की जा रही है...",
            14f,
            Color.LTGRAY
        )

        screen.addView(studioMessage)

        queueScroll = ScrollView(this)

        queueContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(2, 4, 2, 12)
        }

        queueScroll!!.addView(queueContainer)

        screen.addView(
            queueScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setScreen(screen, "studio")

        loadQueue()
    }

    // --------------------------------------------------
    // GENERATE SCRIPT
    // --------------------------------------------------

    private fun generateScript() {
        if (isWaiting) {
            showLoading("एक अनुरोध पहले से चल रहा है।")
            return
        }

        val topic = topicInput?.text?.toString()?.trim().orEmpty()

        if (topic.isBlank()) {
            showError("कृपया वीडियो का विषय लिखें।")
            return
        }

        isWaiting = true
        studioMessage?.text = "AI नई Script बना रहा है..."

        thread {
            val result = request(
                path = "/studio/generate",
                method = "POST",
                body = JSONObject().put("topic", topic)
            )

            runOnUiThread {
                isWaiting = false

                if (result.first in 200..299) {
                    try {
                        val json = JSONObject(result.second)

                        if (json.optBoolean("success", false)) {
                            val item = json.optJSONObject("item")

                            val title = item?.optString(
                                "title",
                                "नई Script"
                            ) ?: "नई Script"

                            studioMessage?.text =
                                "✅ Script तैयार: $title"

                            showLoading("Script तैयार हो गई!")

                            loadQueue()
                        } else {
                            studioMessage?.text =
                                "Script नहीं बन पाई।"

                            showError(
                                json.optString(
                                    "message",
                                    "AI Script बनाने में समस्या हुई।"
                                )
                            )
                        }
                    } catch (_: Exception) {
                        showError("सर्वर का जवाब पढ़ा नहीं जा सका।")
                    }
                } else {
                    val message = errorMessage(
                        result.first,
                        result.second
                    )

                    studioMessage?.text = message
                    showError(message)
                }
            }
        }
    }

    // --------------------------------------------------
    // LOAD QUEUE
    // --------------------------------------------------

    private fun loadQueue() {
        if (currentScreen != "studio") return

        studioMessage?.text = "Queue लोड हो रही है..."

        thread {
            val result = request(
                path = "/studio/queue",
                method = "GET"
            )

            runOnUiThread {
                if (currentScreen != "studio") return@runOnUiThread

                val container = queueContainer ?: return@runOnUiThread
                container.removeAllViews()

                if (result.first !in 200..299) {
                    studioMessage?.text =
                        errorMessage(result.first, result.second)

                    container.addView(
                        makeText(
                            "Queue लोड नहीं हो पाई। Refresh दबाकर दोबारा कोशिश करें।"
                        )
                    )
                    return@runOnUiThread
                }

                try {
                    val json = JSONObject(result.second)

                    if (!json.optBoolean("success", false)) {
                        studioMessage?.text = "Queue उपलब्ध नहीं है।"
                        return@runOnUiThread
                    }

                    val items = json.optJSONArray("items") ?: JSONArray()

                    studioMessage?.text =
                        "📚 Queue में ${items.length()} Scripts"

                    if (items.length() == 0) {
                        container.addView(
                            makeText(
                                "अभी Queue खाली है।\n\n" +
                                "पहली Script बनाने के लिए ऊपर " +
                                "Generate Script दबाएँ।",
                                16f
                            )
                        )
                        return@runOnUiThread
                    }

                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        addQueueCard(container, item)
                    }

                } catch (_: Exception) {
                    studioMessage?.text = "Queue का डेटा पढ़ने में समस्या हुई।"
                }
            }
        }
    }

    // --------------------------------------------------
    // QUEUE CARD
    // --------------------------------------------------

    private fun addQueueCard(
        container: LinearLayout,
        item: JSONObject
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
            setBackgroundColor(Color.rgb(35, 35, 57))
        }

        val id = item.optString("id", "")

        val title = item.optString("title", "बिना शीर्षक की Script")
        val topic = item.optString("topic", "")
        val script = item.optString("script", "")
        val hook = item.optString("hook", "")
        val status = item.optString("status", "pending")
        val duration = item.optInt("durationSeconds", 45)

        card.addView(makeText(title, 18f))

        card.addView(
            makeText(
                "विषय: $topic\n" +
                "स्थिति: ${statusHindi(status)}\n" +
                "अवधि: $duration सेकंड",
                13f,
                Color.LTGRAY
            )
        )

        if (hook.isNotBlank()) {
            card.addView(
                makeText("🎯 Hook:\n$hook", 14f)
            )
        }

        val preview = if (script.length > 450) {
            script.take(450) + "…"
        } else {
            script
        }

        card.addView(
            makeText(
                if (preview.isBlank()) {
                    "Script का टेक्स्ट उपलब्ध नहीं है।"
                } else {
                    "📝 Script:\n$preview"
                },
                14f
            )
        )

        val detailsButton = makeButton("पूरी Script / Scenes देखें") {
            showScriptDetails(item)
        }

        card.addView(detailsButton)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val approveButton = makeButton("✅ Approve") {
            reviewScript(
                id,
                "approved",
                "मंजूर"
            )
        }

        val reviseButton = makeButton("✏️ Revise") {
            showRevisionDialog(id)
        }

        val rejectButton = makeButton("❌ Reject") {
            confirmReject(id)
        }

        actionRow.addView(
            approveButton,
            LinearLayout.LayoutParams(0, 52, 1f)
        )

        actionRow.addView(
            reviseButton,
            LinearLayout.LayoutParams(0, 52, 1f)
        )

        actionRow.addView(
            
