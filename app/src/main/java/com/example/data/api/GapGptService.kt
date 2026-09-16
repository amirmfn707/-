package com.example.data.api

import android.util.Log
import com.example.data.model.ScheduleItem
import com.example.util.PersianDateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class ParsedScheduleResult(
    val title: String,
    val type: String,
    val dateTimeMillis: Long?,
    val durationMinutes: Int,
    val notes: String?,
    val reminderMinutesBefore: Int,
    val rawAssistantResponse: String? = null
)

class GapGptService(private val getApiKey: () -> String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    /**
     * Parses voice input and extracts all appointments, tasks, and reminders (one or multiple).
     */
    suspend fun parseVoiceSchedules(
        transcript: String,
        modelName: String = "gpt-4o-mini"
    ): Result<List<ParsedScheduleResult>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لطفاً ابتدا کلید API پلتفرم GapGPT را در بخش تنظیمات وارد نمایید.")
            )
        }

        val nowIsoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date())
        val persianDesc = PersianDateUtil.getCurrentPersianDescription()

        val systemPrompt = """
            تو یک دستیار صوتی هوشمند، حرفه‌ای و دقیق برای زمان‌بندی، ثبت یادآورها و مدیریت قرارهای کاری به زبان فارسی هستی.
            کاربر ممکن است یک، دو یا چند قرار کاری، جلسه یا یادآوری مختلف را در یک پیام صوتی بیان کند.
            زمان جاری سیستم: $nowIsoFormat
            تاریخ و روز جاری به شمسی: $persianDesc
            
            وظیفه تو این است که کل پیام کاربر را تحلیل کرده و تمام برنامه‌ها، جلسات و یادآوری‌های ذکر شده را استخراج کنی.
            قوانین حیاتی:
            ۱. اگر کاربر دو یا چند کار را در یک پیام گفت (مثلاً: «فردا ساعت ۱۰ جلسه با احمدی دارم و ساعت ۴ بعدازظهر هم یادآوری پرداخت قسط» یا «ساعت ۲ با علی قرار دارم و ساعت ۵ برم دکتر»)، حتماً به ازای هر کدام یک شیء جداگانه در آرایه JSON بساز. هرگز فقط اولین مورد را برنگردان.
            ۲. خروجی باید فقط و فقط یک آرایه JSON خام (حتی اگر فقط ۱ برنامه باشد) بدون هیچ متن، توضیح یا بلوک مارک‌داون باشد.
            
            ساختار آرایه JSON:
            [
              {
                "title": "عنوان کوتاه و صریح برنامه به فارسی (مثلاً: جلسه با احمدی)",
                "type": "meeting" یا "reminder" یا "task" یا "event",
                "isoDateTime": "تاریخ و ساعت به فرمت ISO مانند 2026-09-17T10:00:00 (اگر زمان مشخص است، وگرنه null)",
                "durationMinutes": 30,
                "notes": "توضیحات و جزئیات بیشتر در صورت وجود، وگرنه null",
                "reminderMinutesBefore": 15
              }
            ]
            
            قوانین زمانی:
            - "فردا" یعنی ۱ روز بعد از زمان جاری. "پس‌فردا" یعنی ۲ روز بعد.
            - اگر کاربر ساعت‌های متفاوتی را برای کارهای مختلف گفت، برای هر برنامه ساعت دقیق خودش را بگذار.
            - اگر زمان نسبی گفت (مثلاً ۱۰ دقیقه دیگر، نیم ساعت دیگر)، تاریخ و زمان دقیق آن را نسبت به زمان جاری سیستم محاسبه کن.
            - تعیین نوع: جلسه یا ویزیت -> "meeting"، هشدار و یادآوری -> "reminder"، کار انجام‌دادنی -> "task"، رویداد عمومی -> "event".
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("temperature", 0.2)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", transcript)
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.gapgpt.app/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        401 -> "کلید API پلتفرم GapGPT نامعتبر است (401). لطفاً کلید صحیح را در تنظیمات وارد کنید."
                        429 -> "محدودیت درخواست یا سقف اعتبار کلید GapGPT تمام شده است (429)."
                        else -> "خطای سرور GapGPT (${response.code}): $responseBodyStr"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val respJson = JSONObject(responseBodyStr)
                val choices = respJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("پاسخی از مدل هوش مصنوعی دریافت نشد."))
                }

                val content = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val results = parseJsonToResults(content, transcript)
                Result.success(results)
            }
        } catch (e: Exception) {
            Log.e("GapGptService", "Network or parsing error", e)
            Result.failure(Exception("خطا در ارتباط با سرور هوش مصنوعی: ${e.localizedMessage ?: "عدم اتصال"}"))
        }
    }

    suspend fun parseVoiceSchedule(
        transcript: String,
        modelName: String = "gpt-4o-mini"
    ): Result<ParsedScheduleResult> {
        val multiResult = parseVoiceSchedules(transcript, modelName)
        return multiResult.map { list ->
            list.firstOrNull() ?: ParsedScheduleResult(
                title = transcript,
                type = ScheduleItem.TYPE_TASK,
                dateTimeMillis = null,
                durationMinutes = 30,
                notes = null,
                reminderMinutesBefore = 15
            )
        }
    }

    private fun parseJsonToResults(content: String, fallbackTranscript: String): List<ParsedScheduleResult> {
        var cleanJson = content.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.removePrefix("```json")
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```")
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.removeSuffix("```")
        }
        cleanJson = cleanJson.trim()

        val results = mutableListOf<ParsedScheduleResult>()

        fun parseSingleItem(obj: JSONObject): ParsedScheduleResult {
            val title = obj.optString("title", fallbackTranscript)
            val type = obj.optString("type", ScheduleItem.TYPE_TASK)
            val isoDateTime = if (obj.isNull("isoDateTime")) null else obj.optString("isoDateTime", null)
            val duration = obj.optInt("durationMinutes", 30)
            val notes = if (obj.isNull("notes")) null else obj.optString("notes", null)
            val reminderMinutes = obj.optInt("reminderMinutesBefore", 15)
            val millis = PersianDateUtil.parseIsoToMillis(isoDateTime)

            return ParsedScheduleResult(
                title = if (title.isBlank()) fallbackTranscript else title,
                type = type,
                dateTimeMillis = millis,
                durationMinutes = duration,
                notes = notes,
                reminderMinutesBefore = reminderMinutes,
                rawAssistantResponse = content
            )
        }

        try {
            if (cleanJson.startsWith("[")) {
                val array = JSONArray(cleanJson)
                for (i in 0 until array.length()) {
                    val itemObj = array.optJSONObject(i)
                    if (itemObj != null) {
                        results.add(parseSingleItem(itemObj))
                    }
                }
            } else if (cleanJson.startsWith("{")) {
                val obj = JSONObject(cleanJson)
                val array = obj.optJSONArray("schedules")
                    ?: obj.optJSONArray("items")
                    ?: obj.optJSONArray("events")
                if (array != null && array.length() > 0) {
                    for (i in 0 until array.length()) {
                        val itemObj = array.optJSONObject(i)
                        if (itemObj != null) {
                            results.add(parseSingleItem(itemObj))
                        }
                    }
                } else {
                    results.add(parseSingleItem(obj))
                }
            }
        } catch (e: Exception) {
            Log.e("GapGptService", "Error parsing json array, attempting fallback", e)
        }

        if (results.isEmpty()) {
            results.add(
                ParsedScheduleResult(
                    title = fallbackTranscript,
                    type = ScheduleItem.TYPE_TASK,
                    dateTimeMillis = null,
                    durationMinutes = 30,
                    notes = null,
                    reminderMinutesBefore = 15,
                    rawAssistantResponse = content
                )
            )
        }

        return results
    }
}
