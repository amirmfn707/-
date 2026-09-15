package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PersianDateUtil {

    private val PERSIAN_MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val PERSIAN_WEEK_DAYS = arrayOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه"
    )

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    /**
     * Converts Gregorian Year, Month (1-12), Day to Jalali Year, Month (1-12), Day
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy2 = if (gm > 2) gy + 1 else gy
        var gDayNo = 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd
        for (i in 0 until gm - 1) {
            gDayNo += gDaysInMonth[i]
        }

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        while (jm < 11 && jDayNo >= jDaysInMonth[jm]) {
            jDayNo -= jDaysInMonth[jm]
            jm++
        }
        val jd = jDayNo + 1
        return JalaliDate(jy, jm + 1, jd)
    }

    fun toPersianDigits(text: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = java.lang.StringBuilder()
        for (c in text) {
            if (c in '0'..'9') {
                sb.append(persianDigits[c - '0'])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun formatPersianDateTime(timeMillis: Long?): String {
        if (timeMillis == null) return "بدون زمان مشخص"
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val jDate = gregorianToJalali(gy, gm, gd)

        val dayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1
        val dayName = PERSIAN_WEEK_DAYS[dayOfWeekIndex % 7]
        val monthName = PERSIAN_MONTH_NAMES[jDate.month - 1]

        val hour = String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY))
        val minute = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

        val result = "$dayName ${jDate.day} $monthName ${jDate.year} - ساعت $hour:$minute"
        return toPersianDigits(result)
    }

    fun formatPersianTimeOnly(timeMillis: Long?): String {
        if (timeMillis == null) return "--:--"
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val hour = String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY))
        val minute = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))
        return toPersianDigits("$hour:$minute")
    }

    fun formatPersianDateOnly(timeMillis: Long?): String {
        if (timeMillis == null) return "بدون تاریخ"
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val jDate = gregorianToJalali(gy, gm, gd)
        val monthName = PERSIAN_MONTH_NAMES[jDate.month - 1]
        return toPersianDigits("${jDate.day} $monthName")
    }

    fun getRelativeDayLabel(timeMillis: Long?): String {
        if (timeMillis == null) return "بدون زمان"
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timeMillis }

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startOfTarget = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffDays = ((startOfTarget.timeInMillis - startOfToday.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

        return when {
            diffDays < 0 -> "گذشته"
            diffDays == 0 -> "امروز"
            diffDays == 1 -> "فردا"
            diffDays == 2 -> "پس‌فردا"
            diffDays in 3..7 -> "این هفته"
            else -> "آینده"
        }
    }

    fun getCountdownLabel(timeMillis: Long?): String {
        if (timeMillis == null) return ""
        val diff = timeMillis - System.currentTimeMillis()
        if (diff < 0) return "سپری شده"
        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24

        return toPersianDigits(
            when {
                days > 0 -> "$days روز دیگر"
                hours > 0 -> "$hours ساعت و ${minutes % 60} دقیقه دیگر"
                minutes > 0 -> "$minutes دقیقه دیگر"
                else -> "همین الان"
            }
        )
    }

    fun parseIsoToMillis(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                if (p.endsWith("X")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(isoString)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return null
    }

    fun getCurrentPersianDescription(): String {
        val nowCal = Calendar.getInstance()
        val gy = nowCal.get(Calendar.YEAR)
        val gm = nowCal.get(Calendar.MONTH) + 1
        val gd = nowCal.get(Calendar.DAY_OF_MONTH)
        val jDate = gregorianToJalali(gy, gm, gd)
        val dayIndex = nowCal.get(Calendar.DAY_OF_WEEK) - 1
        val dayName = PERSIAN_WEEK_DAYS[dayIndex % 7]
        val monthName = PERSIAN_MONTH_NAMES[jDate.month - 1]
        val hour = String.format(Locale.US, "%02d", nowCal.get(Calendar.HOUR_OF_DAY))
        val min = String.format(Locale.US, "%02d", nowCal.get(Calendar.MINUTE))
        return "$dayName ${jDate.day} $monthName ${jDate.year} ساعت $hour:$min"
    }
}
