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
    var liked: HashMap<String, Any> = hashMapOf()

    fun getUserAge(): Int {
        val sdf = SimpleDateFormat("dd MMMM yyyy")
        val date = sdf.parse(birthday)
        val currentDate = Date()
        val userAge = (currentDate.time - date.time) / 86400000 / 365
        return userAge.toInt()
    }
}
