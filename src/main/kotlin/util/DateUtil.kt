package com.hamster.pray.genshin.util

import java.text.SimpleDateFormat
import java.util.*

class DateUtil {
    companion object{
        fun getHourStart(value: Int): Date {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, value)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return Date(cal.timeInMillis)
        }

        fun getDateStr(): String {
            return SimpleDateFormat("yyyyMMdd").format(Date(System.currentTimeMillis()))
        }

    }
}