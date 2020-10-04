package com.kratsapps.memedom

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class Memes : Serializable {
    var postID: String = ""
    var postTitle: String = ""
    var postImageURL: String = ""
    var postUsername: String = ""
    var postProfileURL: String = ""
    var postUserUID: String = ""

    var postPoints: Long = 0
    var postLikers: List<String> = listOf()

    var postDate: Long = 0
    var postComments: Long = 0
    var postReports: Long = 0

    fun getPostLikeCount(): Int {
        return postLikers.size
    }

    fun postDateString(): String {
        val seconds = postDate / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        if (days > 3) {
            val dateString = SimpleDateFormat("yyyy-MM-dd").format(Date(postDate))
            return dateString
        } else if (days > 0) {
            return "$days days ago"
        } else if (hours < 23 && hours > 0) {
            return "$hours hours ago"
        } else if (minutes < 60 && minutes > 0) {
            return "$minutes minutes ago"
        } else {
            return "Just now"
        }
    }
}