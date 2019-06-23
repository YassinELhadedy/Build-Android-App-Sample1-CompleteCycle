package com.transporter.streetglide.infrastructure.dto

import java.util.*

/**
 * Created by yassin on 10/24/17.
 * SgDateTime
 */
data class SgDateTime(val year: Int,
                      val month: Int,
                      val day: Int,
                      val hours: Int,
                      val minutes: Int,
                      val seconds: Int) {
    /*
     * The date conversion below are not accurate. Usually time zone handling
     * should be handled by the Java API. But duo to a bug in Android DLS
     * especially for Egypt we had to do the conversion manually a below.
     */
    companion object {
        fun Date.toSgDateTime(): SgDateTime {
            val cal = GregorianCalendar(TimeZone.getTimeZone("UTC"))
            cal.time = this
            cal.add(Calendar.HOUR, 2)
            return SgDateTime(cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND))
        }
    }

    fun toDate(): Date {
        val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"))
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, minutes)
        calendar.set(Calendar.SECOND, seconds)
        calendar.set(Calendar.MILLISECOND, 0)
        return Date(calendar.timeInMillis - 2 * 60 * 60 * 1000)
    }

}
