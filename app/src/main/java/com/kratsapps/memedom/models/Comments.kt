package com.kratsapps.memedom.models

import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

class Comments : Serializable {
    var commentID: String = ""
    var commentText: String = ""
    var postID: String = ""
    var userPhotoURL: String = ""
    var userName: String = ""
    var commentDate: Long = 0
    var commentLikers: List<String> = listOf()
    var showActions: Boolean = true

    var replies: List<Any> = listOf()

    fun getCommentLikeCount(): Int {
        return commentLikers.size
    }

    fun commentDateString(): String {

        val postDateFromNow = (GregorianCalendar().timeInMillis - commentDate)

        val seconds: Int = (postDateFromNow / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = weeks / 4
        val years = months / 12

        //Log.d("Date", "System ${System.currentTimeMillis()} / PostDate $postDate / Seconds $seconds / Minutes $minutes / Hours $hours / Days $days / Weeks $weeks / Months $months / Years $years")

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