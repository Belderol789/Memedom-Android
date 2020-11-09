package com.kratsapps.memedom.models

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MemeDomUser: Serializable {
    var name: String = ""
    var gender: String = "Other"
    var birthday: String = ""
    var profilePhoto: String = ""
    var uid: String = ""
    var email: String = ""
    var bio: String = ""
    var gallery: List<String> = listOf()
    var memes: List<String> = listOf()

    var rejects: List<String> = listOf()
    var matches: List<String> = listOf()
    var liked: HashMap<String, Any> = hashMapOf()

    var rejectedMemes: List<String> = listOf()

    fun getUserAge(): Int {
        val sdf = SimpleDateFormat("dd MMMM yyyy")
        val date = sdf.parse(birthday)
        val currentDate = Date()
        val userAge = (currentDate.time - date.time) / 86400000 / 365
        return userAge.toInt()
    }
}

