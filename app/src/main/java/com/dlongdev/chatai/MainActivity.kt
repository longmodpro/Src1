package com.dlongdev.chatai

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(33, 33, 35)
    private val panel = Color.rgb(44, 44, 46)
    private val fg = Color.rgb(236, 236, 241)
    private val muted = Color.rgb(170, 170, 180)
    private val green = Color.rgb(16, 163, 127)
    private lateinit var root: LinearLayout
    private lateinit var messagesBox: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: TextView
    private lateinit var status: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private val messages = mutableListOf<JSONObject>()
    private val prefs by lazy { getSharedPreferences("chat_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        loadHistory()
        renderMessages()
        if (messages.isEmpty()) addWelcome()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(12), dp(12)); setBackgroundColor(bg)
        }
        val title = TextView(this).apply {
            text = "Chat AI"; textSize = 20f; setTextColor(fg); typeface = Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply { text = "  ·  Trợ lý AI"; textSize = 12f; setTextColor(muted) }
        val titleWrap = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; addView(title); addView(subtitle) }
        header.addView(titleWrap, LinearLayout.LayoutParams(0, -2, 1f))
        val settings = TextView(this).apply {
            text = "⚙"; textSize = 24f; setTextColor(fg); gravity = Gravity.CENTER
            setPadding(dp(12), dp(4), dp(12), dp(4)); contentDescription = "Cài đặt API"
            setOnClickListener { showSettings() }
        }
        header.addView(settings)
        root.addView(header)
        val divider = View(this).apply { setBackgroundColor(Color.rgb(65, 65, 68)) }
        root.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))

        scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        messagesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(18)) }
        scroll.addView(messagesBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        status = TextView(this).apply { text = "Nhập API key trong ⚙ để bắt đầu"; textSize = 12f; setTextColor(muted); setPadding(dp(18), dp(5), dp(18), dp(5)) }
        root.addView(status)
        val composerOuter = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(4), dp(12), dp(10)); setBackgroundColor(bg) }
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM
            setPadding(dp(10), dp(7), dp(7), dp(7)); background = rounded(panel, 24)
        }
        input = EditText(this).apply {
            hint = "Nhắn tin cho Chat AI"; setHintTextColor(muted); setTextColor(fg); textSize = 16f
            setBackgroundColor(Color.TRANSPARENT); minLines = 1; maxLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        sendButton = TextView(this).apply {
            text = "↑"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = rounded(green, 50); setPadding(dp(13), dp(3), dp(13), dp(5)); setOnClickListener { sendMessage() }
        }
        composer.addView(sendButton, LinearLayout.LayoutParams(dp(44), dp(44)))
        composerOuter.addView(composer)
        val foot = TextView(this).apply { text = "AI có thể mắc lỗi. Kiểm tra thông tin quan trọng."; textSize = 10f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(0, dp(7), 0, 0) }
        composerOuter.addView(foot)
        root.addView(composerOuter)
        setContentView(root)
    }

    private fun addWelcome() {
        val welcome = JSONObject().put("role", "assistant").put("content", "Xin chào! Mình có thể giúp gì cho bạn hôm nay?\n\nĐể sử dụng AI, hãy nhấn ⚙ và nhập API key của bạn.")
        messages.add(welcome); saveHistory(); renderMessages()
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        if (prefs.getString("api_key", "").isNullOrBlank()) {
            showSettings("Bạn cần nhập API key trước khi gửi tin nhắn."); return
        }
        messages.add(JSONObject().put("role", "user").put("content", text))
        input.setText(""); hideKeyboard(); saveHistory(); renderMessages()
        setBusy(true, "Đang chờ phản hồi…")
        executor.execute {
            try {
                val answer = callApi()
                runOnUiThread {
                    messages.add(JSONObject().put("role", "assistant").put("content", answer))
                    saveHistory(); renderMessages(); setBusy(false, "Đã trả lời")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    messages.add(JSONObject().put("role", "assistant").put("content", "Không thể gọi API: ${e.message ?: "lỗi không xác định"}\n\nKiểm tra API key, kết nối mạng, model và hạn mức API rồi thử lại."))
                    saveHistory(); renderMessages(); setBusy(false, "Có lỗi xảy ra")
                }
            }
        }
    }

    private fun callApi(): String {
        val apiKey = prefs.getString("api_key", "") ?: ""
        val model = prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini"
        val endpoint = (prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "").trim()
        require(endpoint.startsWith("https://")) { "Endpoint phải dùng HTTPS." }
        val bodyMessages = JSONArray()
        val systemPrompt = prefs.getString("system_prompt", "Bạn là trợ lý hữu ích. Trả lời bằng ngôn ngữ người dùng sử dụng.") ?: ""
        bodyMessages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        // Send a bounded context window so requests do not grow indefinitely.
        messages.takeLast(30).forEach { m ->
            val role = m.optString("role")
            if (role == "user" || role == "assistant") bodyMessages.put(JSONObject().put("role", role).put("content", m.optString("content")))
        }
        val payload = JSONObject().put("model", model).put("messages", bodyMessages).put("temperature", 0.7)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 60000; doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            val json = JSONObject(response)
            if (code !in 200..299) {
                val apiError = json.optJSONObject("error")?.optString("message") ?: "HTTP $code"
                throw IllegalStateException("HTTP $code — $apiError")
            }
            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "(Phản hồi trống)")
        } finally { connection.disconnect() }
    }

    private fun showSettings(message: String? = null) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(8), dp(22), dp(4)) }
        fun field(hintText: String, value: String, secret: Boolean = false): EditText = EditText(this).apply {
            hint = hintText; setText(value); textSize = 14f
            if (secret) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        val key = field("OpenAI API key (sk-…)", prefs.getString("api_key", "") ?: "", true)
        val model = field("Model", prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini")
        val endpoint = field("API endpoint HTTPS", prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "")
        val system = field("System prompt", prefs.getString("system_prompt", "Bạn là trợ lý hữu ích. Trả lời bằng ngôn ngữ người dùng sử dụng.") ?: "")
        box.addView(labelled("API key", key)); box.addView(labelled("Model", model)); box.addView(labelled("Endpoint", endpoint)); box.addView(labelled("System prompt", system))
        val note = TextView(this).apply { text = "API key được lưu cục bộ trên thiết bị. Không đưa key vào GitHub hoặc chia sẻ APK có chứa key. API có thể phát sinh chi phí theo tài khoản của bạn."; textSize = 12f; setTextColor(muted); setPadding(0, dp(10), 0, 0) }
        box.addView(note)
        AlertDialog.Builder(this).setTitle("Cài đặt AI").setMessage(message).setView(box)
            .setPositiveButton("Lưu") { _, _ ->
                val ep = endpoint.text.toString().trim()
                if (!ep.startsWith("https://")) { Toast.makeText(this, "Endpoint phải dùng HTTPS", Toast.LENGTH_LONG).show(); return@setPositiveButton }
                prefs.edit().putString("api_key", key.text.toString().trim()).putString("model", model.text.toString().trim().ifBlank { "gpt-4o-mini" })
                    .putString("endpoint", ep).putString("system_prompt", system.text.toString().trim()).apply()
                status.text = if (key.text.toString().isBlank()) "Chưa cấu hình API key" else "Đã lưu cấu hình · ${model.text}"
                Toast.makeText(this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show()
            }.setNeutralButton("Xóa lịch sử") { _, _ ->
                messages.clear(); saveHistory(); addWelcome(); Toast.makeText(this, "Đã xóa lịch sử chat", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Đóng", null).show()
    }

    private fun labelled(label: String, view: EditText): View {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, dp(2)) }
        wrap.addView(TextView(this).apply { text = label; textSize = 12f; setTextColor(muted) })
        wrap.addView(view)
        return wrap
    }

    private fun renderMessages() {
        messagesBox.removeAllViews()
        messages.forEach { msg ->
            val user = msg.optString("role") == "user"
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = if (user) Gravity.END else Gravity.START; setPadding(0, dp(7), 0, dp(7)) }
            val name = TextView(this).apply { text = if (user) "Bạn" else "Chat AI"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(if (user) green else muted); setPadding(dp(4), 0, dp(4), dp(4)) }
            row.addView(name)
            val bubble = TextView(this).apply {
                text = msg.optString("content"); textSize = 15.5f; setTextColor(fg); setTextIsSelectable(true)
                setPadding(dp(13), dp(11), dp(13), dp(11)); background = rounded(if (user) Color.rgb(54, 54, 58) else panel, 16)
            }
            val params = LinearLayout.LayoutParams(if (user) (resources.displayMetrics.widthPixels * 0.84).toInt() else -1, -2)
            row.addView(bubble, params); messagesBox.addView(row)
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun saveHistory() {
        val array = JSONArray(); messages.takeLast(100).forEach { array.put(it) }
        prefs.edit().putString("history", array.toString()).apply()
    }

    private fun loadHistory() {
        try {
            val array = JSONArray(prefs.getString("history", "[]") ?: "[]")
            for (i in 0 until array.length()) messages.add(array.getJSONObject(i))
        } catch (_: Exception) { messages.clear() }
    }

    private fun setBusy(busy: Boolean, text: String) {
        sendButton.isEnabled = !busy; sendButton.alpha = if (busy) 0.5f else 1f
        status.text = text
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() { super.onDestroy(); executor.shutdown() }
}
