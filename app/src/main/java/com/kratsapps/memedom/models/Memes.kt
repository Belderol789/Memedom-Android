package com.kratsapps.memedom.models

import android.util.Log
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class Memes : Serializable {
    var userGender: String = ""
    var userAge: Long = 0

    var postID: String = ""
    var postTitle: String = ""
    var postImageURL: String = ""
    var postUsername: String = ""
    var postProfileURL: String = ""
    var postUserUID: String = ""
    var postType: String = ""

    var postSavers: List<String> = listOf()
    var postLikers: List<String> = listOf()

    var postDate: Long = 0
    var postComments: Long = 0
    var postShares: Long = 0
    var postReports: Long = 0

    var postHeight: Long = 800
    var postNSFW: Boolean = false

    fun getPostLikeCount(): Int {
        return postLikers.count()
    }

    fun postDateString(): String {

        val postDateFromNow = (GregorianCalendar().timeInMillis - postDate)

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