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
    val rawAssistantResponse: String? = null,
    val actionType: String? = null, // "wp_create_post", "wc_get_orders", "wc_sales_report", "wc_create_coupon"
    val postTitle: String? = null,
    val postContent: String? = null,
    val postStatus: String? = "draft", // "draft", "publish", "future"
    val postPublishIsoDateTime: String? = null,
    val couponCode: String? = null,
    val couponAmount: String? = null
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
                "title": "عنوان کوتاه و صریح برنامه یا کار به فارسی",
                "type": "meeting" یا "reminder" یا "task" یا "event",
                "isoDateTime": "تاریخ و ساعت به فرمت ISO مانند 2026-09-17T10:00:00 (اگر زمان مشخص است، وگرنه null)",
                "durationMinutes": 30,
                "notes": "توضیحات و جزئیات بیشتر در صورت وجود، وگرنه null",
                "reminderMinutesBefore": 15,
                "actionType": "wp_create_post" یا "wc_get_orders" یا "wc_sales_report" یا "wc_create_coupon" یا null,
                "postTitle": "عنوان پست در صورت ایجاد مقاله، وگرنه null",
                "postContent": "متن پیش‌نویس مقاله به فارسی در صورت درخواست پست، وگرنه null",
                "postStatus": "draft" یا "publish" یا "future",
                "postPublishIsoDateTime": "تاریخ و ساعت انتشار در فرمت ISO اگر زمان انتشار مشخص شد وگرنه null",
                "couponCode": "کد تخفیف انگلیسی، وگرنه null",
                "couponAmount": "مبلغ یا درصد تخفیف، وگرنه null"
              }
            ]

            دستورات ویژه مدیریت سایت وردپرس و ووکامرس:
            - اگر کاربر درخواست ایجاد، نوشتن یا انتشار مقاله/پست در سایت داد:
              * actionType را "wp_create_post" بگذار.
              * عنوان مناسب را در postTitle و محتوای باکیفیت و ساختاریافته را در postContent قرار بده.
              * تعیین وضعیت انتشار (بسیار مهم):
                ۱) اگر کاربر تاریخ یا زمان انتشار را ذکر کرد (مثلاً: «فردا ساعت ۶ عصر در سایتم منتشر کن» یا «برای ۵ شنبه ساعت ۱۰ پست کن»):
                   postStatus = "future"
                   postPublishIsoDateTime = تاریخ و ساعت محاسبه شده به فرمت ISO (مثلاً 2026-09-19T18:00:00)
                ۲) اگر صراحتاً گفت «منتشر کن»، «پست کن»، «همین الان بفرست روی سایت»:
                   postStatus = "publish"
                   postPublishIsoDateTime = null
                ۳) اگر فقط گفت «پیش‌نویس بساز» یا زمان/دستور انتشار نداد:
                   postStatus = "draft"
                   postPublishIsoDateTime = null
            - اگر کاربر استعلام سفارشات جدید یا ووکامرس را خواست (مثلاً: «سفارش‌های جدید سایت چیست؟» یا «سفارش‌های ووکامرس»):
              actionType را "wc_get_orders" بگذار و title را «بررسی سفارش‌های ووکامرس» قرار بده.
            - اگر گزارش فروش خواست (مثلاً: «گزارش فروش سایت»):
              actionType را "wc_sales_report" بگذار.
            - اگر ساخت کد تخفیف خواست (مثلاً: «کد تخفیف ۱۵ درصدی با نام BAHAR بساز»):
              actionType را "wc_create_coupon"، couponCode را «BAHAR» و couponAmount را «15» بگذار.

            تفکیک دقیق نوع برنامه (Type Classification - بسیار مهم و دقیق):
            ۱. "meeting" (جلسه و قرار کاری):
               - کلمات کلیدی: «جلسه»، «قرار ملاقات»، «دیدار با»، «میتینگ»، «ویزیت دکتر»، «مصاحبه»، «مشاوره با»، «جلسه آنلاین».
               - ویژگی: معمولاً durationMinutes را بین ۳۰ تا ۶۰ دقیقه بگذار.
            ۲. "reminder" (یادآوری صوتی و آلارم فوری):
               - کلمات کلیدی: «یادم بنداز»، «یادآوری کن»، «فراموش نشه»، «زنگ بزنم به»، «قرص»، «آلارم بذار»، «بیدارم کن»، «پیام بدم».
               - ویژگی: durationMinutes را ۰ یا ۵ بگذار و reminderMinutesBefore را ۰ (سر وقت) بگذار.
            ۳. "task" (وظیفه و کار اجرایی چک‌لیستی):
               - کلمات کلیدی: «باید انجام بدم»، «تسک»، «خرید وسایل»، «آماده کردن گزارش»، «ارسال بسته»، «بررسی حساب»، «تمیزکاری»، «کدنویسی».
               - ویژگی: کارهایی که باید انجام شوند و تیک پایان بخورند.
            ۴. "event" (رویداد و مناسبت):
               - کلمات کلیدی: «تولد»، «سالگرد»، «نمایشگاه»، «همایش»، «کنفرانس»، «تعطیلی»، «جشن»، «وبینار».
               - ویژگی: معمولاً رویداد تمام‌روز یا بازه‌ای است.

            قوانین زمانی:
            - "فردا" یعنی ۱ روز بعد از زمان جاری. "پس‌فردا" یعنی ۲ روز بعد.
            - اگر کاربر ساعت‌های متفاوتی را برای کارهای مختلف گفت، برای هر برنامه ساعت دقیق خودش را بگذار.
            - اگر زمان نسبی گفت (مثلاً ۱۰ دقیقه دیگر، نیم ساعت دیگر)، تاریخ و زمان دقیق آن را نسبت به زمان جاری سیستم محاسبه کن.
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

            val actionType = if (obj.isNull("actionType")) null else obj.optString("actionType", null)
            val postTitle = if (obj.isNull("postTitle")) null else obj.optString("postTitle", null)
            val postContent = if (obj.isNull("postContent")) null else obj.optString("postContent", null)
            val postStatus = obj.optString("postStatus", "draft")
            val postPublishIsoDateTime = if (obj.isNull("postPublishIsoDateTime")) null else obj.optString("postPublishIsoDateTime", null)
            val couponCode = if (obj.isNull("couponCode")) null else obj.optString("couponCode", null)
            val couponAmount = if (obj.isNull("couponAmount")) null else obj.optString("couponAmount", null)

            return ParsedScheduleResult(
                title = if (title.isBlank()) fallbackTranscript else title,
                type = type,
                dateTimeMillis = millis,
                durationMinutes = duration,
                notes = notes,
                reminderMinutesBefore = reminderMinutes,
                rawAssistantResponse = content,
                actionType = actionType,
                postTitle = postTitle,
                postContent = postContent,
                postStatus = postStatus,
                postPublishIsoDateTime = postPublishIsoDateTime,
                couponCode = couponCode,
                couponAmount = couponAmount
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
