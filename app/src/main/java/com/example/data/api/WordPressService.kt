package com.example.data.api

import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WordPressService(
    private val getSiteUrl: () -> String,
    private val getUsername: () -> String,
    private val getAppPassword: () -> String,
    private val getConsumerKey: () -> String = { "" },
    private val getConsumerSecret: () -> String = { "" }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun getBaseUrl(): String {
        var url = getSiteUrl().trim().removeSuffix("/")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    private fun getAuthHeader(isWooCommerce: Boolean = false): String {
        val ck = getConsumerKey().trim()
        val cs = getConsumerSecret().trim()
        if (isWooCommerce && ck.isNotBlank() && cs.isNotBlank()) {
            return Credentials.basic(ck, cs)
        }
        val user = getUsername().trim()
        val pass = getAppPassword().trim().replace(" ", "")
        return Credentials.basic(user, pass)
    }

    /**
     * Tests connection to WordPress REST API and verifies user permissions.
     */
    suspend fun testConnection(): Result<WpConnectionStatus> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl()
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(Exception("آدرس سایت وردپرس وارد نشده است."))
        }

        val request = Request.Builder()
            .url("$baseUrl/wp-json/wp/v2/users/me")
            .addHeader("Authorization", getAuthHeader(false))
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val name = json.optString("name", "کاربر وردپرس")
                    Result.success(
                        WpConnectionStatus(
                            isConnected = true,
                            siteName = baseUrl.removePrefix("https://").removePrefix("http://"),
                            userDisplayName = name
                        )
                    )
                } else {
                    val err = when (response.code) {
                        401 -> "نام کاربری یا گذرواژه کاربردی (Application Password) اشتباه است."
                        403 -> "دسترسی کاربر برای استفاده از REST API مجاز نیست."
                        404 -> "مسیر REST API در سایت یافت نشد. اطمینان حاصل کنید وردپرس و پیوندهای یکتا فعال هستند."
                        else -> "خطای وردپرس (${response.code}): $bodyStr"
                    }
                    Result.failure(Exception(err))
                }
            }
        } catch (e: Exception) {
            Log.e("WordPressService", "Connection test failed", e)
            Result.failure(Exception("خطا در اتصال به سایت وردپرس: ${e.localizedMessage ?: "عدم ارتباط با هاست"}"))
        }
    }

    /**
     * Creates a new post (draft, published, or scheduled) in WordPress.
     */
    suspend fun createPost(
        title: String,
        content: String,
        status: String = "draft",
        excerpt: String? = null,
        scheduledDateIso: String? = null
    ): Result<WpPost> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl()
        val effectiveStatus = if (!scheduledDateIso.isNullOrBlank()) "future" else status
        val json = JSONObject().apply {
            put("title", title)
            put("content", content)
            put("status", effectiveStatus)
            if (!scheduledDateIso.isNullOrBlank()) {
                put("date", scheduledDateIso)
            }
            if (!excerpt.isNullOrBlank()) {
                put("excerpt", excerpt)
            }
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/wp-json/wp/v2/posts")
            .addHeader("Authorization", getAuthHeader(false))
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val respObj = JSONObject(bodyStr)
                    val post = WpPost(
                        id = respObj.optLong("id"),
                        title = respObj.optJSONObject("title")?.optString("rendered") ?: title,
                        content = respObj.optJSONObject("content")?.optString("rendered") ?: content,
                        excerpt = respObj.optJSONObject("excerpt")?.optString("rendered"),
                        status = respObj.optString("status", status),
                        link = respObj.optString("link")
                    )
                    Result.success(post)
                } else {
                    Result.failure(Exception("خطا در ثبت پست (${response.code}): $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Log.e("WordPressService", "createPost error", e)
            Result.failure(Exception("خطا در ایجاد پست در وردپرس: ${e.localizedMessage}"))
        }
    }

    /**
     * Retrieves recent posts from WordPress.
     */
    suspend fun getRecentPosts(perPage: Int = 10): Result<List<WpPost>> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl()
        val request = Request.Builder()
            .url("$baseUrl/wp-json/wp/v2/posts?per_page=$perPage&status=publish,draft,pending")
            .addHeader("Authorization", getAuthHeader(false))
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val array = JSONArray(bodyStr)
                    val list = mutableListOf<WpPost>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val title = obj.optJSONObject("title")?.optString("rendered") ?: "بدون عنوان"
                        val content = obj.optJSONObject("content")?.optString("rendered") ?: ""
                        val excerpt = obj.optJSONObject("excerpt")?.optString("rendered")
                        val status = obj.optString("status", "publish")
                        val link = obj.optString("link")
                        val date = obj.optString("date")

                        list.add(
                            WpPost(
                                id = obj.optLong("id"),
                                title = cleanHtml(title),
                                content = cleanHtml(content),
                                excerpt = excerpt?.let { cleanHtml(it) },
                                status = status,
                                dateGmt = date,
                                link = link
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("خطا در دریافت لیست مقالات (${response.code})"))
                }
            }
        } catch (e: Exception) {
            Log.e("WordPressService", "getRecentPosts error", e)
            Result.failure(Exception("عدم دسترسی به مقالات: ${e.localizedMessage}"))
        }
    }

    /**
     * Retrieves recent orders from WooCommerce.
     */
    suspend fun getRecentOrders(status: String? = null, perPage: Int = 10): Result<List<WcOrder>> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl()
        val urlBuilder = StringBuilder("$baseUrl/wp-json/wc/v3/orders?per_page=$perPage")
        if (!status.isNullOrBlank()) {
            urlBuilder.append("&status=$status")
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .addHeader("Authorization", getAuthHeader(true))
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val array = JSONArray(bodyStr)
                    val list = mutableListOf<WcOrder>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.optLong("id")
                        val number = obj.optString("number", id.toString())
                        val orderStatus = obj.optString("status", "processing")
                        val total = obj.optString("total", "0")
                        val currency = obj.optString("currency", "تومان")

                        val billing = obj.optJSONObject("billing")
                        val firstName = billing?.optString("first_name", "") ?: ""
                        val lastName = billing?.optString("last_name", "") ?: ""
                        val customerName = "$firstName $lastName".trim().ifEmpty { "مشتری مهمان" }
                        val customerPhone = billing?.optString("phone")

                        val lineItems = obj.optJSONArray("line_items")
                        var itemsCount = 0
                        val itemsNames = mutableListOf<String>()
                        if (lineItems != null) {
                            itemsCount = lineItems.length()
                            for (j in 0 until lineItems.length()) {
                                val itemObj = lineItems.getJSONObject(j)
                                val name = itemObj.optString("name", "محصول")
                                val qty = itemObj.optInt("quantity", 1)
                                itemsNames.add("$name ($qty)")
                            }
                        }

                        val shipping = obj.optJSONObject("shipping")
                        val address = shipping?.let {
                            "${it.optString("city", "")} ${it.optString("address_1", "")}".trim()
                        }

                        list.add(
                            WcOrder(
                                id = id,
                                number = number,
                                status = orderStatus,
                                total = total,
                                currency = currency,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                dateCreated = obj.optString("date_created", ""),
                                itemsCount = itemsCount,
                                itemsSummary = itemsNames.joinToString(", "),
                                shippingAddress = address
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("خطا در دریافت سفارش‌های ووکامرس (${response.code}): $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Log.e("WordPressService", "getRecentOrders error", e)
            Result.failure(Exception("خطا در دریافت سفارشات: ${e.localizedMessage}"))
        }
    }

    /**
     * Updates an order status (e.g. from "processing" to "completed").
     */
    suspend fun updateOrderStatus(orderId: Long, newStatus: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl()
        val json = JSONObject().apply {
            put("status", newStatus)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/wp-json/wc/v3/orders/$orderId")
            .addHeader("Authorization", getAuthHeader(true))
            .put(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("خطا در تغییر وضعیت سفارش (${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a WooCommerce discount coupon.
     */
    suspend fun createCoupon(
        code: String,
        amount: String,
        discountType: String = "percent",
        description: String? = null
    ): Result<WcCoupon> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl()
        val json = JSONObject().apply {
            put("code", code)
            put("amount", amount)
            put("discount_type", discountType)
            if (!description.isNullOrBlank()) {
                put("description", description)
            }
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/wp-json/wc/v3/coupons")
            .addHeader("Authorization", getAuthHeader(true))
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val obj = JSONObject(bodyStr)
                    val coupon = WcCoupon(
                        id = obj.optLong("id"),
                        code = obj.optString("code", code),
                        amount = obj.optString("amount", amount),
                        discountType = obj.optString("discount_type", discountType),
                        description = obj.optString("description")
                    )
                    Result.success(coupon)
                } else {
                    Result.failure(Exception("خطا در ایجاد کد تخفیف (${response.code}): $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves WooCommerce Sales Report (Total revenue, order count).
     */
    suspend fun getSalesReport(period: String = "month"): Result<WcSalesReport> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl()
        val request = Request.Builder()
            .url("$baseUrl/wp-json/wc/v3/reports/sales?period=$period")
            .addHeader("Authorization", getAuthHeader(true))
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val array = JSONArray(bodyStr)
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        val report = WcSalesReport(
                            totalSales = obj.optString("total_sales", "۰"),
                            netSales = obj.optString("net_sales", "۰"),
                            averageSales = obj.optString("average_sales", "۰"),
                            totalOrders = obj.optInt("total_orders", 0),
                            totalItems = obj.optInt("total_items", 0)
                        )
                        Result.success(report)
                    } else {
                        Result.success(WcSalesReport())
                    }
                } else {
                    Result.failure(Exception("خطا در استعلام گزارش فروش (${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace("<p>", "")
            .replace("</p>", "\n")
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<[^>]*>".toRegex(), "")
            .replace("&#8211;", "-")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .trim()
    }
}
