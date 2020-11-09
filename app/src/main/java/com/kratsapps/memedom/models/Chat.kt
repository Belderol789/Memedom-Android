package com.kratsapps.memedom.models

import java.io.Serializable
import java.util.*

class Chat: Serializable {
    var chatID: String = ""
    var chatUserID: String = ""
    var chatType: Long = 0
    var chatDate: Long = 0
    var chatContent: String = ""
    var chatImageURL: String = ""

    fun commentDateString(): String {

        val postDateFromNow = (GregorianCalendar().timeInMillis - chatDate)

        val seconds: Int = (postDateFromNow / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = weeks / 4
        val years = months / 12

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