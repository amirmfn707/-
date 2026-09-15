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

    suspend fun parseVoiceSchedule(
        transcript: String,
        modelName: String = "gpt-4o-mini"
    ): Result<ParsedScheduleResult> = withContext(Dispatchers.IO) {
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
            تو یک دستیار هوشمند و دقیق زمان‌بندی و یادآوری کارهای شخصی و کاری به زبان فارسی هستی.
            کاربر یک متن صوتی یا پیامی می‌گوید.
            زمان جاری سیستم: $nowIsoFormat
            تاریخ و روز جاری به شمسی: $persianDesc
            
            وظیفه تو این است که پیام کاربر را درک کرده و برنامه‌ریزی کاری را استخراج کنی.
            فقط و فقط یک شیء JSON خام بدون هیچ توضیح اضافی یا مارک‌داون بازگردان، با این ساختار:
            {
              "title": "عنوان کوتاه و صریح برنامه به فارسی",
              "type": "meeting" یا "reminder" یا "task" یا "event",
              "isoDateTime": "تاریخ و ساعت به فرمت ISO مانند 2026-09-16T15:30:00 (اگر زمان یا تاریخی مشخص شده باشد، وگرنه null)",
              "durationMinutes": 30,
              "notes": "توضیحات و جزئیات بیشتر در صورت وجود، وگرنه null",
              "reminderMinutesBefore": 15
            }
            قوانین:
            - عبارت "فردا" یعنی ۱ روز بعد از زمان جاری.
            - "پس‌فردا" یعنی ۲ روز بعد.
            - اگر فقط ساعت گفته شده باشد (مثلاً ساعت ۴ بعدازظهر)، نسبت به ساعت کنونی تاریخ را امروز یا فردا بگذار.
            - نوع را مشخص کن: اگر جلسه است "meeting"، اگر یادآوری است "reminder"، اگر کار انجام‌دادنی است "task"، اگر رویداد عمومی است "event".
            - اگر هیچ تاریخ یا ساعتی وجود نداشت، isoDateTime را null بگذار.
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

                val cleanJson = content
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val parsedObj = JSONObject(cleanJson)
                val title = parsedObj.optString("title", transcript)
                val type = parsedObj.optString("type", ScheduleItem.TYPE_TASK)
                val isoDateTime = if (parsedObj.isNull("isoDateTime")) null else parsedObj.optString("isoDateTime", null)
                val duration = parsedObj.optInt("durationMinutes", 30)
                val notes = if (parsedObj.isNull("notes")) null else parsedObj.optString("notes", null)
                val reminderMinutes = parsedObj.optInt("reminderMinutesBefore", 15)

                val millis = PersianDateUtil.parseIsoToMillis(isoDateTime)

                Result.success(
                    ParsedScheduleResult(
                        title = if (title.isBlank()) transcript else title,
                        type = type,
                        dateTimeMillis = millis,
                        durationMinutes = duration,
                        notes = notes,
                        reminderMinutesBefore = reminderMinutes,
                        rawAssistantResponse = content
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GapGptService", "Network or parsing error", e)
            Result.failure(Exception("خطا در ارتباط با سرور هوش مصنوعی: ${e.localizedMessage ?: "عدم اتصال"}"))
        }
    }
}
