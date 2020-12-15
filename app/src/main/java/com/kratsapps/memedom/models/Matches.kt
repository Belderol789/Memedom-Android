package com.kratsapps.memedom.models

import android.util.Log
import java.io.Serializable
import java.util.*

class Matches: Serializable {
    var name: String = ""
    var uid: String = ""
    var profilePhoto: String = ""
    var matchText: String = ""
    var chatDate: Long = 0
    var onlineDate: Long = 0
    var offered: String = ""
    var matchStatus: Boolean = false
    var online: Boolean = false

    fun postDateString(date: Long): String {

        val postDateFromNow = (GregorianCalendar().timeInMillis - date)

        val seconds: Int = (postDateFromNow / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = weeks / 4
        val years = months / 12

        Log.d("MatchDate", "MatchDate $date NOW $postDateFromNow")

        if (months > 12) {
            return "${years}y ago"
        } else if (weeks > 4) {
            return "${months}m ago"
        } else if (days > 7) {
            return "${weeks}w ago"
        } else if (days > 0) {
            return "${days}d ago"
        } else if (hours < 23 && hours > 0) {
            return "${hours}h ago"
        } else if (minutes < 60 && minutes > 0) {
            return "${minutes}min ago"
        } else {
            return "Just now"
        }
    }
}