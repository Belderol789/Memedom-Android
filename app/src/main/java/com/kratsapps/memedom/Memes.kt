package com.kratsapps.memedom
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class Memes: Serializable {
    var postID: String = ""
    var postTitle: String = ""
    var postImageURL: String = ""
    var postUsername: String = ""
    var postProfileURL: String = ""

    var postDate: Long = 0
    var postLikes: Long = 0
    var postComments: Long = 0
    var postReports: Long = 0

    fun postDateString(): String {
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(postDate))
        return dateString
    }
}